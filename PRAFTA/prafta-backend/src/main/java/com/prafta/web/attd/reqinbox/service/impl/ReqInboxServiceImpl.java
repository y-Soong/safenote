package com.prafta.web.attd.reqinbox.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.siteauth.result.AccessibleSiteResult;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd07.util.AttdReqTypeUtils;
import com.prafta.web.attd.reqinbox.dto.response.ProcessedReqListResponse;
import com.prafta.web.attd.reqinbox.mapper.ReqInboxMapper;
import com.prafta.web.attd.reqinbox.result.PendingReqResult;
import com.prafta.web.attd.reqinbox.result.PendingSchedReqResult;
import com.prafta.web.attd.reqinbox.service.ReqInboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** {@link ReqInboxService} 구현 (prafta-019 후속, 접수함다중사업장권한확장-002). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReqInboxServiceImpl implements ReqInboxService {

    private final ReqInboxMapper reqInboxMapper;
    private final SiteAccessService siteAccessService;

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
