package com.prafta.web.user.user05.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user05.application.param.DailyUserListParam;
import com.prafta.web.user.user05.dto.request.DailyUserListRequest;
import com.prafta.web.user.user05.dto.response.DailyUserListResponse;
import com.prafta.web.user.user05.service.User05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/user05")
@RequiredArgsConstructor
public class User05Controller {

    private final User05Service user05Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/daily-user-lists")
    public ResponseEntity<?> getDailyUserList(
            @ModelAttribute DailyUserListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        DailyUserListResponse response = user05Service.selectDailyUserList(
                DailyUserListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
