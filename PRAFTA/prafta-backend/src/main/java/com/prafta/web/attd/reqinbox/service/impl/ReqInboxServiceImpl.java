package com.prafta.web.attd.reqinbox.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.cmm.siteauth.result.AccessibleSiteResult;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd07.util.AttdReqTypeUtils;
import com.prafta.web.attd.reqinbox.dto.response.ApprovalLineResponse;
import com.prafta.web.attd.reqinbox.dto.response.ProcessedReqListResponse;
import com.prafta.web.attd.reqinbox.mapper.ReqInboxMapper;
import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;
import com.prafta.web.attd.reqinbox.result.ReqSummaryResult;
import com.prafta.web.attd.reqinbox.service.ReqInboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** {@link ReqInboxService} 구현 (prafta-019 후속, 접수함다중사업장권한확장-002). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReqInboxServiceImpl implements ReqInboxService {

    // 결재 단계 상태 [SYS044] - ApprovalStepGateServiceImpl/AppLeaveApprovalServiceImpl 과 동일 코드값
    // (공유 테이블 TB_USER_ATTD_REQ_APPROVAL). '01' = 현재 처리 대상인 진행중 단계.
    private static final String STEP_APPLIED = "01";

    private final ReqInboxMapper reqInboxMapper;
    private final SiteAccessService siteAccessService;
    private final ApprovalLineMapper approvalLineMapper;

    @Override
    public List<PendingReqResult> getPendingRequests(String cmpnyCd, String siteCd, String userCd, String authCd,
                                                      String reqTypeGroup, String reqSiteCd) {
        // 매니저 전용 게이트. JWT 기반 authCd를 사용하므로 body 위조로 권한 escalation 불가
        // (reject endpoint 와 동일 패턴). 일반 작업자의 대기요청·요청자명 열람 차단.
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("reqinbox pending rejected - insufficient privilege. authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        List<String> reqTypes;
        if ("correction".equals(reqTypeGroup)) {
            // 근태 생성('01')/수정('02') — AttdReqTypeUtils.isAttendanceReqType 와 동일 allow-list.
            reqTypes = List.of("01", "02");
        } else if ("overtime".equals(reqTypeGroup)) {
            // 초과근무 생성('03')/수정('04') — AttdReqTypeUtils.isOvertimeReqType allow-list.
            // (PRAFTA-025: 초과근무 수정('04') 승인·반려가 구현되어 접수함에도 함께 노출한다.
            //  승인 시 03=새 OT INSERT / 04=기존 OT(TARGET_ID) UPDATE 로 분기 처리된다.)
            reqTypes = List.of("03", "04");
        } else {
            // 스케줄 수정('10')은 컬럼 세트가 달라 getPendingSchedRequests 전용 경로로 처리한다(plan 결정 B).
            // 그 외 값은 미지원 — fail-closed.
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        List<String> siteCds = resolveSiteCds(cmpnyCd, userCd, authCd, siteCd, reqSiteCd);
        if (siteCds.isEmpty()) {
            // 접근 가능 사업장 원장이 비어있는 극단 케이스 방어 — SQL IN() 빈 목록 오류 예방.
            return List.of();
        }
        return reqInboxMapper.selectPendingRequests(cmpnyCd, siteCds, reqTypes);
    }

    @Override
    public List<PendingSchedReqResult> getPendingSchedRequests(String cmpnyCd, String siteCd, String userCd,
                                                               String authCd, String reqSiteCd) {
        // 매니저 전용 게이트 — getPendingRequests 와 동일 규칙(JWT 기반 authCd, body 위조로 escalation 불가).
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("reqinbox pending rejected - insufficient privilege. authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        List<String> siteCds = resolveSiteCds(cmpnyCd, userCd, authCd, siteCd, reqSiteCd);
        if (siteCds.isEmpty()) {
            return List.of();
        }
        return reqInboxMapper.selectPendingSchedRequests(
                cmpnyCd, siteCds, AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY);
    }

    @Override
    public List<PendingSchedReqResult> getPendingDefaultSchChangeRequests(String cmpnyCd, String siteCd,
                                                                            String userCd, String authCd,
                                                                            String reqSiteCd) {
        // 매니저 전용 게이트 — getPendingSchedRequests 와 동일 규칙(JWT 기반 authCd, body 위조로 escalation 불가).
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("reqinbox pending rejected - insufficient privilege. authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        List<String> siteCds = resolveSiteCds(cmpnyCd, userCd, authCd, siteCd, reqSiteCd);
        if (siteCds.isEmpty()) {
            return List.of();
        }
        // 유효버전 판정 기준일 = 명일(applyDefaultSchChange 의 "명일부터" 정책과 표시 정합, §조사 2번 근거).
        String asOfYmd = java.time.LocalDate.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return reqInboxMapper.selectPendingDefaultSchChangeRequests(
                cmpnyCd, siteCds, AttdReqTypeUtils.REQ_TYPE_DEFAULT_SCH_CHANGE, asOfYmd);
    }

    @Override
    public ProcessedReqListResponse getProcessedRequests(String cmpnyCd, String siteCd, String userCd,
                                                         String authCd, String reqTypeGroup, String reqSiteCd) {
        // 매니저 전용 게이트 — 대기 목록과 동일 규칙(JWT 기반 authCd, body 위조로 escalation 불가).
        // 조회 자체는 "처리자 = 본인" 스코프라 타인 데이터 열람이 성립하지 않지만,
        // 요청자명 노출 화면이므로 신규 조회 EP 게이트 원칙에 따라 동일하게 막는다.
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("reqinbox processed rejected - insufficient privilege. authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        List<String> siteCds = resolveSiteCds(cmpnyCd, userCd, authCd, siteCd, reqSiteCd);

        // 연차 탭: 결재라인 이력(사업장 무관, §0.2-3 무수정) + 연차 변경 확인 이력(보조 섹션, 사업장 스코프 적용)
        if ("leave".equals(reqTypeGroup)) {
            return ProcessedReqListResponse.builder()
                    .processedList(reqInboxMapper.selectProcessedLeaveApprovals(cmpnyCd, userCd))
                    .leaveChangeList(siteCds.isEmpty()
                            ? List.of()
                            : reqInboxMapper.selectProcessedLeaveChangeRequests(cmpnyCd, siteCds, userCd))
                    .build();
        }

        if (siteCds.isEmpty()) {
            // 접근 가능 사업장 원장이 비어있는 극단 케이스 방어 — SQL IN() 빈 목록 오류 예방.
            return ProcessedReqListResponse.builder().processedList(List.of()).leaveChangeList(List.of()).build();
        }

        List<String> reqTypes;
        if ("correction".equals(reqTypeGroup)) {
            reqTypes = List.of("01", "02");
        } else if ("overtime".equals(reqTypeGroup)) {
            reqTypes = List.of("03", "04");
        } else if ("schedule".equals(reqTypeGroup)) {
            reqTypes = List.of(AttdReqTypeUtils.REQ_TYPE_SCHED_MODIFY);
        } else if ("defaultSchChange".equals(reqTypeGroup)) {
            // selectProcessedRequests SQL 자체는 REQ_TYPE 필터가 제네릭하다(§조사 1번 근거) — 신규 쿼리 불필요.
            reqTypes = List.of(AttdReqTypeUtils.REQ_TYPE_DEFAULT_SCH_CHANGE);
        } else {
            // 미지원 그룹 — fail-closed(대기 목록과 동일).
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return ProcessedReqListResponse.builder()
                .processedList(reqInboxMapper.selectProcessedRequests(cmpnyCd, siteCds, userCd, reqTypes))
                .leaveChangeList(List.of())
                .build();
    }

    @Override
    public List<AccessibleSiteResult> getAccessibleSites(String cmpnyCd, String userCd, String authCd) {
        // 매니저 전용 게이트 — 목록 조회 endpoint 와 동일 규칙(프론트 셀렉터 옵션도 요청자명과 같은 관리 정보로 취급).
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("reqinbox accessible-sites rejected - insufficient privilege. authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        return siteAccessService.getAccessibleSites(cmpnyCd, userCd, authCd);
    }

    @Override
    public ApprovalLineResponse getApprovalLine(String cmpnyCd, String siteCd, String userCd, String authCd,
                                                String reqId, String reqTypeGroup) {
        boolean isLeave = "leave".equals(reqTypeGroup);
        if (!isLeave && !"correction".equals(reqTypeGroup) && !"overtime".equals(reqTypeGroup)
                && !"schedule".equals(reqTypeGroup) && !"defaultSchChange".equals(reqTypeGroup)) {
            // 미지원 그룹 - fail-closed(다른 reqinbox 조회 endpoint 와 동일 원칙).
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        ReqSummaryResult reqSummary = reqInboxMapper.selectReqSummaryByReqId(cmpnyCd, reqId);
        if (reqSummary == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }

        // QA 재작업(P3-1) - reqTypeGroup(클라이언트 주장 라벨)과 실제 REQ_TYPE 정합성 검증.
        // 이 검증은 아래 매니저 게이트/소유권 검증 분기(isLeave 판정) 이전에 수행해야 한다 - 그렇지
        // 않으면 예를 들어 correction/overtime/schedule 요청의 reqId 를 reqTypeGroup=leave 로 조회해
        // 매니저 게이트를 우회하고 소유권 검증(isRequester) 경로로 통과할 수 있다(타입 혼동).
        if (!reqTypeGroupMatchesActualType(reqTypeGroup, reqSummary.reqType())) {
            log.warn("reqinbox approval-line reqTypeGroup 불일치 - reqTypeGroup={}, actualReqType={}, reqId={}",
                    reqTypeGroup, reqSummary.reqType(), reqId);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (!isLeave && !AuthRoleUtils.isManager(authCd)) {
            // correction/overtime/schedule: 매니저(master/hr) 전용 게이트 - getPendingRequests 와 동일 규칙.
            log.warn("reqinbox approval-line rejected - insufficient privilege. authCd={}, reqTypeGroup={}",
                    authCd, reqTypeGroup);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(cmpnyCd, reqId);

        if (isLeave) {
            // leave: 매니저 게이트 없음. 결재선 결재자 실존(AppLeaveApprovalServiceImpl.findMyStep 과 동일
            // 패턴) 또는 요청자 본인 또는 canManageAllNodes 중 하나가 아니면 IDOR 차단.
            boolean isApprover = isApproverInSteps(steps, userCd);
            boolean isRequester = userCd != null && userCd.equals(reqSummary.userCd());
            if (!isApprover && !isRequester && !AuthRoleUtils.canManageAllNodes(authCd)) {
                log.warn("reqinbox approval-line(leave) IDOR 차단 - userCd={}, reqId={}", userCd, reqId);
                throw new ApiException(AttdErrorCode.ATTD_403_002);
            }
        } else {
            // correction/overtime/schedule: 요청 소속 사업장이 caller 접근 가능 사업장에 포함되는지 검증
            // (IDOR 가드, 실패 시 COMMON_403_003) - resolveSiteCds 의 assertSiteAccess 재사용.
            siteAccessService.assertSiteAccess(cmpnyCd, userCd, authCd, siteCd, reqSummary.siteCd());
        }

        return buildApprovalLineResponse(steps, userCd, authCd);
    }

    /**
     * canProcess/currentApproverUserNm 읽기 전용 산출(근태결재선통합 P3-1 plan §1 결정 D).
     *
     * <p>{@code ApprovalStepGateService.resolveProcessableStep} 은 예외를 던지고 orphan REQ 를 만나면
     * 결재선을 lazy INSERT 하는 부작용이 있어 GET 요청에서 재사용하지 않는다 - steps 배열에서
     * {@code approvalStatus=='01'}(진행중)인 첫 항목을 찾아 직접 판정한다. 그런 항목이 없으면(전부
     * 처리완료 또는 orphan) canProcess=false, currentApproverUserNm=null.
     */
    private ApprovalLineResponse buildApprovalLineResponse(List<ApprovalStepVO> steps, String callerUserCd,
                                                            String authCd) {
        String currentApproverUserNm = null;
        boolean canProcess = false;
        if (steps != null) {
            for (ApprovalStepVO s : steps) {
                if (STEP_APPLIED.equals(s.getApprovalStatus())) {
                    currentApproverUserNm = s.getApproverUserNm();
                    canProcess = (callerUserCd != null && callerUserCd.equals(s.getApproverUserCd()))
                            || AuthRoleUtils.canManageAllNodes(authCd);
                    break;
                }
            }
        }
        return ApprovalLineResponse.builder()
                .steps(steps == null ? List.of() : steps)
                .canProcess(canProcess)
                .currentApproverUserNm(currentApproverUserNm)
                .build();
    }

    /**
     * QA 재작업(P3-1) - reqTypeGroup(클라이언트 주장 라벨)이 실제 REQ_TYPE 과 대응하는지 검증한다.
     * 매핑은 {@link AttdReqTypeUtils} 의 기존 allow-list(다른 reqinbox/근태 endpoint 와 공유하는
     * 단일 출처)를 그대로 재사용한다: correction↔01/02, overtime↔03/04, schedule↔10, leave↔05/06,
     * defaultSchChange↔14(PRAFTA-002).
     */
    private boolean reqTypeGroupMatchesActualType(String reqTypeGroup, String actualReqType) {
        if ("leave".equals(reqTypeGroup)) {
            return AttdReqTypeUtils.isLeaveReqType(actualReqType);
        }
        if ("correction".equals(reqTypeGroup)) {
            return AttdReqTypeUtils.isAttendanceReqType(actualReqType);
        }
        if ("overtime".equals(reqTypeGroup)) {
            return AttdReqTypeUtils.isOvertimeReqType(actualReqType);
        }
        if ("schedule".equals(reqTypeGroup)) {
            return AttdReqTypeUtils.isScheduleModifyReqType(actualReqType);
        }
        if ("defaultSchChange".equals(reqTypeGroup)) {
            return AttdReqTypeUtils.isDefaultSchChangeReqType(actualReqType);
        }
        return false;
    }

    /** 결재선에 userCd 가 결재자로 실존하는지(단계 무관) - AppLeaveApprovalServiceImpl.findMyStep 과 동일 패턴. */
    private boolean isApproverInSteps(List<ApprovalStepVO> steps, String userCd) {
        if (steps == null || userCd == null) {
            return false;
        }
        for (ApprovalStepVO s : steps) {
            if (userCd.equals(s.getApproverUserCd())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 목록 조회 사업장 스코프 해석(접수함다중사업장권한확장-002).
     *
     * <p>{@code reqSiteCd} 가 있으면 접근 가능 여부를 개별 검증(IDOR 가드, 실패 시
     * {@code COMMON_403_003})한 후 그 1건으로 좁힌다 — {@code assertSiteAccess} 가 이미
     * master/hr 전사 허용 → 토큰 사업장 fast path → 원장 순으로 판정하므로 "접근 가능 목록에
     * 포함되는지" 검증과 동등하다. 없으면 접근 가능 사업장 전체를 사용한다.
     */
    private List<String> resolveSiteCds(String cmpnyCd, String userCd, String authCd, String gvSiteCd,
                                         String reqSiteCd) {
        if (reqSiteCd != null && !reqSiteCd.isBlank()) {
            siteAccessService.assertSiteAccess(cmpnyCd, userCd, authCd, gvSiteCd, reqSiteCd);
            return List.of(reqSiteCd);
        }
        return siteAccessService.getAccessibleSites(cmpnyCd, userCd, authCd).stream()
                .map(AccessibleSiteResult::siteCd)
                .toList();
    }
}
