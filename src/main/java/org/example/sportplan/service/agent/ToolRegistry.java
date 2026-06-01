package org.example.sportplan.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

// 工具注册中心：自动收集所有 AgentTool 实现，提供按权限查询和执行能力
@Slf4j
@Service
public class ToolRegistry {

    private final Map<String, AgentTool> toolMap = new LinkedHashMap<>();

    // Spring 自动注入所有 AgentTool 实现
    public ToolRegistry(List<AgentTool> tools) {
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            log.info("注册 Agent 工具: {}", tool.getName());
        }
    }

    // 获取当前用户可用的工具列表（转换为 OpenAI function calling 格式）
    public List<Map<String, Object>> getAvailableTools(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentTool tool : toolMap.values()) {
            if (tool.isAvailable(userId)) {
                Map<String, Object> func = new HashMap<>();
                func.put("name", tool.getName());
                func.put("description", tool.getDescription());
                func.put("parameters", tool.getParameters());
                Map<String, Object> toolDef = new HashMap<>();
                toolDef.put("type", "function");
                toolDef.put("function", func);
                result.add(toolDef);
            }
        }
        return result;
    }

    // 获取当前用户可用的工具名称列表（用于系统提示词）
    public List<String> getAvailableToolNames(Long userId) {
        List<String> names = new ArrayList<>();
        for (AgentTool tool : toolMap.values()) {
            if (tool.isAvailable(userId)) {
                names.add(tool.getName());
            }
        }
        return names;
    }

    // 按名称执行工具
    public String executeTool(String name, Map<String, Object> args, Long userId) {
        AgentTool tool = toolMap.get(name);
        if (tool == null) {
            return "{\"error\":\"未知工具: " + name + "\"}";
        }
        if (!tool.isAvailable(userId)) {
            return "{\"error\":\"无权限使用工具: " + name + "\"}";
        }
        try {
            return tool.execute(args, userId);
        } catch (Exception e) {
            log.error("执行工具{}失败: {}", name, e.getMessage());
            return "{\"error\":\"查询失败：" + e.getMessage() + "\"}";
        }
    }
}
