package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 이번주 일별 바텀시트 4액션 활성도 (계약 §3.2 days[].actions).
 * <p>전부 서버 산출(매트릭스: UI-A006). 프론트는 표시만.
 */
@Getter
@Builder
public class WeekDayActionsResponse {
    private final boolean canRequestScheduleModify; // 스케줄 수정
    private final boolean canRequestAttendanceCorrection; // 근태 보정
    private final boolean canRequestOvertime; // 초과근무 (근태 마감 전까지, 재기획서 §3.2)
    private final boolean canRequestLeave; // 연차 신청
    // prafta-app-013: 스케줄 없는 날 연차는 full-day(온전한 하루)만 허용한다는 힌트 플래그(=!hasSchedule).
    //   연차 신청 폼이 반차/시간차 차단에 소비(Follow-up F1). 본 작업은 계약만 내림.
    //   leaveFullDayOnly 는 is 접두가 아니므로 Jackson 직렬화 명칭 안전(메모리 함정 회피).
    private final boolean leaveFullDayOnly;
}
