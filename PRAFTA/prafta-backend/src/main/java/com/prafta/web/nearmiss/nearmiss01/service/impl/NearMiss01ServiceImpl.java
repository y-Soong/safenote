package com.prafta.web.nearmiss.nearmiss01.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.nearmiss.NearMissErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.nearmiss.nearmiss01.application.command.AssessmentTransferCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.ChangeStatusCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.InsertIncidentCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.SaveIncidentCommand;
import com.prafta.web.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentInfoParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentListParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.ReclassifyParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.SaveIncidentParam;
import com.prafta.web.nearmiss.nearmiss01.application.query.IncidentInfoQuery;
import com.prafta.web.nearmiss.nearmiss01.application.query.IncidentListQuery;
import com.prafta.web.nearmiss.nearmiss01.application.query.NearMissIdSeqQuery;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.ReclassifyResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.StatusCountResponse;
import com.prafta.web.nearmiss.nearmiss01.mapper.NearMiss01Mapper;
import com.prafta.web.nearmiss.nearmiss01.result.IncidentResult;
import com.prafta.web.nearmiss.nearmiss01.result.StatusCountResult;
import com.prafta.web.nearmiss.nearmiss01.service.NearMiss01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NearMiss01ServiceImpl implements NearMiss01Service {

    private static final String STATUS_RECEIVED  = "100"; // 접수
    private static final String STATUS_REVIEWING = "200"; // 검토중
    private static final String STATUS_ACTING    = "300"; // 조치중
    private static final String STATUS_COMPLETED = "400"; // 완료
    private static final String STATUS_REJECTED  = "900"; // 반려

    // 허용 전이: 100->200->300->400, 어디서든 900(반려). 그 외 전이는 422.
    private static final Map<String, String> FORWARD_TRANSITION = Map.of(
        STATUS_RECEIVED, STATUS_REVIEWING
        , STATUS_REVIEWING, STATUS_ACTING
        , STATUS_ACTING, STATUS_COMPLETED
    );

    private final NearMiss01Mapper nearMiss01Mapper;

    @Override
    public IncidentListResponse selectIncidentList(IncidentListParam param) {
        log.info("아차사고 목록 조회 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<IncidentResult> incidentResultList =
            nearMiss01Mapper.selectIncidentList(IncidentListQuery.from(param));

        return IncidentListResponse.builder()
            .incidentResultList(incidentResultList)
            .build();
    }

    @Override
    public IncidentInfoResponse selectIncidentInfo(IncidentInfoParam param) {
        log.info("아차사고 상세 조회 진입 - cmpnyCd={}, siteCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        IncidentResult incidentInfo =
            nearMiss01Mapper.selectIncidentInfo(IncidentInfoQuery.from(param));

        if (incidentInfo == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }

        return IncidentInfoResponse.builder()
            .incidentInfo(incidentInfo)
            .build();
    }

    @Override
    public StatusCountResponse selectStatusCounts(IncidentListParam param) {
        log.info("아차사고 상태카운트 조회 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        StatusCountResult statusCount =
            nearMiss01Mapper.selectStatusCounts(IncidentListQuery.from(param));

        return StatusCountResponse.builder()
            .statusCount(statusCount)
            .build();
    }

    @Override
    @Transactional
    public void saveIncident(SaveIncidentParam param) {
        log.info("아차사고 정밀조사 저장 진입 - cmpnyCd={}, siteCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        // 사업장 스코프 존재 확인(IDOR 차단)
        ensureExists(param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        int updated = nearMiss01Mapper.updateIncident(SaveIncidentCommand.from(param));
        if (updated == 0) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
        log.info("아차사고 정밀조사 저장 완료 - nearMissId={}", param.nearMissId());
    }

    @Override
    @Transactional
    public void changeStatus(ChangeStatusParam param) {
        log.info("아차사고 상태전환 진입 - cmpnyCd={}, siteCd={}, nearMissId={}, target={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId(), param.reportStatusCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        String target = param.reportStatusCd();
        if (!StringUtils.hasText(target)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 현재 상태 조회(사업장 스코프). 없으면 404.
        String current = nearMiss01Mapper.selectReportStatus(
            new IncidentInfoQuery(param.siteCd(), param.nearMissId(), param.gvCmpnyCd()));
        if (current == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }

        // 반려(900): 어느 단계든 가능. 단 사유 필수.
        if (STATUS_REJECTED.equals(target)) {
            if (!StringUtils.hasText(param.rejectReason())) {
                throw new ApiException(NearMissErrorCode.NEARMISS_400_001);
            }
        } else {
            // 정방향 전이만 허용: 100->200->300->400
            String allowedNext = FORWARD_TRANSITION.get(current);
            if (allowedNext == null || !allowedNext.equals(target)) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
            }
        }

        int updated = nearMiss01Mapper.updateStatus(ChangeStatusCommand.from(param));
        if (updated == 0) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
        log.info("아차사고 상태전환 완료 - nearMissId={}, {} -> {}",
            param.nearMissId(), current, target);
    }

    @Override
    @Transactional
    public ReclassifyResponse reclassifyFromAssessment(ReclassifyParam param) {
        log.info("위험성평가 재분류 진입 - cmpnyCd={}, siteCd={}, srcProcessCd={}, srcAssessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.srcProcessCd(), param.srcAssessmentCd());

        // 사업장 권한 검증 (cross-site IDOR 차단) — srcAssessmentCd 도 동일 siteCd 스코프로 검증됨
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        if (!StringUtils.hasText(param.srcProcessCd()) || !StringUtils.hasText(param.srcAssessmentCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        AssessmentTransferCommand transferCommand = AssessmentTransferCommand.from(param);

        // 원 위험성평가 건 전환 가능 여부 확인(사업장 스코프 + 전환 허용 상태).
        //   전환 가능 건이 없으면, 단순 존재 여부로 404(미존재) vs 422(이미 처리/이관됨) 를 구분한다.
        if (nearMiss01Mapper.countAssessment(transferCommand) == 0) {
            if (nearMiss01Mapper.countAssessmentAny(transferCommand) > 0) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_002);
            }
            throw new ApiException(NearMissErrorCode.NEARMISS_404_002);
        }

        // 채번: NM + YYYYMMDD + 3자리 SEQ (사업장+당일 기준)
        String nearMissId = nearMiss01Mapper.selectNextNearMissId(NearMissIdSeqQuery.from(param));

        // (1) tb_near_miss INSERT (REPORT_STATUS_CD='100', SRC_* 기록).
        //   DESCRIPTION 은 NOT NULL — 원 평가건에서 넘어온 사건 경위가 비어있으면 출처 식별 기본 문구로 대체한다.
        InsertIncidentCommand insertCommand = InsertIncidentCommand.from(param, nearMissId);
        if (!StringUtils.hasText(insertCommand.description())) {
            String fallbackDesc = String.format(
                "위험성평가 요청에서 이관된 사건 (공정 %s / 평가 %s)",
                param.srcProcessCd(), param.srcAssessmentCd());
            insertCommand = InsertIncidentCommand.withDescription(insertCommand, fallbackDesc);
        }
        nearMiss01Mapper.insertIncident(insertCommand);

        // (2) 원 tb_risk_assessment 상태 -> '005' 이관 (D2: 상태값 신설, 추적 보존)
        int transferred = nearMiss01Mapper.transferAssessmentStatus(transferCommand);
        if (transferred == 0) {
            // 동시성 등으로 갱신 실패 시 트랜잭션 롤백 유도
            throw new ApiException(NearMissErrorCode.NEARMISS_404_002);
        }

        log.info("위험성평가 재분류 완료 - 신규 nearMissId={}, src={}/{}",
            nearMissId, param.srcProcessCd(), param.srcAssessmentCd());

        return ReclassifyResponse.builder()
            .nearMissId(nearMissId)
            .build();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    /**
     * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단).
     *
     * <p>nearmiss 는 부서(node) 단위가 아니라 사업장 단위이므로 attd11 의 canManageNode 를
     * 그대로 재사용하지 않는다(canManageNode 는 비 master/hr 에 대해 nodeCd 필수 + 노드관리자만 통과 →
     * 사업장 단독 권한 보유자를 부당하게 차단). 대신 전사 권한(master/hr)은 예외로 전체 허용하고
     * (예외 판정 기준은 canManageNode 내부의 {@link AuthRoleUtils#isManager}와 동일하게 맞춤),
     * 그 외 사용자는 tb_user_site_auth(USE_YN='Y') 매핑 보유 여부로 사업장 접근을 판정한다.
     */
    private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
        // 전사 권한(master/hr): 모든 사업장 접근 허용 (canManageNode 와 동일한 전사권한 예외 기준)
        if (AuthRoleUtils.isManager(authCd)) {
            return;
        }
        // 사업장 미지정이면 사업장 단위 검증 불가 → 차단
        if (!StringUtils.hasText(siteCd)) {
            log.warn("아차사고 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(NearMissErrorCode.NEARMISS_403_001);
        }
        // 그 외: tb_user_site_auth 매핑 보유 시에만 허용
        if (nearMiss01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
            log.warn("아차사고 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            throw new ApiException(NearMissErrorCode.NEARMISS_403_001);
        }
    }

    private void ensureExists(String gvCmpnyCd, String siteCd, String nearMissId) {
        String status = nearMiss01Mapper.selectReportStatus(
            new IncidentInfoQuery(siteCd, nearMissId, gvCmpnyCd));
        if (status == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
    }
}
