package com.prafta.web.baim.baim05.result;

/**
 * PRAFTA-055-3 — 슬롯 사용 이력 1행 결과.
 * PII(이름/휴대폰)는 SQL 에서 마스킹된 값만 담는다(평문 절대 금지).
 * issueChannelNm/releaseTypeNm 은 FNC_CMM_INFO_SRCH 로 코드→라벨 변환된 값.
 */
public record SlotHisResult(
    String hisId
    , String issueChannelNm
    , String occupyDtime
    , String releaseDtime
    , String userNmMasked
    , String mblNoMasked
    , String releaseUser
    , String releaseTypeNm
    , String releaseReason
){
}
