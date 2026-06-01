package org.example.sportplan.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateGroupRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.GroupMemberResponse;
import org.example.sportplan.dto.response.GroupResponse;
import org.example.sportplan.dto.response.JoinRequestResponse;
import org.example.sportplan.service.GroupService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ApiResponse<List<GroupResponse>> list(HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        return ApiResponse.success(groupService.getAllGroups(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> get(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        return ApiResponse.success(groupService.getGroup(id, userId));
    }

    @PostMapping
    public ApiResponse<GroupResponse> create(
            @Valid @RequestBody CreateGroupRequest body,
            HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        return ApiResponse.success(groupService.createGroup(userId, body));
    }

    @PostMapping("/{id}/join")
    public ApiResponse<Void> requestJoin(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        groupService.requestJoinGroup(id, userId);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/join-requests")
    public ApiResponse<List<JoinRequestResponse>> pendingRequests(
            @PathVariable Long id, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        return ApiResponse.success(groupService.getPendingRequests(id, userId));
    }

    @GetMapping("/my-requests")
    public ApiResponse<List<JoinRequestResponse>> myRequests(HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        return ApiResponse.success(groupService.getMyPendingRequests(userId));
    }

    @PostMapping("/join-requests/{requestId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long requestId, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        groupService.approveRequest(requestId, userId);
        return ApiResponse.success();
    }

    @PostMapping("/join-requests/{requestId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long requestId, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        groupService.rejectRequest(requestId, userId);
        return ApiResponse.success();
    }

    @PostMapping("/leave")
    public ApiResponse<Void> leave(HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        groupService.leaveGroup(userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> dissolve(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        groupService.dissolveGroup(id, userId);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> members(
            @PathVariable Long id, HttpServletRequest request) {
        Long userId = getSessionUserId(request);
        return ApiResponse.success(groupService.getMembers(id, userId));
    }

    @PostMapping("/{groupId}/set-admin/{userId}")
    public ApiResponse<Void> setAdmin(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = getSessionUserId(request);
        groupService.setMemberAdmin(groupId, userId, currentUserId);
        return ApiResponse.success();
    }

    private Long getSessionUserId(HttpServletRequest request) {
        return (Long) request.getSession(false).getAttribute("userId");
    }
}
