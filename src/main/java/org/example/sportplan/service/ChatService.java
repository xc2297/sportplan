package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.sportplan.entity.ExerciseType;
import org.example.sportplan.entity.RewardItem;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.ExerciseTypeMapper;
import org.example.sportplan.mapper.RewardItemMapper;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.agent.ToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

// AI 智能助手服务：调用智谱GLM接口，支持多轮 tool calling 按需查询数据
@Slf4j
@Service
public class ChatService {

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("${zhipu.model}")
    private String model;

    @Value("${zhipu.api-url}")
    private String apiUrl;

    // 最大工具调用轮次（防止无限循环）
    private static final int MAX_TOOL_ROUNDS = 5;

    private final ExerciseTypeMapper exerciseTypeMapper;
    private final RewardItemMapper rewardItemMapper;
    private final UserMapper userMapper;
    private final GroupService groupService;
    private final ToolRegistry toolRegistry;

    private static final ObjectMapper mapper = new ObjectMapper();

    public ChatService(ExerciseTypeMapper exerciseTypeMapper,
                       RewardItemMapper rewardItemMapper,
                       UserMapper userMapper,
                       GroupService groupService,
                       ToolRegistry toolRegistry) {
        this.exerciseTypeMapper = exerciseTypeMapper;
        this.rewardItemMapper = rewardItemMapper;
        this.userMapper = userMapper;
        this.groupService = groupService;
        this.toolRegistry = toolRegistry;
    }

    public Map<String, Object> chat(String userMessage) {
        return chat(new ArrayList<>(), userMessage, null);
    }

    // 核心对话方法：多轮 tool calling 循环，支持 AI 自主规划和链式查询
    public Map<String, Object> chat(List<Map<String, String>> history, String userMessage, Long userId) {
        String systemPrompt = buildSystemPrompt(userId);
        List<Map<String, Object>> tools = toolRegistry.getAvailableTools(userId);

        // 拼装消息列表
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        // 保留最近20条历史
        int start = Math.max(0, history.size() - 20);
        for (Map<String, String> h : history.subList(start, history.size())) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", h.get("role"));
            m.put("content", h.get("content"));
            messages.add(m);
        }

        // 添加当前用户消息（去重：如果 history 最后一条已经是当前消息则跳过）
        boolean historyEndsWithCurrent = !history.isEmpty()
                && "user".equals(history.get(history.size() - 1).get("role"))
                && userMessage.equals(history.get(history.size() - 1).get("content"));
        if (!historyEndsWithCurrent) {
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
        }

        try {
            sanitizeMessages(messages);

            // ====== 多轮工具调用循环 ======
            // AI 可以在每一轮决定是否需要调用工具，最多循环 MAX_TOOL_ROUNDS 次
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("messages", messages);
                body.put("stream", false);
                body.put("do_sample", false);
                if (!tools.isEmpty()) {
                    body.put("tools", tools);
                    if (round == 0) body.put("tool_choice", "auto");
                }

                JsonNode resp = callApi(body);
                if (resp == null) {
                    return errorReply("AI 服务返回异常");
                }

                JsonNode choice = resp.at("/choices/0");
                String finishReason = choice.at("/finish_reason").asText("");
                JsonNode messageNode = choice.at("/message");

                // 没有工具调用，直接返回 AI 回复
                if (!"tool_calls".equals(finishReason)) {
                    String content = messageNode.at("/content").asText("");
                    return parseRecordIntents(content != null ? content : "", userId);
                }

                // 有工具调用：执行本轮所有工具调用，追加结果到消息列表，继续下一轮
                log.info("工具调用轮次 {}/{}, 执行工具后继续循环", round + 1, MAX_TOOL_ROUNDS);
                executeToolCalls(messageNode, messages, userId);
            }

