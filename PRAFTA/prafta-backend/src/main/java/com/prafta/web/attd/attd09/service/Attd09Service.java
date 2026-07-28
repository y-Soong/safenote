package com.prafta.web.attd.attd09.service;

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

/**
 * attd09 — 연차 현황 대시보드/상세/수동 부여 (PRAFTA-017-2, 정책서 §8.5).
 *
 * <p>비즈니스 로직은 {@code LeaveDashboardService}로 위임하는 어댑터 계층이다
 * (baim07 ↔ LeavePolicyService 동일 패턴).
 */
public interface Attd09Service {

    /** 연차 현황 대시보드 목록 + 메트릭 + 부서옵션 조회. */
    LeaveDashboardResponse getDashboard(LeaveDashboardListParam param);

    /** 직원별 연차 상세 조회. */
    LeaveDetailResponse getDetail(LeaveDetailParam param);

    /** 수동 부여 가능 휴가 종류 조회. */
    ManualTypesResponse getManualTypes(ManualTypesParam param);

    /** 연차 수동 부여(단일/일괄 공통). */
    ManualGrantResponse manualGrant(ManualGrantParam param);

    /** 관리자 수동 부여 연차 회수(soft cancel, PRAFTA-031). */
    LeaveRecallResponse recallGrant(LeaveRecallParam param);

    /** 입사일 기준 연차 부여(테스트/검증용, 일괄). */
    HireDateGrantResponse hireDateGrant(HireDateGrantParam param);

    /** 정책 기준 부여 프리뷰(read-only dry-run). 적용 전 집계 반환. (prafta-022 작업 D) */
    PolicyGrantPreviewResponse previewPolicyGrant(PolicyGrantParam param);

    /** 정책 기준 부여 적용(일괄). 엔진 hireDateGrant 위임. (prafta-022 작업 D) */
    PolicyGrantResponse policyGrant(PolicyGrantParam param);

    /**
     * 정책 기준 부여 — 활성 연차정책 안내 정보 조회(읽기 전용). (prafta-022 보완)
     *
     * <p>첫해 방식(AXIS3)이 PRORATE면 차년도 일괄 폴백 안내 문구를 함께 반환한다(부여 로직 불변).
     */
    PolicyGrantPolicyInfoResponse getPolicyInfo(PolicyInfoParam param);

    /** PC-07(D9-②): 회사 부담 보전 연간 집계(N일/M건 + 상세 목록). 관리자(MASTER/HR) 전용. */
    RemnantCoverSummaryResponse getRemnantCoverSummary(RemnantSummaryParam param);

    /** PC-07(D9-③·N2): 소멸 임박 짜투리 리포트(절사 끝수 구분 포함). 관리자(MASTER/HR) 전용. */
    RemnantReportResponse getRemnantReport(RemnantReportParam param);
}
