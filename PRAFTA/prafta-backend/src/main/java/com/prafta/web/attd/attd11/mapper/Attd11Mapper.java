package com.prafta.web.attd.attd11.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd11.application.query.MonthlyAttdSummaryQuery;
import com.prafta.web.attd.attd11.result.AbsentDayRowResult;
import com.prafta.web.attd.attd11.result.AttdSummaryRowResult;
import com.prafta.web.attd.attd11.result.HalfLeaveWindowResult;
import com.prafta.web.attd.attd11.result.OvertimeSummaryResult;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 매퍼.
 *
 * 지각/조퇴/근무시간의 일시(YYYYMMDDHHmm) 기반 자정 넘김 보정 판정은
 * SQL 로는 복잡하므로(attd07/Attd_08 가 화면단 재판정을 택한 것과 동일 이유)
 * 원시 행(selectAttdSummaryRows)을 조회한 뒤 service 에서 판정/집계한다.
 * 초과근무 분 합은 단순 SUM 이라 SQL 에서 USER_CD 단위로 집계해 내려준다.
 */
@Mapper
public interface Attd11Mapper {

    /**
     * (USER_CD, WORK_YMD, WORK_SEQ) 단위 출퇴근 실적 + 해당 차수 유효 스케줄(계획시각/휴게) 원시 행.
     * node_tree(하위부서 RECURSIVE) + target_user(USE_YN='Y', WITHDRAWAL_DATE IS NULL, USER_NM LIKE)
     * 스코프. effective-dating 스케줄 조인은 attd07 selectMonthlyAttdList 패턴 재사용.
     */
    List<AttdSummaryRowResult> selectAttdSummaryRows(MonthlyAttdSummaryQuery query);

    /**
     * 사용자별 COMPLETED 초과근무 분 합(해당 월). OT_STATUS='COMPLETED' AND DEL_YN='N'.
     */
    List<OvertimeSummaryResult> selectOvertimeSummary(MonthlyAttdSummaryQuery query);

    /**
     * COM-016-F (8-3) / HB-06 - 결근 계상용 일자별 원시 행.
     * 모수 = TB_USER_WORK_PLAN 의 해당 월 스케줄 근무일(WORK_PLAN_CD 가 SCH_CD).
     * 제외 = 종일 연차일(TB_USER_LEAVE_USE '00' 확정) / 휴일(TB_HOLIDAY) / 출근기록 존재일 / 미래일.
     * 스코프는 selectAttdSummaryRows 와 동일(node_tree + target_user).
     *
     * <p>HB-06: 종전 {@code selectAbsentDayCount}(사용자별 COUNT)를 일자별 행 반환으로 바꿨다.
     * 결근을 분으로 계상하려면 그날 소정근로분 D 와 부분연차 면제분이 필요하고,
     * D 는 {@code ScheduleWorkMinutesUtils} 단일 출처로 계산해야 하기 때문이다.
     */
    List<AbsentDayRowResult> selectAbsentDayRows(MonthlyAttdSummaryQuery query);

    /**
     * HB-05(D1)/D-3 - 조회 월의 확정 반차('01' + 시각 보유) 면제 구간 목록(한 날 다건 가능).
     * 지각·조퇴 판정용 유효 소정 구간은 서비스가 {@code PartialLeaveWindowUtils.resolveAll} 로 산출한다.
     */
    List<HalfLeaveWindowResult> selectHalfLeaveWindows(MonthlyAttdSummaryQuery query);
}
