package com.prafta.web.user.user09.result;

/**
 * 소정-09: 셀프가입 신청 1건(매퍼 원본 행).
 *
 * <p>★내부 운반체다 — 휴대폰 암호문({@code mblNoEnc})을 담고 있으므로 <b>그대로 응답하지 않는다.</b>
 * 서비스가 복호 후 마스킹하여 {@code SelfJoinListResponse.Row} 로 옮긴다(User_06 블랙리스트 패턴).
 *
 * <p>★record 컬럼 순서 = 매퍼 SELECT 순서와 1:1 (MyBatis 위치 매핑 — feedback_mybatis_record_column_order).
 */
public record SelfJoinRowResult(
        String userCd
        , String userId
        , String userNm
        , String siteCd
        , String siteNm
        , String nodeCd
        , String nodeNm
        , String mblNoEnc
        , String mblNoLast4
        , String accountStatus
        , String applyDtime
) {
}
