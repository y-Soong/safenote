package com.prafta.web.risk.riskimpr01.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.risk.RiskImprErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.risk.riskimpr01.application.command.ImprovementCompleteCommand;
import com.prafta.web.risk.riskimpr01.application.command.ImprovementItemCommand;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementCompleteParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemDeleteParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemListParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemSaveParam;
import com.prafta.web.risk.riskimpr01.application.query.ImprovementItemListQuery;
import com.prafta.web.risk.riskimpr01.dto.response.ImprovementItemListResponse;
import com.prafta.web.risk.riskimpr01.mapper.RiskImpr01Mapper;
import com.prafta.web.risk.riskimpr01.result.ImprovementItemResult;
import com.prafta.web.risk.riskimpr01.service.RiskImpr01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위험성평가 개선항목(지속평가대상 관리, prafta-058) 서비스.
 *
 * <p>지속개선대상(SYS011 '005') 위험성평가 건의 개선항목 N건을 등록/수정/삭제하고,
 * "개선완료" 액션으로 ASSESSMENT_STATUS 를 '003'(개선완료) 으로 전이한다. 전이 시
 * 개선항목 N건은 보존하고, 최종 개선 후 위험도를 tb_risk_assessment.REVAL_* 로 승격하여
 * 기존 리스트/보고서 호환을 유지한다.
 *
 * <p>모든 진입부에서 사업장 권한을 검증해 cross-site IDOR 을 차단한다(risklink01 패턴 복제).
 * 식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskImpr01ServiceImpl implements RiskImpr01Service {

    // 위험성평가 진행상태 SYS011
    private static final String STATUS_REVIEW_REQUESTED = "001"; // 검토요청
    private static final String STATUS_IMPROVE_PLANNED  = "002"; // 개선예정
    private static final String STATUS_CONTINUOUS       = "005"; // 지속개선대상

    // 파일 도메인 타입 SYS010: 002 위험성평가 (사진 채널 재사용)
    private static final String FILE_TYPE_RISK = "002";

    // 개선 후 위험도 매우낮음 상한(1~3). 개선완료 가드(D1)
    private static final int VERY_LOW_RISK_MAX = 3;

    private final RiskImpr01Mapper riskImpr01Mapper;
    private final FileService fileService;
    private final FileMapper fileMapper;

    @Override
    public ImprovementItemListResponse selectImprovementItems(ImprovementItemListParam param) {
        log.info("개선항목 목록 조회 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<ImprovementItemResult> improvementItemList =
            riskImpr01Mapper.selectImprovementItems(ImprovementItemListQuery.from(param));

        return ImprovementItemListResponse.builder()
            .improvementItemList(improvementItemList)
            .build();
    }

    @Override
    @Transactional
    public void saveImprovementItem(ImprovementItemSaveParam param, MultipartFile file) {
        log.info("개선항목 저장 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}, improvementSeq={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.improvementSeq());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        validateKeys(param.processCd(), param.assessmentCd());

        // 대상 위험성평가 존재 + 편집 가능(003/004 차단) 검증
        assertAssessmentEditable(param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // 사진 있으면 저장하여 FILE_MGMT_CD 획득 (없으면 null → 매퍼에서 보존)
        String fileMgmtCd = null;
        if (file != null && !file.isEmpty()) {
            fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(param.gvCmpnyCd(), FILE_TYPE_RISK));
            fileService.fileSave(FileInfoParam.from(
                param.gvCmpnyCd()
                , param.gvUserCd()
                , param.siteCd()
                , FILE_TYPE_RISK
                , fileMgmtCd
                , file
            ));
        }

        if (param.improvementSeq() == null) {
            // 신규: 평가키 내 MAX+1 채번 후 INSERT.
            // 동시 등록 시 같은 SEQ 가 채번되어 PK 충돌(DuplicateKey)이 날 수 있으므로,
            // 네임드락(신규 락키 도입) 대신 코드베이스 관례(AppNearMiss01 등 DuplicateKeyException 흡수 패턴)에 맞춰
            // 충돌 시 재채번 재시도한다(마이그 불필요·데이터 손상 없음·두 번째 저장 실패 제거).
            int nextSeq = insertImprovementItemWithRetry(param, fileMgmtCd);
            log.info("개선항목 신규 등록 완료 - assessmentCd={}, improvementSeq={}", param.assessmentCd(), nextSeq);
        } else {
            // 수정: 평가키 + SEQ + USE_YN='Y' UPDATE (0행 → 404)
            int affected = riskImpr01Mapper.updateImprovementItem(
                ImprovementItemCommand.from(param, param.improvementSeq(), fileMgmtCd));
            if (affected == 0) {
                throw new ApiException(RiskImprErrorCode.RISKIMPR_404_002);
            }
            log.info("개선항목 수정 완료 - assessmentCd={}, improvementSeq={}", param.assessmentCd(), param.improvementSeq());
        }
    }

    @Override
    @Transactional
    public void deleteImprovementItem(ImprovementItemDeleteParam param) {
        log.info("개선항목 삭제 진입 - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}, improvementSeq={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(), param.improvementSeq());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        validateKeys(param.processCd(), param.assessmentCd());
        if (param.improvementSeq() == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 대상 위험성평가 존재 + 편집 가능(003/004 차단) 검증
        assertAssessmentEditable(param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        int affected = riskImpr01Mapper.softDeleteImprovementItem(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd(),
            param.improvementSeq(), param.gvUserCd());
        if (affected == 0) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_404_002);
        }

        log.info("개선항목 삭제 완료 - assessmentCd={}, improvementSeq={}", param.assessmentCd(), param.improvementSeq());
    }

    @Override
    @Transactional
    public void completeImprovement(ImprovementCompleteParam param) {
        log.info("개선완료 진입(005→003) - cmpnyCd={}, siteCd={}, processCd={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        validateKeys(param.processCd(), param.assessmentCd());

        // 대상 위험성평가 존재 + 상태 005(지속개선대상) 검증
        String status = riskImpr01Mapper.selectAssessmentStatus(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        if (status == null) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_404_001);
        }
        if (!STATUS_CONTINUOUS.equals(status)) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_422_001);
        }

        // 개선 후 위험도를 서버에서 빈도×강도로 재계산(Low-1B). 클라 revalRiskLv 는 신뢰하지 않는다.
        // 점수 null/비정상이면 재계산 불가 → fail-closed(가드 차단).
        int recalcRiskScore = recalcRiskScore(param.revalLikelihoodScore(), param.revalSeverityScore());

        // 개선 후 위험도 매우낮음(1~3) 가드 (D1) — 재계산값 기준
        if (!isVeryLowRiskScore(recalcRiskScore)) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_422_002);
        }

        // 개선항목 1건 이상 존재 검증
        int activeCount = riskImpr01Mapper.countActiveImprovementItems(
            param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
        if (activeCount == 0) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_422_004);
        }

        // 003 전이 + REVAL_* 동기화 (WHERE 005). REVAL_RISK_LV 는 재계산값으로 저장(Low-1B).
        // 동시성으로 005 아니면 0행 → 422
        int affected = riskImpr01Mapper.updateAssessmentComplete(
            ImprovementCompleteCommand.from(param, String.valueOf(recalcRiskScore)));
        if (affected == 0) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_422_001);
        }

        log.info("개선완료(003 전이) 완료 - assessmentCd={}, 보존 개선항목 {}건", param.assessmentCd(), activeCount);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    // 개선항목 채번 재시도 횟수(Low-4). 동시 등록 충돌은 드물어 소수 재시도로 충분하다.
    private static final int INSERT_SEQ_MAX_RETRY = 3;

    /**
     * MAX+1 채번 후 INSERT. PK 충돌(DuplicateKey) 시 재채번하여 재시도한다(Low-4 동시성 방어).
     * @return 최종 적재된 IMPROVEMENT_SEQ
     */
    private int insertImprovementItemWithRetry(ImprovementItemSaveParam param, String fileMgmtCd) {
        DuplicateKeyException lastDup = null;
        for (int attempt = 1; attempt <= INSERT_SEQ_MAX_RETRY; attempt++) {
            int nextSeq = riskImpr01Mapper.selectNextImprovementSeq(
                param.gvCmpnyCd(), param.siteCd(), param.processCd(), param.assessmentCd());
            try {
                riskImpr01Mapper.insertImprovementItem(
                    ImprovementItemCommand.from(param, nextSeq, fileMgmtCd));
                return nextSeq;
            } catch (DuplicateKeyException dup) {
                // 동시 등록으로 같은 SEQ 가 선점됨 → 재채번 재시도
                lastDup = dup;
                log.warn("개선항목 채번 충돌, 재시도 - assessmentCd={}, seq={}, attempt={}/{}",
                    param.assessmentCd(), nextSeq, attempt, INSERT_SEQ_MAX_RETRY);
            }
        }
        // 재시도 한도 초과: 동시성 폭주로 보고 충돌을 전파(GlobalExceptionHandler 가 처리)
        throw lastDup;
    }

    /**
     * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단).
     * 전사 권한(master/hr)은 전체 허용, 그 외는 tb_user_site_auth(USE_YN='Y') 매핑 보유 시에만 허용.
     */
    private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
        if (AuthRoleUtils.isManager(authCd)) {
            return;
        }
        if (!StringUtils.hasText(siteCd)) {
            log.warn("개선항목 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(RiskImprErrorCode.RISKIMPR_403_001);
        }
        if (riskImpr01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
            log.warn("개선항목 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            throw new ApiException(RiskImprErrorCode.RISKIMPR_403_001);
        }
    }

    /** 평가키 필수값 검증. */
    private void validateKeys(String processCd, String assessmentCd) {
        if (!StringUtils.hasText(processCd) || !StringUtils.hasText(assessmentCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /**
     * 대상 위험성평가 존재 + 개선항목 편집 가능 상태 검증.
     * <p>※ SYS011 005(지속개선대상)는 문자열상 "003" 보다 크므로 risklink01 의 compareTo("003")>=0
     *   차단을 그대로 쓰면 005 도 편집 차단된다(P2.md §주의). 따라서 화이트리스트({001,002,005})로
     *   허용하고 개선완료(003)/미처리대상(004)만 편집 차단한다.
     */
    private void assertAssessmentEditable(String cmpnyCd, String siteCd, String processCd, String assessmentCd) {
        String status = riskImpr01Mapper.selectAssessmentStatus(cmpnyCd, siteCd, processCd, assessmentCd);
        if (status == null) {
            throw new ApiException(RiskImprErrorCode.RISKIMPR_404_001);
        }
        boolean editable = STATUS_REVIEW_REQUESTED.equals(status)
            || STATUS_IMPROVE_PLANNED.equals(status)
            || STATUS_CONTINUOUS.equals(status);
        if (!editable) {
            // 003(개선완료) / 004(미처리대상) 등은 모두 동일 사유로 편집 불가 (Low-5: 분기 단일화)
            throw new ApiException(RiskImprErrorCode.RISKIMPR_422_003);
        }
    }

    /**
     * 개선 후 위험도를 빈도×강도로 서버 재계산(Low-1B).
     * <p>점수가 null 이거나 허용범위(빈도 1~5, 강도 1~4)를 벗어나면 위험도-점수 모순 저장을 막기 위해
     * -1 을 반환하여 호출부 가드에서 fail-closed(차단) 처리되게 한다. 클라 revalRiskLv 는 사용하지 않는다.
     */
    private int recalcRiskScore(Integer likelihoodScore, Integer severityScore) {
        if (likelihoodScore == null || severityScore == null) {
            return -1;
        }
        if (likelihoodScore < 1 || likelihoodScore > 5 || severityScore < 1 || severityScore > 4) {
            return -1;
        }
        return likelihoodScore * severityScore;
    }

    /** 재계산된 위험도 점수가 매우낮음(1~3) 인지 판정. -1(재계산 불가) 은 차단. */
    private boolean isVeryLowRiskScore(int riskScore) {
        return riskScore >= 1 && riskScore <= VERY_LOW_RISK_MAX;
    }
}
