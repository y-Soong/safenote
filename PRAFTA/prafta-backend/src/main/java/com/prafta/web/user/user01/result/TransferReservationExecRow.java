package com.prafta.web.user.user01.result;

/**
 * 발효(실행) 대상 소속이동 예약 1건 — PRAFTA-WEB_001-2(Terminal B).
 *
 * <p>자정 스케줄러가 발효일 도래(MOVE_DATE &lt;= 오늘) RESERVED 예약을 조회해 담는다.
 * 모든 발효 변경은 본 행의 {@code cmpnyCd}/{@code userCd} 범위로만 수행한다(cross-tenant 방지).
 *
 * <ul>
 *   <li>{@code insertNo} = 등록자(master/hr) — 발효 변경의 감사 컬럼(UPDATE_NO 등) actor 로 사용한다.</li>
 *   <li>{@code toDefaultSchCd} = 정규직만 보유(일용직 null) — 기본근무 발효 분기에 사용.</li>
 * </ul>
 */
public record TransferReservationExecRow(
    String cmpnyCd
    , String reservationId
    , String userCd
    , String fromSiteCd
    , String fromNodeCd
    , String toSiteCd
    , String toNodeCd
    , String toDefaultSchCd
    , String moveDate
    , String employmentType
    , String insertNo
) {}
