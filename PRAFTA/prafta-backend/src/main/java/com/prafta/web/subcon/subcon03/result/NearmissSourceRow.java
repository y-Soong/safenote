package com.prafta.web.subcon.subcon03.result;

/**
 * 아차사고 스냅샷 생성 원천행 1건(제공측 tb_near_miss, USE_YN='Y' — PRAFTA-SUBCON-T7 §5-4).
 *
 * <p>MyBatis record 위치매핑: SELECT 컬럼 순서 = 컴포넌트 순서.
 *
 * <p>{@code reporterId} 는 동의 필터 + REPORTER_SEQ 로컬 채번 전용(미저장). {@code nearMissId} 는 정렬 안정용
 *    서버 내부값(미저장). {@code fileMgmtCd} 는 원본 파일코드 — 복제 후 신규 코드로 치환 저장. 성명(reporterNm)은
 *    원천 평문(FNC_CMM_INFO_SRCH USER_NM) 그대로 복사. {@code occurDtime} 은 'YYYY-MM-DD HH:MM:SS' 포맷 문자열.
 */
public record NearmissSourceRow(
    String reporterId
    , String nearMissId
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
){
}
