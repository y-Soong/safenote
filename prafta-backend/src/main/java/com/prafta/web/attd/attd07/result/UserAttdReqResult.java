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
) {
}
