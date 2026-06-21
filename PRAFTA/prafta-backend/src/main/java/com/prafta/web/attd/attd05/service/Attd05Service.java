package com.prafta.web.attd.attd05.service;

import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeDeleParam;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeParam;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.dto.response.DeleteUserWorkPlansResponse;
import com.prafta.web.attd.attd05.dto.response.DeleteWorkPlanCellsResponse;
import com.prafta.web.attd.attd05.dto.response.LeaveTypeResponse;
import com.prafta.web.attd.attd05.dto.response.SaveUserWorkPlansResponse;
import com.prafta.web.attd.attd05.dto.response.SchTypeListResponse;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;

public interface Attd05Service {

	UserWorkPlansResponse getUserWorkPlan(UserWorkPlansParam param);

	SchTypeListResponse getSchTypeList(SchTypeListParam param);

	LeaveTypeResponse getLeaveTypeList(LeaveTypeListParam param);

	SaveUserWorkPlansResponse saveUserWorkPlans(SchTypeParam param);

	/**
	 * 근무계획 월 단위 삭제 (prafta-com-016-C-3).
	 * <p>초과근무(OT) 등록/신청 보유일은 삭제에서 제외(부분 삭제)하고, 제외된 일자를 skippedList 로 반환한다.
	 * 연차 등록일도 SQL(NOT EXISTS leave_use)로 보존된다. 마감/교대 가드는 기존대로 전체 차단(hard-throw).
	 */
	DeleteUserWorkPlansResponse deleteUserWorkPlans(SchTypeDeleParam param);

	/**
	 * PRAFTA-041 - 근무계획 셀(사용자+근무일) 단위 삭제. 비우는 셀이 직접(REQ_ID NULL) 법정 연차였으면
	 * 직접 연차 사용기록 취소(차감 복원)를 함께 수행한다.
	 * <p>prafta-com-008-E (M2): 승인기반(REQ_ID NOT NULL) 종일 연차가 있는 셀은 직접 삭제할 수 없어
	 * skip 하고, skippedList/카운트로 사용자에게 안내한다(무결성 변경 없이 피드백만).
	 */
	DeleteWorkPlanCellsResponse deleteUserWorkPlanCells(com.prafta.web.attd.attd05.application.param.WorkPlanCellDeleParam param);
}
