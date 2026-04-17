package com.prafta.web.attd.attd05.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.dto.request.UserWorkPlansRequest;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;
import com.prafta.web.attd.attd05.service.Attd05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd05")
@RequiredArgsConstructor
public class Attd05Controller {

    private final Attd05Service attd05Service;
    private final JwtUtil jwtUtil;
    
    @GetMapping("/user-work-plans")
    public ResponseEntity<?> getUserWorkPlan(@ModelAttribute UserWorkPlansRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	UserWorkPlansResponse response = attd05Service.getUserWorkPlan(UserWorkPlansParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
//
//    @PostMapping("/save-attd-std-time-rules")
//    public ResponseEntity<?> saveAttdStdTimeRule(@Valid @RequestBody AttdStdTimeRuleRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
//
//        attd04Service.saveAttdStdTimeRule(AttdStdTimeRuleParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
//
//        return ResponseEntity.status(HttpStatus.OK).build();
//    }
}
