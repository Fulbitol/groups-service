package com.matchpoint.groups_service.controller;

import com.matchpoint.groups_service.dto.request.JoinRequestRequest;
import com.matchpoint.groups_service.dto.response.JoinRequestResponse;
import com.matchpoint.groups_service.service.JoinRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @PostMapping("/api/v1/groups/{groupId}/join-requests")
    public ResponseEntity<JoinRequestResponse> create(@PathVariable Long groupId,
                                                        @Valid @RequestBody JoinRequestRequest request) {
        JoinRequestResponse response = joinRequestService.createJoinRequest(groupId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/join-requests/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/api/v1/join-requests/{id}/accept")
    public ResponseEntity<JoinRequestResponse> accept(@PathVariable Long id) {
        JoinRequestResponse response = joinRequestService.acceptJoinRequest(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/v1/join-requests/{id}/reject")
    public ResponseEntity<JoinRequestResponse> reject(@PathVariable Long id) {
        JoinRequestResponse response = joinRequestService.rejectJoinRequest(id);
        return ResponseEntity.ok(response);
    }
}
