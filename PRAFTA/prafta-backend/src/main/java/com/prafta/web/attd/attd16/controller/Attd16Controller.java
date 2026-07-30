package com.prafta.web.attd.attd16.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd16.application.param.LeaveUsageCalendarParam;
import com.prafta.web.attd.attd16.dto.request.LeaveUsageCalendarRequest;
import com.prafta.web.attd.attd16.dto.response.LeaveUsageCalendarResponse;
import com.prafta.web.attd.attd16.service.Attd16Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ATTD16-T1 - 연차 사용 현황 캘린더 컨트롤러(읽기 전용).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.web.* 에 /prafta/webApi 자동 부여):
 * <ul>
 *   <li>GET /prafta/webApi/attd16/leave-usage-calendar (월별 연차 사용 실적 일자 전개 조회)</li>
 * </ul>
 * 식별값(회사/권한/사용자/토큰 사업장)은 JWT 에서만 도출한다(IDOR 차단). 쓰기 EP 는 없다.
 */
@Slf4j
@RestController
@RequestMapping("/attd16")
@RequiredArgsConstructor
public class Attd16Controller {

    private final Attd16Service attd16Service;
    private final JwtUtil jwtUtil;

    /** 월별 연차 사용 현황 조회. (프론트: /webApi/attd16/leave-usage-calendar) */
    @GetMapping("/leave-usage-calendar")
    public ResponseEntity<?> getLeaveUsageCalendar(
            @ModelAttribute LeaveUsageCalendarRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeaveUsageCalendarResponse response = attd16Service.getLeaveUsageCalendar(
                LeaveUsageCalendarParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
