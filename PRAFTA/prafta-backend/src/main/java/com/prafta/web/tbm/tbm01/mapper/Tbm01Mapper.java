package com.prafta.web.tbm.tbm01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbm01.application.command.TbmEduInfoCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemInfoCommand;
import com.prafta.web.tbm.tbm01.application.query.TbmEduDetailQuery;
import com.prafta.web.tbm.tbm01.application.query.TbmEduInfoListQuery;
import com.prafta.web.tbm.tbm01.application.query.TbmEduItemInfoListQuery;
import com.prafta.web.tbm.tbm01.result.TbmEduInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduItemInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduUsedSessionResult;

@Mapper
public interface Tbm01Mapper {

	List<TbmEduInfoResult> selectTbmEduInfo(TbmEduInfoListQuery query);

	List<TbmEduItemInfoResult> selectTbmEduItemInfo(TbmEduItemInfoListQuery query);

	String selectMtrlCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);

	String selectMtrlItemCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);

	void mergeTbmEduInfo(TbmEduInfoCommand command);

	void mergeTbmEduItemInfo(TbmEduItemInfoCommand command);

	void deleteTbmEduItemInfo(TbmEduItemCommand command);

	void deleteTbmEduInfo(TbmEduInfoCommand command);

	/* prafta-033-A: W-03 상세 */

	/** 묶음 마스터 1건 + 스코프 산출(isCommonContent). 권한/스코프 검증용 SITE_CD 포함. */
	TbmEduInfoResult selectTbmEduDetail(TbmEduDetailQuery query);

	/** 묶음 세부항목(미디어) 목록 - 썸네일/길이 포함. */
	List<TbmEduItemInfoResult> selectTbmEduDetailItems(TbmEduDetailQuery query);

	/** 이 묶음을 사용한 TBM 세션 이력(B 이전 빈 목록). */
	List<TbmEduUsedSessionResult> selectTbmEduUsedSessions(TbmEduDetailQuery query);
}
