package com.prafta.web.subcon.subcon03.application.command;

/**
 * 아차사고 스냅샷 상세행 INSERT 커맨드 1건(PRAFTA-SUBCON-T7 §5-4, 배치).
 *
 * <p>PII 최소수집: 인적 정보는 제보자 성명(평문) + 소속표시(회사명)뿐. 원본 USER_CD/리뷰어/사번은 미저장.
 *    fileMgmtCd 는 복제된 수신사 소유 파일코드. occurDtime 은 'YYYY-MM-DD HH:MM:SS' 문자열(datetime 컬럼 저장).
 */
public record SnapshotNearmissInsertCommand(
    Long snapshotId
    , int rowSeq
    , String affilCmpnyNm
    , int reporterSeq
    , String reporterNm
    , String occurDtime
    , String processNm
    , String locationDesc
    , String description
    , String potentialSeverityNm
    , String immediateActionDesc
    , String adminTempActionDesc
    , String causeDesc
    , String preventionDesc
    , String reportStatusNm
    , String fileMgmtCd
    , String insertNo
){
}
