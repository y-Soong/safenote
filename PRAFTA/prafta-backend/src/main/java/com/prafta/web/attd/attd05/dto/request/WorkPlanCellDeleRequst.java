package com.prafta.web.attd.attd05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRAFTA-041 - 근무계획 셀(사용자+근무일) 단위 삭제 요청.
 *
 * <p>기존 {@link SchTypeDeleRequst}(사용자+월 단위 삭제)와 달리 근무일(WORK_YMD)을 셀 단위로 식별한다.
 * 비우는 셀이 법정 연차였으면 서버가 직접 연차 사용기록 취소(차감 복원)를 함께 수행한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkPlanCellDeleRequst {
	private String siteCd;
	private String userCd;
	private String workYmd;
}
