package com.prafta.app.attd.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.attd.admin.application.param.AdminDailyAttdParam;
import com.prafta.app.attd.admin.application.param.AdminMonthlyAttdParam;
import com.prafta.app.attd.admin.dto.response.DailyAttdResponse;
import com.prafta.app.attd.admin.dto.response.MonthlyAttdResponse;
import com.prafta.app.attd.admin.service.AppAdminAttdService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * J1-5: 모바일 앱 관리자 모드 근태 상세 컨트롤러(조회 전용 — 일자/월별).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET /prafta/appApi/admin/attd-detail/daily   (일자 근태 현황)</li>
 *   <li>GET /prafta/appApi/admin/attd-detail/monthly (월별 집계)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/admin/attd-detail/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/authCd/siteCd 는 JWT 클레임에서만 도출하며
 *   path/query 로 식별자를 받지 않는다. nodeCd 는 리소스 키이며 서버가 토큰 스코프 내인지 재검증한다(스코프 밖이면 빈 결과).
 *
 * <p>권한([권한매트릭스 §3]): ATTD_DETAIL = master ∥ hr ∥ nodeAdmin (safe 단독 ⛔). 서버가 강제(A-1 게이트 부재 결함 비상속).
 * <p>PII(휴대폰/이메일) 미노출 — 이름·노드명만. 조회 전용(쓰기 부수효과 없음).
 */
@Slf4j
@RestController
@RequestMapping("/admin/attd-detail")
@RequiredArgsConstructor
public class AppAdminAttdController {

    private final AppAdminAttdService appAdminAttdService;
    private final JwtUtil jwtUtil;

    /** 일자 근태 현황(workYmd 필수, nodeCd/keyword/page/pageSize 선택). */
    @GetMapping("/daily")
    public ResponseEntity<?> getDaily(
            @RequestParam("workYmd") String workYmd,
            @RequestParam(value = "nodeCd", required = false) String nodeCd,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        DailyAttdResponse response = appAdminAttdService.selectDaily(
                AdminDailyAttdParam.of(workYmd, nodeCd, keyword, page, pageSize, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 월별 집계(yearMonth 필수, nodeCd/keyword/page/pageSize 선택). */
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(
            @RequestParam("yearMonth") String yearMonth,
            @RequestParam(value = "nodeCd", required = false) String nodeCd,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        MonthlyAttdResponse response = appAdminAttdService.selectMonthly(
                AdminMonthlyAttdParam.of(yearMonth, nodeCd, keyword, page, pageSize, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
