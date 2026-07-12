package com.prafta.web.risk.risklink01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.risk.RiskLinkErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.risk.risklink01.application.command.NearMissLinkCommand;
import com.prafta.web.risk.risklink01.application.param.AvailableNearMissParam;
import com.prafta.web.risk.risklink01.application.param.LinkedNearMissParam;
import com.prafta.web.risk.risklink01.application.param.NearMissLinkParam;
import com.prafta.web.risk.risklink01.application.query.AvailableNearMissQuery;
import com.prafta.web.risk.risklink01.application.query.LinkedNearMissQuery;
import com.prafta.web.risk.risklink01.dto.response.NearMissLinkListResponse;
import com.prafta.web.risk.risklink01.mapper.RiskLink01Mapper;
import com.prafta.web.risk.risklink01.result.LinkNearMissResult;
import com.prafta.web.risk.risklink01.service.RiskLink01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가-아차사고 참조 연계(prafta-054-3) 서비스.
 *
 * <p>위험성평가에서 같은 사업장의 완료(SYS063='300') 아차사고를 검색·연결·해제·조회한다.
 * 아차사고 데이터를 평가로 복사하지 않는 순수 "참조" 연계이며, 모든 진입부에서 사업장 권한을
 * 검증해 cross-site IDOR 을 차단한다(nearmiss01 의 assertSiteAccess 패턴 복제).
 * 편집(연결/해제)은 위험성평가 개선완료(ASSESSMENT_STATUS='003') 전에만 허용한다(사용자 확정 §3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskLink01ServiceImpl implements RiskLink01Service {

    // 위험성평가 진행상태 SYS011: 개선완료(003) 이상은 참조 아차사고 편집 불가
    private static final String ASSESSMENT_STATUS_DONE = "003";
    // 아차사고 처리상태 SYS063: 완료(300) 건만 참조로 연결 가능 (T6-14B-5/D4: 완료 400→300 재번호)
    private static final String NEAR_MISS_STATUS_COMPLETED = "300";

    private final RiskLink01Mapper riskLink01Mapper;

    @Override
    public NearMissLinkListResponse selectAvailableNearMiss(AvailableNearMissParam param) {
        log.info("연결 후보 아차사고 검색 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<LinkNearMissResult> nearMissList =
            riskLink01Mapper.selectAvailableNearMiss(AvailableNearMissQuery.from(param));

        return NearMissLinkListResponse.builder()
            .nearMissList(nearMissList)
            .build();
    }

    @Override
    public NearMissLinkListResponse selectLinkedNearMiss(LinkedNearMissParam param) {
        log.info("연결된 아차사고 목록 조회 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<LinkNearMissResult> nearMissList =
            riskLink01Mapper.selectLinkedNearMiss(LinkedNearMissQuery.from(param));

        return NearMissLinkListResponse.builder()
            .nearMissList(nearMissList)
            .build();
    }

    @Override
    @Transactional
    public void linkNearMiss(NearMissLinkParam param) {
        log.info("참조 아차사고 연결 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.nearMissId());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        validateKeys(param);

        // (1) 대상 위험성평가 건 존재 + 개선완료(003) 전 확인
        assertAssessmentEditable(param);

        // (2) 대상 아차사고 존재 + 같은 사업장 + 완료(400) 확인
        String nmStatus = riskLink01Mapper.selectNearMissReportStatus(
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId());
        if (nmStatus == null) {
            throw new ApiException(RiskLinkErrorCode.RISKLINK_404_002);
        }
        if (!NEAR_MISS_STATUS_COMPLETED.equals(nmStatus)) {
            throw new ApiException(RiskLinkErrorCode.RISKLINK_422_002);
        }

        // (3) upsert: 기존 행(USE_YN 무관) 존재 시 USE_YN='Y' 복원, 없으면 INSERT
        NearMissLinkCommand command = NearMissLinkCommand.from(param);
        if (riskLink01Mapper.countLink(command) > 0) {
            riskLink01Mapper.restoreLink(command);
        } else {
            riskLink01Mapper.insertLink(command);
        }

        log.info("참조 아차사고 연결 완료 - assessmentCd={}, nearMissId={}",
            param.assessmentCd(), param.nearMissId());
    }

    @Override
    @Transactional
    public void unlinkNearMiss(NearMissLinkParam param) {
        log.info("참조 아차사고 해제 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.nearMissId());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        validateKeys(param);

        // 대상 위험성평가 건 존재 + 개선완료(003) 전 확인 (003 이상은 편집 불가)
        assertAssessmentEditable(param);

        // soft delete (USE_YN='N'). 대상 행이 없어도 멱등 처리(이미 해제/미연결).
        riskLink01Mapper.updateLinkUseYn(NearMissLinkCommand.from(param));

        log.info("참조 아차사고 해제 완료 - assessmentCd={}, nearMissId={}",
            param.assessmentCd(), param.nearMissId());
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    /**
     * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단).
     * nearmiss01 의 동명 헬퍼와 동일 정책: 전사 권한(master/hr)은 전체 허용, 그 외는
     * tb_user_site_auth(USE_YN='Y') 매핑 보유 시에만 허용한다.
     */
    private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
        if (AuthRoleUtils.isManager(authCd)) {
            return;
        }
        if (!StringUtils.hasText(siteCd)) {
            log.warn("참조연계 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(RiskLinkErrorCode.RISKLINK_403_001);
        }
        if (riskLink01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
            log.warn("참조연계 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            throw new ApiException(RiskLinkErrorCode.RISKLINK_403_001);
        }
    }

    /** 연결/해제 키 필수값 검증. */
    private void validateKeys(NearMissLinkParam param) {
        if (!StringUtils.hasText(param.processCd())
                || !StringUtils.hasText(param.assessmentCd())
                || !StringUtils.hasText(param.nearMissId())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /** 대상 위험성평가 건 존재 + 개선완료(003) 전 확인. 없으면 404, 003 이상이면 422. */
    private void assertAssessmentEditable(NearMissLinkParam param) {
        String assessmentStatus = riskLink01Mapper.selectAssessmentStatus(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        if (assessmentStatus == null) {
            throw new ApiException(RiskLinkErrorCode.RISKLINK_404_001);
        }
        // 개선완료(003) 이상은 편집 불가. SYS011 코드는 문자열 비교(003 이상 = 003, 004)
        if (assessmentStatus.compareTo(ASSESSMENT_STATUS_DONE) >= 0) {
            throw new ApiException(RiskLinkErrorCode.RISKLINK_422_001);
        }
    }
}
