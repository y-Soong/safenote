package com.prafta.web.chkLst.chkLst05.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.web.chkLst.chkLst05.application.param.HistListParam;
import com.prafta.web.chkLst.chkLst05.application.query.HistListQuery;
import com.prafta.web.chkLst.chkLst05.dto.response.AnswerHistListResponse;
import com.prafta.web.chkLst.chkLst05.dto.response.DefectHistListResponse;
import com.prafta.web.chkLst.chkLst05.mapper.ChkLst05Mapper;
import com.prafta.web.chkLst.chkLst05.result.AnswerHistResult;
import com.prafta.web.chkLst.chkLst05.result.DefectHistResult;
import com.prafta.web.chkLst.chkLst05.service.ChkLst05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst05ServiceImpl implements ChkLst05Service {

	private final ChkLst05Mapper chkLst05Mapper;

	@Override
	public AnswerHistListResponse selectAnswerHistList(HistListParam param) {

		log.info("순회점검 응답 이력 조회 진입 - siteCd={}, from={}, to={}, chkptCd={}, inspectItemCd={}",
				param.siteCd(), param.fromWorkDate(), param.toWorkDate(), param.chkptCd(), param.inspectItemCd());

		// 사업장 스코프/IDOR 는 쿼리 내 CMPNY_CD=gv + TB_USER_SITE_AUTH 조인으로 호출자 권한 사업장만 노출한다.
		List<AnswerHistResult> answerHistList = chkLst05Mapper.selectAnswerHistList(
				HistListQuery.from(param, HistListQuery.FILE_TYPE_INSPECT));

		return AnswerHistListResponse.builder()
				.answerHistList(answerHistList)
				.build();
	}

	@Override
	public DefectHistListResponse selectDefectHistList(HistListParam param) {

		log.info("순회점검 불량조치 이력 조회 진입 - siteCd={}, from={}, to={}, chkptCd={}, inspectItemCd={}",
				param.siteCd(), param.fromWorkDate(), param.toWorkDate(), param.chkptCd(), param.inspectItemCd());

		List<DefectHistResult> defectHistList = chkLst05Mapper.selectDefectHistList(
				HistListQuery.from(param, HistListQuery.FILE_TYPE_DEFECT_ACTION));

		return DefectHistListResponse.builder()
				.defectHistList(defectHistList)
				.build();
	}
}
