package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.SkippedCellResult;

import lombok.Builder;
import lombok.Value;

/**
 * 근무계획 월 단위 삭제 응답 (prafta-com-016-C-3).
 *
 * <p>월 삭제는 초과근무(OT) 등록/신청 보유일을 <b>부분 삭제로 제외</b>한다(전체 차단이 아닌
 * 일 단위 부분 삭제). 제외된 OT 보유일은 {@code skippedList}(사유 HAS_OVERTIME)로 반환하여
 * 프론트가 BatchResultPop 로 안내한다. 연차 등록일은 SQL(NOT EXISTS leave_use)로 보존된다.
 */
@Value
@Builder
public class DeleteUserWorkPlansResponse {

	/** 실제 삭제된 사용자 수(요청 사용자 중 삭제 시도 대상). 참고용. */
	int deletedUserCount;

	/** F-11-1: 실제 삭제된 근무계획(TB_USER_WORK_PLAN) 행 수 합계. 프론트 결과 팝업의 성공 건수 표시용. */
	int deletedCount;

	/** OT 보유로 삭제에서 제외된 (사용자, 일자) 목록. */
	List<SkippedCellResult> skippedList;
}
