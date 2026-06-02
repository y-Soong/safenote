package com.prafta.web.attd.attd04.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleParam;
import com.prafta.web.attd.attd04.application.param.AttdStdTimeRuleListParam;
import com.prafta.web.attd.attd04.dto.request.AttdStdTimeRuleRequest;
import com.prafta.web.attd.attd04.dto.response.AttdStdTimeRuleListResponse;
import com.prafta.web.attd.attd04.service.Attd04Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd04")
@RequiredArgsConstructor
public class Attd04Controller {

    private final Attd04Service attd04Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/attd-std-time-rule-lists")
    public ResponseEntity<?> getAttdStdTimeRuleList(@RequestHeader(value = "Authorization", required = false) String authorization) {

        AttdStdTimeRuleListResponse response = attd04Service.getAttdStdTimeRuleList(AttdStdTimeRuleListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/save-attd-std-time-rules")
    public ResponseEntity<?> saveAttdStdTimeRule(@Valid @RequestBody AttdStdTimeRuleRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd04Service.saveAttdStdTimeRule(AttdStdTimeRuleParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
