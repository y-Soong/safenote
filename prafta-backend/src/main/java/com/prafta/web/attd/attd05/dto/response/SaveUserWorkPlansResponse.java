package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.SkippedCellResult;

import lombok.Builder;
import lombok.Value;

/**
 * 근무계획 저장 응답.
 * - savedCount : 실제 저장(INSERT/UPDATE)된 셀 수
 * - skippedList : 근무타입 검증 위반으로 저장에서 제외된 셀 목록
 */
@Value
@Builder
public class SaveUserWorkPlansResponse {
	int savedCount;

	List<SkippedCellResult> skippedList;
}
