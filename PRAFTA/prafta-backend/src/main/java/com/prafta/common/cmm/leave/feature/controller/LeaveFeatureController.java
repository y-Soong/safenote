package com.prafta.common.cmm.leave.feature.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.leave.feature.dto.response.LeaveFeatureVisibilityResponse;
import com.prafta.common.cmm.leave.feature.dto.response.MyStdWorkSummaryResponse;
import com.prafta.common.cmm.leave.feature.service.LeaveFeatureService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-06: 연차 기능 노출 판정 + 본인 소정근로(단시간 파생) 조회 — 웹/앱 공용 컨트롤러.
 *
 * <p>자동 프리픽스({@code com.prafta.common.*} → {@code /prafta/comApi}) 적용 → 실제 매핑:
 * <ul>
 *   <li>GET /prafta/comApi/leave-feature/visibility        — 연차 기능 노출 여부(회사 단위 판정)</li>
 *   <li>GET /prafta/comApi/leave-feature/std-work-summary  — 본인 소정근로 요약(단시간 파생 판정)</li>
 * </ul>
 *
 * <p><b>인가 설계 (★신규 조회 EP 게이트 — feedback_web_new_query_screen_needs_node_gate)</b>
 * <ul>
 *   <li>두 EP 모두 <b>본인 스코프 전용</b>이다. 회사코드/사용자코드를 쿼리·바디로 <b>받지 않고</b>
 *       JWT 클레임({@code gv_cmpnyCd}/{@code gv_userCd})에서만 도출한다 → 타인·타사 조회 경로가
 *       구조적으로 존재하지 않으므로(IDOR 불가) {@code canManageNode} 부서 게이트가 필요 없다.</li>
 *   <li>타인의 소정근로를 조회하는 관리자용 목록/이력(User_10, 소정-10)은 <b>본 컨트롤러가 아니라</b>
 *       별도 관리 EP 로 만들고, 그쪽에는 {@code canManageNode} 게이트를 반드시 건다.
 *       ★본 컨트롤러에 {@code userCd} 파라미터를 추가하는 순간 그 전제가 깨진다 — 추가 금지.</li>
 *   <li>PII 를 반환하지 않는다(플래그·분(minute) 값·코드만).</li>
 * </ul>
 *
 * <p>인증은 {@code AuthAspect} 가 JWT 를 강제한다(본 컨트롤러는 pointcut 대상).
 */
@Slf4j
@RestController
@RequestMapping("/leave-feature")
@RequiredArgsConstructor
public class LeaveFeatureController {

    private final LeaveFeatureService leaveFeatureService;
    private final JwtUtil jwtUtil;

    /**
     * 연차 기능 노출 판정 (회사 단위).
     *
     * <p>숨김 조건 = 법정 자동 부여 토글 off <b>그리고</b> 회사 연차 부여 이력 0건.
     * 앱 홈 연차 카드·연차 신청 진입점, 웹 연차 메뉴 노출 제어에 쓴다(일용직 게이트 패턴 계열).
     */
    @GetMapping("/visibility")
    public ResponseEntity<?> getVisibility(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        return ResponseEntity.status(HttpStatus.OK).body(
                LeaveFeatureVisibilityResponse.of(
                        leaveFeatureService.resolveVisibility(tokenInfo.gv_cmpnyCd())));
    }

    /**
     * 본인 소정근로 요약 조회 (단시간 파생 판정 포함).
     *
     * @param baseYmd 기준일(YYYYMMDD, 선택). 미지정/형식오류면 오늘 기준.
     */
    @GetMapping("/std-work-summary")
    public ResponseEntity<?> getMyStdWorkSummary(
            @RequestParam(value = "baseYmd", required = false) String baseYmd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        return ResponseEntity.status(HttpStatus.OK).body(
                MyStdWorkSummaryResponse.of(
                        leaveFeatureService.resolveMyStdWorkSummary(
                                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), baseYmd)));
    }

    /** JWT 클레임 → TokenInfo. 회사/사용자 식별 부재면 인증 결함(COMMON_400_003). */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return tokenInfo;
    }
}
