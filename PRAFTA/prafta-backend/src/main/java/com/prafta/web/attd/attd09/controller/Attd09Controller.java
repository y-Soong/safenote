package com.prafta.web.attd.attd09.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd09.application.param.CoverGrantParam;
import com.prafta.web.attd.attd09.application.param.HireDateGrantParam;
import com.prafta.web.attd.attd09.application.param.LeaveDashboardListParam;
import com.prafta.web.attd.attd09.application.param.LeaveDetailParam;
import com.prafta.web.attd.attd09.application.param.LeaveRecallParam;
import com.prafta.web.attd.attd09.application.param.ManualGrantParam;
import com.prafta.web.attd.attd09.application.param.ManualTypesParam;
import com.prafta.web.attd.attd09.application.param.PolicyGrantParam;
import com.prafta.web.attd.attd09.application.param.PolicyInfoParam;
import com.prafta.web.attd.attd09.application.param.RemnantReportParam;
import com.prafta.web.attd.attd09.application.param.RemnantSummaryParam;
import com.prafta.web.attd.attd09.application.param.ShortfallListParam;
import com.prafta.web.attd.attd09.application.param.UsageHistoryParam;
import com.prafta.web.attd.attd09.dto.request.BulkManualGrantRequest;
import com.prafta.web.attd.attd09.dto.request.CoverGrantRequest;
import com.prafta.web.attd.attd09.dto.request.HireDateGrantRequest;
import com.prafta.web.attd.attd09.dto.request.LeaveDashboardListRequest;
import com.prafta.web.attd.attd09.dto.request.LeaveRecallRequest;
import com.prafta.web.attd.attd09.dto.request.ManualGrantRequest;
import com.prafta.web.attd.attd09.dto.request.PolicyGrantRequest;
import com.prafta.web.attd.attd09.dto.request.ShortfallListRequest;
import com.prafta.web.attd.attd09.dto.response.CoverGrantResponse;
import com.prafta.web.attd.attd09.dto.response.HireDateGrantResponse;
import com.prafta.web.attd.attd09.dto.response.LeaveDashboardResponse;
import com.prafta.web.attd.attd09.dto.response.LeaveDetailResponse;
import com.prafta.web.attd.attd09.dto.response.LeaveRecallResponse;
import com.prafta.web.attd.attd09.dto.response.ManualGrantResponse;
import com.prafta.web.attd.attd09.dto.response.ManualTypesResponse;
import com.prafta.web.attd.attd09.dto.response.PolicyGrantPolicyInfoResponse;
import com.prafta.web.attd.attd09.dto.response.PolicyGrantPreviewResponse;
import com.prafta.web.attd.attd09.dto.response.PolicyGrantResponse;
import com.prafta.web.attd.attd09.dto.response.RemnantCoverSummaryResponse;
import com.prafta.web.attd.attd09.dto.response.RemnantReportResponse;
import com.prafta.web.attd.attd09.dto.response.ShortfallListResponse;
import com.prafta.web.attd.attd09.dto.response.UsageHistoryResponse;
import com.prafta.web.attd.attd09.service.Attd09Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * attd09 — 연차 현황 대시보드/상세/수동 부여 (PRAFTA-017-2, 정책서 §8.5).
 *
 * <p>실 경로 프리픽스는 {@code /prafta/webApi}이므로 본 컨트롤러의 매핑은
 * {@code /prafta/webApi/attd09/...}로 노출된다(프론트 axios도 {@code /webApi/attd09/...} 사용).
 *
 * <p>endpoint:
 * <ul>
 *   <li>GET  /attd09/leave-dashboard/list                  — 대시보드 목록 + 메트릭 + 부서옵션</li>
 *   <li>GET  /attd09/leave-dashboard/{userCd}/detail        — 직원별 연차 상세</li>
 *   <li>GET  /attd09/leave-dashboard/{userCd}/usage-history — 직원별 연도별 연차 사용 이력</li>
 *   <li>GET  /attd09/leave-grant/manual-types               — 수동 부여 가능 휴가 종류</li>
 *   <li>POST /attd09/leave-grant/manual-grant               — 수동 부여 (단일)</li>
 *   <li>POST /attd09/leave-grant/bulk-manual-grant          — 수동 부여 (일괄)</li>
 *   <li>POST /attd09/leave-grant/{grantId}/recall           — 수동 부여 연차 회수 (soft cancel, PRAFTA-031)</li>
 * </ul>
 *
 * <p>권한/스코프(정책서 §8.5.7, 가드레일 3):
 * <ul>
 *   <li>조회(GET): 인증 사용자 + CMPNY_CD 스코프 (JWT만 신뢰).</li>
 *   <li>수동 부여(POST): AUTH_MASTER OR AUTH_HR_MANAGER 진입부 강제 (서비스 계층).</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/attd09")
