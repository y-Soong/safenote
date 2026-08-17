package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.result.MonthlyOvertimeResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttdRecordListResponse {
    List<MonthlyAttdListResult> attdRecordResultList;
    List<MonthlyAttdReqSummaryResult> monthlyAttdReqSummaryResultList;

    /* PRAFTA-017 - 일자별 초과근무 목록 (월 단위). selectMonthlyOvertimeList 결과. */
    List<MonthlyOvertimeResult> monthlyOvertimeResultList;

    /*
     * 연차 변경(이동/삭제) 활성 요청 요약 (월 단위) — 캘린더 셀 강조용.
     * 근태 요청 요약과 별도 리스트로 분리한다(프론트 "처리 필요" 폴백 카운트 중복 방지).
     * reqId 자리에 CHANGE_REQ_ID 가 들어가며, MOVE 는 출발일·이동대상일 두 행이 나올 수 있다.
     */
    List<MonthlyAttdReqSummaryResult> monthlyLeaveChangeSummaryResultList;

    /*
     * A안(2026-08-17): 확정 "시각 보유" 연차(반차/시간차) 구간 목록 (월 단위).
     * 목록 뷰 실근로/인정시간 표시에서 실근태와의 겹침을 차감하는 데 쓴다(연차 시간은 근로시간 미산입).
     * additive — 구 FE 는 무시(무회귀).
     */
    List<com.prafta.web.attd.attd07.result.MonthlyTimeLeaveWindowResult> timeLeaveWindowList;
}
