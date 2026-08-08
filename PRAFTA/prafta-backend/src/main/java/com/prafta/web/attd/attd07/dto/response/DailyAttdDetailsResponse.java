package com.prafta.web.attd.attd07.dto.response;

import java.util.List;

import com.prafta.web.attd.attd07.result.ConfirmedLeaveResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyLeaveChangeReqResult;
import com.prafta.web.attd.attd07.result.DailyOvertimeResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.NeighborAttdSegmentView;
import com.prafta.web.attd.attd07.result.OtLeaveExemptWindowView;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyAttdDetailsResponse {
    DailyAttdDetailsResult dailyAttdDetailsResult;
    List<DailyAttdDetailHistoryResult> dailyAttdDetailHistoryResultList;
    /** PRAFTA-003-7: 응답 키 명명 컨벤션(lowerCamel)에 맞춰 `monthlyAttdReqResultList`로 정규화. */
    List<MonthlyAttdReqResult> monthlyAttdReqResultList;
    /** PRAFTA-003-6: 해당 일자에 등록된 초과근무(OT) 목록. 응답 키는 lowerCamel(`dailyOvertimeResultList`). */
    List<DailyOvertimeResult> dailyOvertimeResultList;
    /** PRAFTA-APP-018-F: 그날 확정 연차 사용내역(자동확정/직접 포함, 미처리 결재대기 제외). 표시 전용. */
    List<ConfirmedLeaveResult> confirmedLeaveResultList;

    /**
     * 그날 걸려 있는 연차 변경(이동/삭제) 활성 요청 목록 — 출발일·이동대상일 양쪽 매칭.
     * AGREED(관리자 확인대기)는 팝업에서 확인/반려 처리 가능, REQUESTED(근로자 응답대기)는 표시 전용.
     */
    List<DailyLeaveChangeReqResult> leaveChangeReqResultList;

    /**
     * PC-07(N8): 대상 사용자·대상일 기준 1일 환산시간(분, 개인 분모 480 캡 — 미산출 480 폴백).
     * AttdDayDetailPop 의 연차 "N일 H시간 M분" 표기 분모(기존 480 고정 폴백 결함 D2 해소, additive).
     */
    int convMinutes;

    /**
     * 겹침가드 개선(2026-08-06): 앞뒤 근무일(D-1 / D+1) 근태 구간 목록(당일 구간은 제외 — time-card 가 이미 표시).
     * 이웃 근무일의 미마감 근태가 이 날짜의 근태 등록·승인을 막는 원인일 때 화면에서 특정할 수 있게 한다.
     * 표시 문자열·상태(status)는 서버가 완성해 내려준다(프론트 재판정 금지). 0건이면 빈 리스트.
     */
    List<NeighborAttdSegmentView> neighborAttdSegmentList;

    /**
     * OT 칩 정합(2026-08-08): 그날 확정 부분연차(반차/시간차)의 면제 구간 — "등록 가능" OT 칩이
     * FE 자체 계산(실근태−스케줄)에 이 구간을 추가로 빼서 서버 검증(ATTD_400_012)과 일치시킨다.
     * 산출 = OT 저장 검증과 동일 단일 출처(buildLeaveExemptSegments). stamp 축 = FE 와 동일
     * (workYmd−1 00:00 원점). additive — 구 FE 는 무시. 0건이면 빈 리스트.
     */
    List<OtLeaveExemptWindowView> otLeaveExemptWindowList;
}
