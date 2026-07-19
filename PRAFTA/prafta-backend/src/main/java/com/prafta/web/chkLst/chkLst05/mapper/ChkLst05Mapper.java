package com.prafta.web.chkLst.chkLst05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.chkLst.chkLst05.application.query.HistListQuery;
import com.prafta.web.chkLst.chkLst05.result.AnswerHistResult;
import com.prafta.web.chkLst.chkLst05.result.DefectHistResult;

/**
 * 순회점검 결과 덮어쓰기 이력 조회 매퍼(PRAFTA-SUBCON-T6-AUDIT-03).
 *
 * <p>자사 스코프(CMPNY_CD=gv) + TB_USER_SITE_AUTH 조인(호출자 권한 사업장만)으로 IDOR/테넌트 격리를 이중 차단한다
 * (chkLst03/04 표준 승계). 조회 전용 — INSERT/UPDATE/DELETE 없음.
 */
@Mapper
public interface ChkLst05Mapper {

	/** 점검 응답 덮어쓰기 이력(좌표별 타임라인 오름차순). */
	List<AnswerHistResult> selectAnswerHistList(HistListQuery query);

	/** 불량조치 덮어쓰기 이력(좌표별 타임라인 오름차순). */
	List<DefectHistResult> selectDefectHistList(HistListQuery query);
}
