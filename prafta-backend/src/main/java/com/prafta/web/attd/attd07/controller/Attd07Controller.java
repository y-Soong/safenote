package com.prafta.web.attd.attd07.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdInfosParam;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailDeleteRequest;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailsRequest;
import com.prafta.web.attd.attd07.dto.request.MonthlyAttdListRequest;
import com.prafta.web.attd.attd07.dto.request.UpdateUserAttdInfosRequest;
import com.prafta.web.attd.attd07.dto.response.AttdRecordListResponse;
import com.prafta.web.attd.attd07.dto.response.DailyAttdDetailsResponse;
import com.prafta.web.attd.attd07.service.Attd07Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd07")
@RequiredArgsConstructor
public class Attd07Controller {

    private final Attd07Service attd07Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/monthly-attd-lists")
    public ResponseEntity<?> getMonthlyAttdList(
            @ModelAttribute MonthlyAttdListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        AttdRecordListResponse response = attd07Service.getMonthlyAttdList(
        		MonthlyAttdListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/update-user-attd-infos")
    public ResponseEntity<?> updateUserAttdInfos(
            @RequestBody List<UpdateUserAttdInfosRequest> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd07Service.updateUserAttdInfos(
                UpdateUserAttdInfosParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/daily-attd-details")
    public ResponseEntity<?> getDailyAttdDetails(
            @ModelAttribute DailyAttdDetailsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        DailyAttdDetailsResponse response = attd07Service.getDailyAttdDetails(
                DailyAttdDetailsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/daily-attd-detail-delete")
    public ResponseEntity<?> dailyAttdDetailDelete(
            @RequestBody DailyAttdDetailDeleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd07Service.dailyAttdDetailDelete(
                DailyAttdDetailDeleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
