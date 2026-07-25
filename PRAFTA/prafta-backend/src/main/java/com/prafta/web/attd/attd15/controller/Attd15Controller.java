package com.prafta.web.attd.attd15.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd15.application.param.Weekly52hListsParam;
import com.prafta.web.attd.attd15.dto.request.Weekly52hListsRequest;
import com.prafta.web.attd.attd15.dto.response.Weekly52hListsResponse;
import com.prafta.web.attd.attd15.service.Attd15Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ATTD15-T1 - 주52시간 관리 컨트롤러.
 * 읽기 전용 조회 API. Attd11 컨트롤러 GET 패턴 동일.
 */
@Slf4j
@RestController
@RequestMapping("/attd15")
@RequiredArgsConstructor
public class Attd15Controller {

    private final Attd15Service attd15Service;
    private final JwtUtil jwtUtil;

    /** 주52시간 관리 조회. (프론트: /webApi/attd15/weekly-52h-lists) */
    @GetMapping("/weekly-52h-lists")
    public ResponseEntity<?> getWeekly52hLists(
            @ModelAttribute Weekly52hListsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Weekly52hListsResponse response = attd15Service.getWeekly52hLists(
                Weekly52hListsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
