package com.prafta.app.attd.admin.service;

import com.prafta.app.attd.admin.application.param.AdminDailyAttdParam;
import com.prafta.app.attd.admin.application.param.AdminMonthlyAttdParam;
import com.prafta.app.attd.admin.dto.response.DailyAttdResponse;
import com.prafta.app.attd.admin.dto.response.MonthlyAttdResponse;

/**
 * J1-5: 앱 관리자 근태 상세 서비스(조회 전용 — 일자/월별).
 *
 * <p>권한([권한매트릭스 §3]): ATTD_DETAIL = master ∥ hr ∥ nodeAdmin (safe 단독 ⛔ — 승인관리와 동일 축).
 * <p>스코프([§4]): master=전사(회사 내 siteCd) / hr=사업장 / 노드관리자=자기노드+자손.
 * nodeCd 는 리소스 키이며 서버가 토큰 스코프 내인지 재검증한다(IDOR — 스코프 밖이면 빈 결과).
 * <p>지각/조퇴 판정은 raw 일시 stamp 비교(자정 넘김 보정 — web attd11 규칙).
 */
public interface AppAdminAttdService {

    /** 일자 근태 현황(직원별 출/퇴근/지각/조퇴/외근 요약). */
    DailyAttdResponse selectDaily(AdminDailyAttdParam param);

    /** 월별 집계(직원별 근무일수/근무시간/지각·조퇴 카운트). */
    MonthlyAttdResponse selectMonthly(AdminMonthlyAttdParam param);
}
