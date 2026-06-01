package org.example.sportplan.service.agent;

import java.util.Map;

// AI Agent 工具接口：所有可被 AI 调用的工具都实现此接口，由 ToolRegistry 自动注册
public interface AgentTool {

    // 工具名称（唯一标识，如 "query_my_data"）
    String getName();

    // 工具描述（告诉 AI 这个工具做什么，何时使用）
    String getDescription();

    // 工具参数定义（OpenAI function calling 的 parameters JSON Schema 格式）
    Map<String, Object> getParameters();

    // 判断当前用户是否有权使用此工具
    boolean isAvailable(Long userId);

    // 执行工具并返回结果（JSON 字符串）
    String execute(Map<String, Object> args, Long userId);
}
