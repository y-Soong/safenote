package com.prafta.web.subcon.subcon03.result;

import lombok.Getter;
import lombok.Setter;

/**
 * 아차사고 스냅샷 상세행(수신 조회 + 릴레이 복사 공용 — PRAFTA-SUBCON-T7 §5-8).
 *
 * <p>MyBatis property 매핑(카멜 별칭). {@code rowSeq/reporterSeq} 는 릴레이 재채번용(프론트는 무시).
 *    {@code fileMgmtCd} 는 수신사 소유 파일코드(릴레이 시 재복제 원본). {@code occurDtime} 은 표시 포맷 문자열.
 *    USER_CD/하위 회사/원본 경로는 담기지 않는다.
 */
@Getter
@Setter
public class SnapshotNearmissDetailResult {

    private Long detailId;
    private Integer rowSeq;
    private Integer reporterSeq;
    private String affilCmpnyNm;
    private String reporterNm;
    private String occurDtime;
    private String processNm;
    private String locationDesc;
    private String description;
    private String potentialSeverityNm;
    private String immediateActionDesc;
    private String adminTempActionDesc;
    private String causeDesc;
    private String preventionDesc;
    private String reportStatusNm;
    private String fileMgmtCd;
}
