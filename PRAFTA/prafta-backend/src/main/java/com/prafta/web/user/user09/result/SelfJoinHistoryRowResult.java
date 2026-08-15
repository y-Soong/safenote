package com.prafta.web.user.user09.result;

/**
 * 소정-09: 셀프가입 처리 이력 1건(매퍼 원본 행).
 *
 * <p>★내부 운반체다 — 휴대폰 암호문({@code mblNoEnc})을 담고 있으므로 <b>그대로 응답하지 않는다.</b>
 * 서비스가 복호 후 마스킹하여 {@code SelfJoinHistoryListResponse.Row} 로 옮긴다
 * ({@link SelfJoinRowResult} 와 동일 규약).
 *
 * <p>★record 필드 순서 = 매퍼 SELECT 컬럼 순서와 1:1 (MyBatis 위치 매핑 —
 * feedback_mybatis_record_column_order). 순서가 어긋나면 값이 밀려 담겨도 예외가 나지 않는다.
 */
public record SelfJoinHistoryRowResult(
        String auditId
        , String processDtime
        , String actionType
        , String userCd
        , String userId
        , String userNm
        , String siteNm
        , String nodeNm
        , String mblNoEnc
        , String mblNoLast4
        , String applyDtime
        , String hireDate
        , String rankNm
        , String processorNm
        , String rejectReason
) {
}
