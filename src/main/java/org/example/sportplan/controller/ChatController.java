package org.example.sportplan.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.service.ChatService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// AI 智能助手接口：接收用户消息，返回AI回复，可能包含运动记录意图
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 返回 {reply:"...", recordIntent:{exerciseTypeId,exerciseName,amount,unit}} 或 {reply:"..."}
    @PostMapping
    public ApiResponse<Map<String, Object>> send(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getSession(false).getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }

        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ApiResponse.error(400, "消息不能为空");
        }

        List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");
        if (history == null) {
            history = Collections.emptyList();
        }

        Map<String, Object> result = chatService.chat(history, message, userId);
        return ApiResponse.success(result);
    }
}
