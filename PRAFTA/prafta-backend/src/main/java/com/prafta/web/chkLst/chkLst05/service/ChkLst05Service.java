package com.prafta.web.chkLst.chkLst05.service;

import com.prafta.web.chkLst.chkLst05.application.param.HistListParam;
import com.prafta.web.chkLst.chkLst05.dto.response.AnswerHistListResponse;
import com.prafta.web.chkLst.chkLst05.dto.response.DefectHistListResponse;

/**
 * 순회점검 결과 덮어쓰기 이력 조회 서비스(PRAFTA-SUBCON-T6-AUDIT-03).
 */
public interface ChkLst05Service {

	/** 점검 응답 덮어쓰기 이력 조회(좌표별 타임라인). */
	AnswerHistListResponse selectAnswerHistList(HistListParam param);

	/** 불량조치 덮어쓰기 이력 조회(좌표별 타임라인). */
	DefectHistListResponse selectDefectHistList(HistListParam param);
}
