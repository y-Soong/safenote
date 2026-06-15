package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.LeaveOverlayResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.ShiftLockOverlayResult;
import com.prafta.web.attd.attd05.result.UserResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserWorkPlansResponse {
	List<UserResult> userListResultList;

	List<DayResult> dayResultList;

	List<SchedResult> schedResultList;

	// prafta-com-008-E-6: 그리드 연차 오버레이(종일 CONFIRMED). work_plan(SCH_CD) 위에 "연차" 표시.
	List<LeaveOverlayResult> leaveOverlayResultList;

	// prafta-com-008-D-5: 그리드 교대 잠금 오버레이. 교대팀 소속 구간(SCH 셀)을 비활성/자물쇠 표시.
	List<ShiftLockOverlayResult> shiftLockOverlayResultList;
}
