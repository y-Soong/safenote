package com.prafta.web.dashboard.dashboard01.service;

import com.prafta.web.dashboard.dashboard01.application.param.DashAttdPlanRegRateParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashAttdStatusRateParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashSafetyAcctParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashSafetyParam;
import com.prafta.web.dashboard.dashboard01.application.param.LeaveUsageParam;
import com.prafta.web.dashboard.dashboard01.application.param.OvertimeTrendParam;
import com.prafta.web.dashboard.dashboard01.dto.response.DashAttdPlanRegRateResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.DashAttdStatusRateResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.DashSafetyAcctResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.LeaveUsageResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.OvertimeTrendResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.SafetyPatrolResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.SafetyRiskResponse;
import com.prafta.web.dashboard.dashboard01.dto.response.SafetyTbmTrendResponse;

/**
 * 웹 관리자 대시보드 서비스 인터페이스 (PRAFTA-DASHBOARD-T1 골격 / T4·T5 안전 탭 추가).
 */
public interface Dashboard01Service {

    // T4: 안전 탭 무사고 배너(S1) + 사고 summary(S5) 조회
    DashSafetyAcctResponse selectSafetyAcct(DashSafetyAcctParam param);

    // T5: 안전 탭 순회점검(S2) — 당일 x/y + 조회월 미이행 수
    SafetyPatrolResponse selectSafetyPatrol(DashSafetyParam param);

    // T5: 안전 탭 위험성평가(S3) — 검토요청/개선예정 카운트 + 조회월 아차사고 건수
    SafetyRiskResponse selectSafetyRisk(DashSafetyParam param);

    // T5: 안전 탭 TBM(S4) — 조회월 포함 과거 12개월 완료 세션 건수 추이
    SafetyTbmTrendResponse selectSafetyTbmTrend(DashSafetyParam param);

    // T2: 근태 탭 A1 근무계획 등록율 조회
    DashAttdPlanRegRateResponse selectAttdPlanRegRate(DashAttdPlanRegRateParam param);

    // T2: 근태 탭 A2 정상/비정상 근무율 조회
    DashAttdStatusRateResponse selectAttdStatusRate(DashAttdStatusRateParam param);

    // T3: 근태 탭 A3 초과근무 6개월 추이 조회 (baseYm 포함 과거 6개월, 누락 월 0채움)
    OvertimeTrendResponse selectOvertimeTrend(OvertimeTrendParam param);

    // T3: 근태 탭 A4 법정연차 사용/사용예정/미사용 3분할 조회 (현재 시점 스냅샷)
    LeaveUsageResponse selectLeaveUsage(LeaveUsageParam param);
}