@RequiredArgsConstructor
public class Attd09Controller {

    private final Attd09Service attd09Service;
    private final JwtUtil jwtUtil;

    /** 연차 현황 대시보드 목록 조회. */
    @GetMapping("/leave-dashboard/list")
    public ResponseEntity<?> getDashboard(
            @ModelAttribute LeaveDashboardListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeaveDashboardResponse response = attd09Service.getDashboard(
                LeaveDashboardListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 직원별 연차 상세 조회. */
    @GetMapping("/leave-dashboard/{userCd}/detail")
    public ResponseEntity<?> getDetail(
            @PathVariable("userCd") String userCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeaveDetailResponse response = attd09Service.getDetail(
                LeaveDetailParam.from(userCd, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 직원별 연도별 연차 사용 이력 조회. */
    @GetMapping("/leave-dashboard/{userCd}/usage-history")
    public ResponseEntity<?> getUsageHistory(
            @PathVariable("userCd") String userCd,
            @RequestParam("year") String year,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        UsageHistoryResponse response = attd09Service.getUsageHistory(
                UsageHistoryParam.from(userCd, year, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 사용 이력 증빙 파일 열람 (연차 신청 증빙 필수화 2026-08-29).
     *
     * <p>MASTER/HR + "이 직원의 연차 사용 건에 첨부된 FILE_TYPE=008 파일" 스코프 검증(서비스 계층).
     * 공개 정적 URL 금지, 인증 스트림 서빙(SEC-1 — 근로계약서와 동일 원칙, no-store).
     */
    @GetMapping("/leave-dashboard/{userCd}/evidence-file/{fileMgmtCd}")
    public ResponseEntity<byte[]> getUsageEvidenceFile(
            @PathVariable("userCd") String userCd,
            @PathVariable("fileMgmtCd") String fileMgmtCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        com.prafta.common.cmm.file.application.model.FileBytesResult file =
                attd09Service.getUsageEvidenceFile(
                        LeaveDetailParam.from(userCd, jwtUtil.getAllClaimsAsMap(authorization)), fileMgmtCd);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(org.springframework.http.MediaType.parseMediaType(file.contentType()))
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.data());
    }

    /** 수동 부여 가능 휴가 종류 조회. */
    @GetMapping("/leave-grant/manual-types")
    public ResponseEntity<?> getManualTypes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ManualTypesResponse response = attd09Service.getManualTypes(
                ManualTypesParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 연차 수동 부여 (단일).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/leave-grant/manual-grant")
    public ResponseEntity<?> manualGrant(
            @RequestBody ManualGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ManualGrantResponse response = attd09Service.manualGrant(
                ManualGrantParam.fromSingle(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 연차 수동 부여 (일괄).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/leave-grant/bulk-manual-grant")
    public ResponseEntity<?> bulkManualGrant(
            @RequestBody BulkManualGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ManualGrantResponse response = attd09Service.manualGrant(
                ManualGrantParam.fromBulk(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 관리자 수동 부여 연차 회수 (soft cancel, PRAFTA-031).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     * 정책서 §8.5.8: STATUS='CANCELED' 소프트 처리 + 사용 이력 불변(USED_DAYS 미갱신).
     */
    @PostMapping("/leave-grant/{grantId}/recall")
    public ResponseEntity<?> recallGrant(
            @PathVariable("grantId") String grantId,
            @RequestBody(required = false) LeaveRecallRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeaveRecallResponse response = attd09Service.recallGrant(
                LeaveRecallParam.from(grantId, request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 입사일 기준 연차 부여 (테스트/검증용, 일괄). 하위호환 유지(기존 경로).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/leave-grant/hire-date-grant")
    public ResponseEntity<?> hireDateGrant(
            @RequestBody HireDateGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        HireDateGrantResponse response = attd09Service.hireDateGrant(
                HireDateGrantParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 기준 부여 프리뷰 (read-only dry-run, prafta-022 작업 D / prafta-032 처리방식 폐기 반영).
     * 적용 전 선택 직원별 신규부여/변경없음 및 추가예정 일수를 집계 반환한다(DB 쓰기 없음).
     * 처리방식별 재발급/취소 집계는 prafta-032(009)로 폐기됐다.
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/leave-grant/policy-grant/preview")
    public ResponseEntity<?> previewPolicyGrant(
            @RequestBody PolicyGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        PolicyGrantPreviewResponse response = attd09Service.previewPolicyGrant(
                PolicyGrantParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 기준 부여 적용 (일괄, prafta-022 작업 D / prafta-032 처리방식 폐기 반영).
     * 활성 연차정책·입사일·경력인정 기준으로 신규 부여만 수행한다. 기존 부여가 있는 직원은 변경 없음(멱등 skip).
     * 처리방식(handlingType) 자동 해석·취소/재발급은 prafta-032(009)로 폐기됐다(입사일 변경 화면의 수동 조정으로 이관).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/leave-grant/policy-grant")
    public ResponseEntity<?> policyGrant(
            @RequestBody PolicyGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        PolicyGrantResponse response = attd09Service.policyGrant(
                PolicyGrantParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * PC-07(D9-②): 회사 부담 보전 연간 집계 (짜투리 보전 ON 회사 — Attd_09 집계 칩).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     *
     * @param year 집계 연도(YYYY). 미지정/형식 불일치면 올해 폴백.
     */
    @GetMapping("/leave-dashboard/remnant-cover-summary")
    public ResponseEntity<?> getRemnantCoverSummary(
            @RequestParam(value = "year", required = false) String year,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RemnantCoverSummaryResponse response = attd09Service.getRemnantCoverSummary(
                RemnantSummaryParam.from(year, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * PC-07(D9-③·N2): 소멸 임박 짜투리 리포트 (짜투리 보전 OFF 회사 — Attd_09 리포트 섹션).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @GetMapping("/leave-dashboard/remnant-report")
    public ResponseEntity<?> getRemnantReport(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RemnantReportResponse response = attd09Service.getRemnantReport(
                RemnantReportParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 기준 부여 — 활성 연차정책 안내 정보 조회 (prafta-022 보완).
     * 첫해 방식(AXIS3)이 PRORATE면 차년도 일괄 폴백 안내 문구를 함께 반환한다(부여 로직 불변, 화면 노출용).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @GetMapping("/leave-grant/policy-info")
    public ResponseEntity<?> getPolicyInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        PolicyGrantPolicyInfoResponse response = attd09Service.getPolicyInfo(
                PolicyInfoParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 경력인정 이원화 Phase 2 §2-2: 입사일 기준 차액 조회 목록(read-only).
     * 게이트: ensureManager(master/hr 전용 — P-13, Attd_09 본문과 동일. 서비스 진입부에서 강제) +
     * 사업장 필터 지정 시 assertSiteAccess 방어선 유지.
     */
    @GetMapping("/leave-dashboard/shortfall/list")
    public ResponseEntity<?> getShortfallList(
            @ModelAttribute ShortfallListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShortfallListResponse response = attd09Service.getShortfallList(
                ShortfallListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 경력인정 이원화 Phase 2 §2-3: 입사일 기준 차액 보전(법정 수기부여, {@code _COVER}) 실행.
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요(기존 수동 부여 관례, 서비스 진입부에서 강제).
     */
    @PostMapping("/leave-grant/cover-grant")
    public ResponseEntity<?> coverGrant(
            @RequestBody CoverGrantRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        CoverGrantResponse response = attd09Service.coverGrant(
                CoverGrantParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
