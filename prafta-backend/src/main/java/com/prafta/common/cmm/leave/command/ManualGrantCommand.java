package com.prafta.common.cmm.leave.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * 연차 수동 부여 입력 객체(단일/일괄 공통, attd09).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.8 (멱등성/부여 레코드 규칙)
 *
 * <p>본 record는 attd09 모듈의 Param에서 변환되어 {@code LeaveDashboardService}로 전달된다.
 * 회사 코드/권한/수행자는 별도 인자로 전달하므로 본 record에는 포함하지 않는다.
 *
 * <p>입력 검증(일수 0.5 단위/양수, 날짜 8자리, leaveCd 화이트리스트)은 서비스 계층에서
 * 서버 권위로 재수행한다(프론트 1차 검증은 게이트일 뿐).
 *
 * @param userCds        대상 사용자 코드 목록 (단일=1건, 일괄=N건). 직원당 1건 INSERT.
 * @param leaveCd        연차 코드 (수동 부여 가능 휴가 종류 화이트리스트 내에서만 허용)
 * @param grantDays      부여 일수 (양수, 0.5일 단위)
 * @param availFromDate  사용 가능 시작일 (YYYYMMDD)
 * @param reason         부여 사유 (GRANT_REASON, NULL 허용)
 */
public record ManualGrantCommand(
      List<String> userCds
    , String leaveCd
    , BigDecimal grantDays
    , String availFromDate
    , String reason
) {
}
