package com.prafta.web.subcon.subcon03.application.command;

import lombok.Getter;

/**
 * 위험성평가 스냅샷 상세행 INSERT 커맨드 1건(PRAFTA-SUBCON-T7 §5-4).
 *
 * <p>개선항목 자식행이 부모 DETAIL_ID 를 참조하므로 단건 INSERT + useGeneratedKeys 로 DETAIL_ID 를 회수한다.
 *    PII 최소수집: 인적 정보는 작성자 성명(평문) + 소속표시(회사명)뿐. 원본 USER_CD/사번/부서/연락처는 미저장.
 */
@Getter
public class SnapshotRiskInsertCommand {

    /** 생성된 상세행ID(useGeneratedKeys 회수). */
    private Long detailId;

    private final Long snapshotId;
    private final int rowSeq;
    private final String affilCmpnyNm;
    private final int assessorSeq;
    private final String processNm;
    private final String riskTypeNm;
    private final String hazardNm;
    private final String assessmentDesc;
    private final String assessmentStatusNm;
    private final String initAssessorNm;
    private final Integer initLikelihood;
    private final Integer initSeverity;
    private final String initRiskLv;
    private final String initDesc;
    private final String initAssessDate;
    private final String initFileMgmtCd;
    private final String revalAssessorNm;
    private final Integer revalLikelihood;
    private final Integer revalSeverity;
    private final String revalRiskLv;
    private final String revalDesc;
    private final String revalAssessDate;
    private final String revalFileMgmtCd;
    private final String insertNo;

    public SnapshotRiskInsertCommand(Long snapshotId, int rowSeq, String affilCmpnyNm, int assessorSeq,
            String processNm, String riskTypeNm, String hazardNm, String assessmentDesc, String assessmentStatusNm,
            String initAssessorNm, Integer initLikelihood, Integer initSeverity, String initRiskLv, String initDesc,
            String initAssessDate, String initFileMgmtCd, String revalAssessorNm, Integer revalLikelihood,
            Integer revalSeverity, String revalRiskLv, String revalDesc, String revalAssessDate,
            String revalFileMgmtCd, String insertNo) {
        this.snapshotId = snapshotId;
        this.rowSeq = rowSeq;
        this.affilCmpnyNm = affilCmpnyNm;
        this.assessorSeq = assessorSeq;
        this.processNm = processNm;
        this.riskTypeNm = riskTypeNm;
        this.hazardNm = hazardNm;
        this.assessmentDesc = assessmentDesc;
        this.assessmentStatusNm = assessmentStatusNm;
        this.initAssessorNm = initAssessorNm;
        this.initLikelihood = initLikelihood;
        this.initSeverity = initSeverity;
        this.initRiskLv = initRiskLv;
        this.initDesc = initDesc;
        this.initAssessDate = initAssessDate;
        this.initFileMgmtCd = initFileMgmtCd;
        this.revalAssessorNm = revalAssessorNm;
        this.revalLikelihood = revalLikelihood;
        this.revalSeverity = revalSeverity;
        this.revalRiskLv = revalRiskLv;
        this.revalDesc = revalDesc;
        this.revalAssessDate = revalAssessDate;
        this.revalFileMgmtCd = revalFileMgmtCd;
        this.insertNo = insertNo;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }
}
