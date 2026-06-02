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
import com.prafta.web.attd.attd07.application.param.RejectUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdInfosParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailDeleteRequest;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailsRequest;
import com.prafta.web.attd.attd07.dto.request.MonthlyAttdListRequest;
import com.prafta.web.attd.attd07.dto.request.RejectUserAttdRequestRequest;
import com.prafta.web.attd.attd07.dto.request.RejectUserOvertimeRequestRequest;
import com.prafta.web.attd.attd07.dto.request.UpdateUserAttdInfosRequest;
import com.prafta.web.attd.attd07.dto.request.UpdateUserAttdRequestRequest;
import com.prafta.web.attd.attd07.dto.request.UpdateUserOvertimeRequestRequest;
import com.prafta.web.attd.attd07.application.param.AttdCloseParam;
import com.prafta.web.attd.attd07.application.param.AttdCloseStatusParam;
import com.prafta.web.attd.attd07.dto.request.AttdCloseRequest;
import com.prafta.web.attd.attd07.dto.request.AttdCloseStatusRequest;
import com.prafta.web.attd.attd07.dto.response.AttdCloseStatusResponse;
import com.prafta.web.attd.attd07.dto.response.AttdRecordListResponse;
import com.prafta.web.attd.attd07.dto.response.DailyAttdDetailsResponse;
import com.prafta.web.attd.attd07.service.Attd07Service;
import com.prafta.web.attd.attd07.service.AttdCloseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd07")
@RequiredArgsConstructor
public class Attd07Controller {

    private final Attd07Service attd07Service;
    private final AttdCloseService attdCloseService;
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
            @ModelAttribute @Valid DailyAttdDetailsRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        DailyAttdDetailsResponse response = attd07Service.getDailyAttdDetails(
                DailyAttdDetailsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/daily-attd-detail-delete")
    public ResponseEntity<?> dailyAttdDetailDelete(
            @RequestBody @Valid DailyAttdDetailDeleteRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd07Service.dailyAttdDetailDelete(
                DailyAttdDetailDeleteParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-user-attd-requests")
    public ResponseEntity<?> updateUserAttdRequest(
            @RequestBody @Valid UpdateUserAttdRequestRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd07Service.updateUserAttdRequest(
                UpdateUserAttdRequestParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-user-overtime-requests")
    public ResponseEntity<?> updateUserOvertimeRequests(
            @RequestBody @Valid UpdateUserOvertimeRequestRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd07Service.updateUserOvertimeRequests(
                UpdateUserOvertimeRequestParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** PRAFTA-008 - 근태 요청 반려. */
    @PostMapping("/reject-user-attd-requests")
    public ResponseEntity<?> rejectUserAttdRequest(
            @RequestBody @Valid RejectUserAttdRequestRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd07Service.rejectUserAttdRequest(
                RejectUserAttdRequestParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** PRAFTA-010 - 초과근무 요청 반려. */
    @PostMapping("/reject-user-overtime-requests")
    public ResponseEntity<?> rejectUserOvertimeRequest(
            @RequestBody @Valid RejectUserOvertimeRequestRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd07Service.rejectUserOvertimeRequest(
                RejectUserOvertimeRequestParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // ===== PRAFTA-019-C 근태 마감 =====

    /** 근태 마감 상태 + 차단 사유 현황 + 이력 조회. */
    @GetMapping("/attd-close-status")
    public ResponseEntity<?> getAttdCloseStatus(
            @ModelAttribute @Valid AttdCloseStatusRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        AttdCloseStatusResponse response = attdCloseService.getCloseStatus(
                AttdCloseStatusParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 근태 마감 실행 (매니저 권한 + 차단 사유 0건). */
    @PostMapping("/attd-close")
    public ResponseEntity<?> attdClose(
            @RequestBody @Valid AttdCloseRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attdCloseService.closeAttendance(
                AttdCloseParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 근태 마감 해제 (매니저 권한). */
    @PostMapping("/attd-unclose")
    public ResponseEntity<?> attdUnclose(
            @RequestBody @Valid AttdCloseRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attdCloseService.uncloseAttendance(
                AttdCloseParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
