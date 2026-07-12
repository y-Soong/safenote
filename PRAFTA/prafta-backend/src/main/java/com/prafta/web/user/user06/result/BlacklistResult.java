package com.prafta.web.user.user06.result;

/**
 * 블랙리스트 목록 1행 결과(프론트 응답).
 *
 * <p>휴대폰(mblNo)은 서버 마스킹 문자열(예 010-****-1234)이다. 평문/암호문은 노출하지 않는다.
 */
public record BlacklistResult(
    String blacklistId
    , String mblNo
    , String reason
    , String useYn
    , String insertNm
    , String insertDate
){
}
