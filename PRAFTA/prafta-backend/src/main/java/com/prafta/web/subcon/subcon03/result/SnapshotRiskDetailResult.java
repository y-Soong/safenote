package com.prafta.web.subcon.subcon03.result;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 위험성평가 스냅샷 상세행(수신 조회 + 릴레이 복사 공용 — PRAFTA-SUBCON-T7 §5-8).
 *
 * <p>MyBatis property 매핑(카멜 별칭). {@code improves} 는 DB 컬럼이 아니라 서비스가 자식 조회 후 그룹핑해 채운다.
 *    {@code rowSeq/assessorSeq} 는 릴레이 재채번용(프론트는 무시). {@code init/revalFileMgmtCd} 는 수신사 소유
 *    파일코드(릴레이 시 재복제 원본). USER_CD/하위 회사/원본 경로는 담기지 않는다.
 */
@Getter
@Setter
public class SnapshotRiskDetailResult {

    private Long detailId;
    private Integer rowSeq;
    private Integer assessorSeq;
    private String affilCmpnyNm;
    private String processNm;
    private String riskTypeNm;
    private String hazardNm;
    private String assessmentDesc;
    private String assessmentStatusNm;
    private String initAssessorNm;
    private Integer initLikelihood;
    private Integer initSeverity;
    private String initRiskLv;
    private String initDesc;
    private String initAssessDate;
    private String initFileMgmtCd;
    private String revalAssessorNm;
    private Integer revalLikelihood;
    private Integer revalSeverity;
    private String revalRiskLv;
    private String revalDesc;
    private String revalAssessDate;
    private String revalFileMgmtCd;

    /** 개선항목 자식행(서비스가 채움 — DB 매핑 아님). */
    private List<SnapshotRiskImproveResult> improves;
}
