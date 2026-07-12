package com.prafta.web.user.user01.application.command;

/**
 * 소속이동 예약 INSERT 커맨드 — PRAFTA-WEB_001-1.
 *
 * <p>채번된 reservationId, 등록시점 스냅샷(FROM_SITE/NODE, EMPLOYMENT_TYPE), 이동 정보, 등록자(insertNo)를 담는다.
 * STATUS='RESERVED' / NOTICE_ACK_YN='N' / DEL_YN='N' 은 XML 에서 고정한다.
 */
public record TransferReservationInsertCommand(
    String cmpnyCd
    , String reservationId
    , String userCd
    , String fromSiteCd
    , String fromNodeCd
    , String toSiteCd
    , String toNodeCd
    , String toDefaultSchCd
    , String moveDate
    , String moveReason
    , String employmentType
    , String insertNo
) {}