            // 超过最大轮次，做最后一次无工具调用获取回答
            log.warn("工具调用达到最大轮次 {}, 强制结束", MAX_TOOL_ROUNDS);
            sanitizeMessages(messages);
            Map<String, Object> finalBody = new HashMap<>();
            finalBody.put("model", model);
            finalBody.put("messages", messages);
            finalBody.put("stream", false);
            JsonNode finalResp = callApi(finalBody);
            if (finalResp == null) {
                return errorReply("AI 服务返回异常");
            }
            String reply = finalResp.at("/choices/0/message/content").asText("");
            return parseRecordIntents(reply != null ? reply : "", userId);

        } catch (Exception e) {
            log.error("调用智谱API失败: {}", e.getMessage());
            return errorReply("AI 服务暂时不可用，请稍后再试。");
        }
    }

    // 执行一轮工具调用：解析 AI 返回的 tool_calls，逐个执行，追加结果到消息列表
    private void executeToolCalls(JsonNode messageNode, List<Map<String, Object>> messages, Long userId) {
        JsonNode toolCalls = messageNode.at("/tool_calls");

        // 构建 assistant 消息，content 必须非空（智谱代理层会把空字符串转 null 导致 422）
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        String assistantContent = messageNode.at("/content").asText("");
        if (assistantContent.trim().isEmpty()) {
            assistantContent = "正在查询数据...";
        }
        assistantMsg.put("content", assistantContent);
        List<Map<String, Object>> toolCallsList = new ArrayList<>();
        for (JsonNode tc : toolCalls) {
            Map<String, Object> tcMap = new HashMap<>();
            tcMap.put("id", tc.at("/id").asText(""));
            tcMap.put("type", "function");
            Map<String, Object> funcMap = new HashMap<>();
            funcMap.put("name", tc.at("/function/name").asText(""));
            funcMap.put("arguments", fixArguments(tc.at("/function/arguments").asText("{}")));
            tcMap.put("function", funcMap);
            toolCallsList.add(tcMap);
        }
        assistantMsg.put("tool_calls", toolCallsList);
        messages.add(assistantMsg);

        // 逐个执行工具并追加 tool 结果
        for (JsonNode tc : toolCalls) {
            String callId = tc.at("/id").asText("");
            String funcName = tc.at("/function/name").asText("");
            String argsStr = fixArguments(tc.at("/function/arguments").asText("{}"));
            Map<String, Object> args;
            try {
                args = mapper.readValue(argsStr, Map.class);
            } catch (Exception e) {
                args = new HashMap<>();
            }
            // 通过 ToolRegistry 执行工具（动态路由到对应 AgentTool 实现）
            String toolResult = toolRegistry.executeTool(funcName, args, userId);
            log.info("工具 {} 执行完成, 结果长度: {}", funcName, toolResult != null ? toolResult.length() : 0);
            Map<String, Object> toolMsg = new HashMap<>();
            toolMsg.put("role", "tool");
            toolMsg.put("content", toolResult);
            toolMsg.put("tool_call_id", callId);
            messages.add(toolMsg);
        }

        sanitizeMessages(messages);
    }

    // ==================== API 调用 ====================

    // 非流式 API 调用，带重试机制（最多重试2次，间隔2秒）
    private JsonNode callApi(Map<String, Object> body) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        // 防御性修复：智谱代理层会把 "content":null 转为 null 导致 Claude API 422
        jsonBody = jsonBody.replace("\"content\":null", "\"content\":\"\"");
        String msgSummary = extractMessageSummary(body);
        log.info("调用智谱API, model={}, 消息概要: {}", body.get("model"), msgSummary);

        Exception lastError = null;
        for (int retry = 0; retry < 2; retry++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(120000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                if (is == null) {
                    log.error("API返回{}，响应流为空", code);
                    return null;
                }

                String responseStr;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    responseStr = sb.toString();
                }

                if (code >= 400) {
                    log.error("API返回错误 code={}, body={}", code, responseStr);
                    return null;
                }

                JsonNode result = mapper.readTree(responseStr);
                String finishReason = result.at("/choices/0/finish_reason").asText("");
                JsonNode msgNode = result.at("/choices/0/message");
                String replyContent = msgNode.at("/content").asText("");
                JsonNode respToolCalls = msgNode.at("/tool_calls");
                if (!respToolCalls.isMissingNode() && respToolCalls.isArray() && respToolCalls.size() > 0) {
                    StringBuilder toolNames = new StringBuilder();
                    for (JsonNode tc : respToolCalls) {
                        if (toolNames.length() > 0) toolNames.append(",");
                        toolNames.append(tc.at("/function/name").asText(""));
                    }
                    log.info("API响应, finish_reason={}, tool_calls=[{}], content={}",
                            finishReason, toolNames,
                            replyContent.length() > 50 ? replyContent.substring(0, 50) + "..." : replyContent);
                } else {
                    log.info("API响应, finish_reason={}, content={}",
                            finishReason,
                            replyContent.length() > 100 ? replyContent.substring(0, 100) + "..." : replyContent);
                }
                JsonNode usage = result.at("/usage");
                if (!usage.isMissingNode()) {
                    log.info("Token用量, prompt={}, completion={}",
                            usage.at("/prompt_tokens").asText("?"),
                            usage.at("/completion_tokens").asText("?"));
                }
                return result;
            } catch (Exception e) {
                lastError = e;
                log.warn("API调用失败(第{}次), 原因: {}", retry + 1, e.getMessage());
                if (retry < 1) Thread.sleep(2000);
            }
        }
        throw lastError;
    }

    // ==================== 运动记录意图解析 ====================

    private Map<String, Object> parseRecordIntents(String reply, Long userId) {
        Map<String, Object> response = new HashMap<>();
        if (reply == null || reply.isEmpty()) {
            response.put("reply", "抱歉，AI 暂时无法回答，请稍后再试。");
            return response;
        }

        Long groupId = null;
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null) groupId = user.getGroupId();
        }
        List<ExerciseType> types = groupId != null
                ? exerciseTypeMapper.selectList(
                        new LambdaQueryWrapper<ExerciseType>()
                                .eq(ExerciseType::getGroupId, groupId)
                                .eq(ExerciseType::getActive, true)
                                .orderByAsc(ExerciseType::getSortOrder))
                : exerciseTypeMapper.selectList(
                        new LambdaQueryWrapper<ExerciseType>()
                                .eq(ExerciseType::getActive, true)
                                .isNull(ExerciseType::getGroupId)
                                .orderByAsc(ExerciseType::getSortOrder));

        List<Map<String, Object>> intents = new ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int idx = reply.indexOf("[RECORD:", searchFrom);
            if (idx < 0) break;
            int endIdx = reply.indexOf("]", idx);
            if (endIdx < 0) break;
            String jsonStr = reply.substring(idx + 8, endIdx);
            try {
                JsonNode recordNode = mapper.readTree(jsonStr);
                String exerciseName = recordNode.has("exerciseName") ? recordNode.get("exerciseName").asText() : null;
                double amount = recordNode.has("amount") ? recordNode.get("amount").asDouble() : 0;
                if (exerciseName != null && amount > 0) {
                    for (ExerciseType t : types) {
                        if (t.getName().equals(exerciseName)) {
                            Map<String, Object> intent = new HashMap<>();
                            intent.put("exerciseTypeId", t.getId());
                            intent.put("exerciseName", exerciseName);
                            intent.put("amount", amount);
                            intent.put("unit", t.getUnit());
                            intents.add(intent);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析RECORD标记失败: {}", e.getMessage());
            }
            reply = reply.substring(0, idx) + reply.substring(endIdx + 1);
            searchFrom = idx;
        }

        if (!intents.isEmpty()) {
            response.put("recordIntents", intents);
        }
        response.put("reply", reply.trim());
        return response;
    }

    private Map<String, Object> errorReply(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("reply", msg);
        return err;
    }

    // ==================== 辅助方法 ====================

    // 修复 GLM 模型返回的 arguments 格式问题："{}{\"data_type\":\"score\"}" → "{\"data_type\":\"score\"}"
    private String fixArguments(String argsText) {
        if (argsText == null || argsText.trim().isEmpty()) {
            return "{}";
        }
        if (argsText.contains("}{")) {
            int idx = argsText.indexOf("}{");
            String fixed = argsText.substring(idx + 1);
            try {
                mapper.readTree(fixed);
                return fixed;
            } catch (Exception e) {
                log.warn("修复arguments后仍非合法JSON: {}", fixed);
                return "{}";
            }
        }
        return argsText;
    }

    @SuppressWarnings("unchecked")
    private String extractMessageSummary(Map<String, Object> body) {
        Object msgs = body.get("messages");
        if (!(msgs instanceof List)) return "无消息";
        List<Map<String, Object>> msgList = (List<Map<String, Object>>) msgs;
        StringBuilder sb = new StringBuilder();
        sb.append("共").append(msgList.size()).append("条[");
        for (int i = 0; i < msgList.size(); i++) {
            if (i > 0) sb.append(", ");
            Map<String, Object> m = msgList.get(i);
            String role = String.valueOf(m.get("role"));
            String content = String.valueOf(m.get("content"));
            if (content.length() > 20) content = content.substring(0, 20) + "...";
            sb.append(i).append(":").append(role).append("/").append(content);
            if (m.containsKey("tool_calls")) sb.append("+tc");
        }
        sb.append("]");
        return sb.toString();
    }

    private void sanitizeMessages(List<Map<String, Object>> messages) {
        for (Map<String, Object> m : messages) {
            if (m.get("content") == null) {
                m.put("content", "");
            }
        }
    }

    // ==================== 系统提示词（固定规则层 + 动态工具描述） ====================

    private String buildSystemPrompt(Long userId) {
        Long groupId = null;
        String userName = "";
        String gender = "";
        boolean isAdmin = false;
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                groupId = user.getGroupId();
                userName = user.getName();
                gender = user.getGender() != null ? user.getGender().name().toLowerCase() : "";
                isAdmin = groupService.isGroupAdmin(userId, groupId);
            }
        }

        List<ExerciseType> types;
        if (groupId != null) {
            types = exerciseTypeMapper.selectList(
                    new LambdaQueryWrapper<ExerciseType>()
                            .eq(ExerciseType::getGroupId, groupId)
                            .eq(ExerciseType::getActive, true)
                            .orderByAsc(ExerciseType::getSortOrder));
        } else {
            types = exerciseTypeMapper.selectList(
                    new LambdaQueryWrapper<ExerciseType>()
                            .eq(ExerciseType::getActive, true)
                            .isNull(ExerciseType::getGroupId)
                            .orderByAsc(ExerciseType::getSortOrder));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是\"运动积分系统\"的智能助手，专门帮助用户解答关于本系统的问题。请用简洁友好的中文回答。\n\n");

        sb.append("## 当前用户信息\n");
        sb.append("- 姓名：").append(userName).append("\n");
        sb.append("- 性别：").append("male".equals(gender) ? "男" : "女").append("\n");
        sb.append("- 角色：").append(isAdmin ? "小组管理员" : "普通成员").append("\n\n");

        sb.append("## 运动类型与积分系数\n");
        for (ExerciseType t : types) {
            sb.append("- ").append(t.getName())
              .append("（单位：").append(t.getUnit()).append("）")
              .append("：男 ").append(t.getMaleCoefficient()).append("分/").append(t.getUnit())
              .append("，女 ").append(t.getFemaleCoefficient()).append("分/").append(t.getUnit())
              .append("，单项每日上限").append(t.getDailyCap()).append("分\n");
        }

        List<RewardItem> rewards;
        if (groupId != null) {
            rewards = rewardItemMapper.selectList(
                    new LambdaQueryWrapper<RewardItem>()
                            .eq(RewardItem::getGroupId, groupId)
                            .orderByAsc(RewardItem::getSortOrder));
        } else {
            rewards = rewardItemMapper.selectList(
                    new LambdaQueryWrapper<RewardItem>()
                            .isNull(RewardItem::getGroupId)
                            .orderByAsc(RewardItem::getSortOrder));
        }

        sb.append("\n## 奖励兑换\n");
        for (RewardItem r : rewards) {
            sb.append("- ").append(r.getName())
              .append("（").append(r.getDescription()).append("）")
              .append("：").append(r.getPointsCost()).append("积分")
              .append("，额度").append(r.getMaxAmount()).append("元\n");
        }

        sb.append("\n## 积分规则\n");
        sb.append("- 积分 = 运动量 × 性别对应系数\n");
        sb.append("- 单项每日积分上限见上方各运动类型\n");
        sb.append("- 每日总积分上限40分\n");
        sb.append("- 同一天同一运动类型重复提交视为更新\n");
        sb.append("- 积分计算保留2位小数\n\n");

        sb.append("## 惩罚机制\n");
        sb.append("- 每天凌晨0:05自动检查，前一天没有运动记录的用户扣减1分可用积分\n");
        sb.append("- 扣分会生成\"未运动惩罚\"记录，只有小组管理员可以删除\n\n");

        sb.append("## 兑换券使用\n");
        sb.append("- 兑换后需点击\"使用\"按钮，填写使用描述并上传凭证图片\n");
        sb.append("- 已使用的兑换券不可撤销\n");
        sb.append("- 小组管理员可撤销未使用的兑换券（退还积分）\n\n");

        sb.append("## 小组功能\n");
        sb.append("- 用户可创建小组或申请加入，申请需管理员审批\n");
        sb.append("- 每人只能加入一个小组\n");
        sb.append("- 创建者自动成为管理员，管理员可将成员设为管理员\n");
        sb.append("- 每个小组有独立的运动类型和奖励（从全局模板复制）\n");
        sb.append("- 管理员可查看所有组员数据，普通成员只看自己\n\n");

        sb.append("## 操作指引\n");
        sb.append("- 记录运动：点击\"记录\"tab\n");
        sb.append("- 查看积分：首页仪表盘\n");
        sb.append("- 兑换奖励：点击\"兑换\"tab\n");
        sb.append("- 查看日历：点击\"我的\"tab\n");
        sb.append("- 小组管理：点击\"小组\"tab\n\n");

        // 动态生成可用工具描述（从 ToolRegistry 获取）
        sb.append("## 可用工具\n");
        sb.append("你可以调用以下工具获取真实数据，支持多轮调用（先查一部分数据，分析后再决定是否继续查询）：\n");
        for (String toolName : toolRegistry.getAvailableToolNames(userId)) {
            sb.append("- ").append(toolName).append("\n");
        }
        sb.append("注意：普通成员无法查看其他人的数据（query_member_data 和 query_pending_requests 仅管理员可用）。\n\n");

        // 规划/反思提示词
        sb.append("## 数据查询策略\n");
        sb.append("当用户的问题需要多步推理时，请按以下策略操作：\n");
        sb.append("1. 先规划：分析用户需要哪些数据，确定查询顺序\n");
        sb.append("2. 逐步查询：先查最关键的数据，根据结果决定是否需要继续查询\n");
        sb.append("3. 综合回答：基于所有查询结果给出完整回答\n\n");
        sb.append("示例：用户问\"我比第二名多多少分\" → 先调用 query_leaderboard 获取排名 → 根据排名数据直接计算回答\n");
        sb.append("示例：用户问\"今天谁没运动\" → 先调用 query_group_info 获取成员列表 → 再逐个调用 query_member_data 查询今日记录 → 综合分析后回答\n\n");

        sb.append("## 运动记录助手\n");
        sb.append("当用户告诉你今天做了什么运动及运动量时，你需要识别记录意图。\n");
        sb.append("在回复中为每项运动添加标记：`[RECORD:{\"exerciseName\":\"运动类型名\",\"amount\":数字}]`\n");
        sb.append("- exerciseName 必须与上方运动类型列表中的名称完全一致\n");
        sb.append("- amount 为数字（如3公里则填3）\n");
        sb.append("- 用户说了多个运动时，每个运动都要添加对应的 [RECORD:...] 标记\n");
        sb.append("- 如果运动类型或运动量不明确，不要添加标记，而是追问用户\n\n");

        sb.append("请基于以上信息回答用户问题。如果用户问的不在系统范围内，礼貌说明你只能回答系统相关问题。");
        return sb.toString();
    }
}
