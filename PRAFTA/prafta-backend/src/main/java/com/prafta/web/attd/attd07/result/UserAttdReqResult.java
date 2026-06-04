package com.prafta.web.attd.attd07.result;

/**
 * Result of selectUserAttdReqByReqId - authoritative REQ row used for
 * server-side authorization and field-mismatch validation.
 *
 * PRAFTA-003: column ATTD_ID was renamed to TARGET_ID because the table now
 * stores requests for multiple target types (attendance / overtime / leave).
 * The semantic of {@link #targetId()} therefore depends on REQ_TYPE.
 *
 * SEC-018: {@link #reqType()} is carried so the service layer can enforce
 * that the attendance-modification endpoint never processes an OT or LEAVE
 * request that happens to share the REQ_ID space.
 */
public record UserAttdReqResult(
      String reqId
    , String targetId
    , String userCd
    , String siteCd
    , String nodeCd
    , String workYmd
    , String workSeq
    , String reqType
    , String reqStatus
    , String delYn

    /**
     * 목표 스케줄 코드 (PRAFTA-APP-007).
     *
     * REQ_TYPE='10'(스케줄 수정 요청)일 때만 의미를 가지며, 승인 시 tb_user_work_plan 의
     * WORK_PLAN_CD 로 upsert 되는 서버 권위 값이다. 클라이언트가 보낸 SCH_CD 는 신뢰하지
     * 않고 이 REQ row 의 값을 사용한다(IDOR/변조 차단). 다른 REQ_TYPE 에서는 NULL.
     */
    , String schCd
) {
}
