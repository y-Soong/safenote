package com.prafta.web.chkLst.chkLst05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 순회점검 이력 조회 요청(PRAFTA-SUBCON-T6-AUDIT-03) — 응답/불량조치 이력 공용 필터.
 * 사업장(siteCd)·기간(fromWorkDate~toWorkDate)은 필수, 점검대상/문항은 선택 좁힘 조건.
 */
@Getter
@Setter
@NoArgsConstructor
public class HistListRequest {
	private String siteCd;			// 사업장코드(필수)
	private String fromWorkDate;	// 점검일자 시작(YYYYMMDD, 필수)
	private String toWorkDate;		// 점검일자 종료(YYYYMMDD, 필수)
	private String chkptCd;			// 점검대상코드(선택)
	private String inspectItemCd;	// 점검문항코드(선택)
}
