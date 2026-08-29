package com.prafta.app.leave.leaveflow.result;

import java.math.BigDecimal;

/**
 * prafta-app-018-A: 연차종류 + 잔여 1행 (apply-meta 본체).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 *   {@code AppLeaveFlowMapper.selectApplicableLeaveTypes} 의 SELECT 절과 1:1 로 맞춘다.
 * <ul>
 *   <li>{@code systemYn} : 법정 시드 여부('Y'면 법정). allowedUnits/aprvRequired 출처 분기 기준.</li>
 *   <li>{@code typeAprvUseYn} : 비법정 결재여부(tb_leave_type_mgmt.APRV_USE_YN).</li>
 *   <li>{@code useUnitType} : 비법정 사용단위(SYS025, NULL 가능 → 서비스에서 '00' 폴백).</li>
 *   <li>{@code balanceDays} : 활성집합 SUM(GRANT_DAYS)-SUM(USED_DAYS). 부여 없으면 0.</li>
 * </ul>
 *
 * <p>{@code evidenceYn}/{@code evidenceGuideMsg} : 연차 신청 증빙 필수화(2026-08-29) — 신규 필드는
 *   위치매핑 규약에 따라 맨 끝에 추가한다(기존 5개 필드 순서 불변).</p>
 */
public record LeaveTypeMetaRow(
      String leaveCd
    , String leaveNm
    , String systemYn
    , String typeAprvUseYn
    , String useUnitType
    , BigDecimal balanceDays
    , String evidenceYn
    , String evidenceGuideMsg
) {
}
