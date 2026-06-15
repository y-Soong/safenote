package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.SkippedCellResult;

import lombok.Builder;
import lombok.Value;

/**
 * prafta-com-008-E (M2): 근무계획 셀 비우기 응답.
 * <p>승인기반(REQ_ID NOT NULL) 종일 연차가 포함된 셀은 셀에서 직접 삭제할 수 없어 skip 하고,
 * 그 사유/카운트를 함께 내려 사용자에게 안내한다(무결성 변경 없이 피드백만 추가).
 * 직접 연차(REQ_ID IS NULL)·일반 근무 셀은 기존대로 즉시 삭제된다.
 * <ul>
 *   <li>deletedCount : 실제 비워진(삭제된) 셀 수</li>
 *   <li>leaveRestoredCount : 직접 연차 차감 복원이 함께 수행된 셀 수</li>
 *   <li>skippedList : 승인기반 연차로 비우기에서 제외된 셀 목록(SaveUserWorkPlansResponse 와 동일 구조 재사용)</li>
 * </ul>
 */
@Value
@Builder
public class DeleteWorkPlanCellsResponse {

	int deletedCount;

	int leaveRestoredCount;

	List<SkippedCellResult> skippedList;
}
