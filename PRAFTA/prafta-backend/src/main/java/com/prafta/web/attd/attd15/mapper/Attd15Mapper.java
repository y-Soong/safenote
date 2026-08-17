package com.prafta.web.attd.attd15.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd15.application.query.Weekly52hListsQuery;
import com.prafta.web.attd.attd15.result.ActualRowResult;
import com.prafta.web.attd.attd15.result.OvertimeSummaryResult;
import com.prafta.web.attd.attd15.result.ScheduledRowResult;
import com.prafta.web.attd.attd15.result.TargetUserResult;
import com.prafta.web.attd.attd15.result.TimeLeaveWindowResult;

/**
 * ATTD15-T1 - 주52시간 관리 매퍼.
 *
 * <p>node_tree(하위부서 RECURSIVE) + target_user(USE_YN='Y', WITHDRAWAL_DATE IS NULL,
 * EMPLOYMENT_TYPE&lt;&gt;DAILY, USER_NM LIKE) 스코프는 {@code Attd11Mapper} 패턴을 그대로 이식하되
 * 일용직 제외 조건을 추가했다(사용자 결정 §2.3).
 *
 * <p>선체크 결과: 지각/조퇴 등 일시 기반 판정은 본 화면에 없으므로 스케줄/실적 원시 행을 조회한
 * 뒤 service 에서 분(minutes) 합산만 수행한다(Attd11 대비 판정 로직 없음, 집계만).
 */
@Mapper
public interface Attd15Mapper {

    /**
     * 필터(사업장/소속부서(+하위)/사용자명/일용직 제외)에 매칭되는 대상 사용자 전원(모수).
     * 스케줄·실적이 모두 0인 사용자도 노출해야 하므로(사용자 결정 §2.4) outer-merge 기준 목록으로 쓴다.
     */
    List<TargetUserResult> selectTargetUsers(Weekly52hListsQuery query);

    /**
     * "등록된 스케줄 기준" — 해당 주(월~일) 근무일(WORK_PLAN_CD 가 스케줄코드인 날)의
     * 사용자·근무일별 원시 스케줄 계획시각/휴게(1구간/2구간). effective-dating 스케줄 조인은
     * Attd11Mapper.selectAttdSummaryRows 의 SCH 서브쿼리 그대로 재사용.
     */
    List<ScheduledRowResult> selectWeeklyScheduledRows(Weekly52hListsQuery query);

    /**
     * "실제 근무 기준" — (USER_CD, WORK_YMD, WORK_SEQ) 단위 출퇴근 실적 원시 행.
     * Attd11Mapper.selectAttdSummaryRows 를 주간 범위(WORK_YMD BETWEEN)로 이식(사용자 결정 §2.1 채택안).
     */
    List<ActualRowResult> selectWeeklyActualRows(Weekly52hListsQuery query);

    /**
     * 사용자별 COMPLETED 초과근무 분 합(해당 주). OT_STATUS='COMPLETED' AND DEL_YN='N'.
     * Attd11Mapper.selectOvertimeSummary 를 주간 범위로 이식.
     */
    List<OvertimeSummaryResult> selectWeeklyOvertimeSummary(Weekly52hListsQuery query);

    /**
     * A안(2026-08-17): 조회 주간 안의 확정 "시각 보유" 연차(반차 01 + 시간차 02/03/04) 구간 목록.
     * 실제기준(raw) 합산에서 실근태와의 겹침을 차감하는 데 쓴다(연차 시간은 근로시간 미산입).
     */
    List<TimeLeaveWindowResult> selectWeeklyTimeLeaveWindows(Weekly52hListsQuery query);
}
