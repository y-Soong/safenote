package com.prafta.web.tbm.tbm01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbm01.application.command.TbmEduAiAnalyzeCommand;
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

	/** 회사 스코프 가드(IDOR 방어): 공급된 mtrlCd 가 호출자 회사 소유인지 COUNT. 0 이면 타 회사/미존재. */
	int countOwnedMtrl(@Param(value = "mtrlCd") String mtrlCd, @Param(value = "gvCmpnyCd") String gvCmpnyCd);

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

	/* T5-2: 사용 중 교육자료 수정/삭제 차단 가드 */

	/** 이 교육자료를 참조하는 활성(취소 외) 세션 수. 0 초과면 수정/삭제 잠금. */
	int selectTbmEduLockingSessionCnt(@Param("mtrlCd") String mtrlCd, @Param("gvCmpnyCd") String gvCmpnyCd);

	/** 세부항목 코드로 소속 교육자료(MTRL_CD) 도출(서버 도출, IDOR 방어). 없으면 NULL. */
	String selectMtrlCdByItemCd(@Param("mtrlItemCd") String mtrlItemCd, @Param("gvCmpnyCd") String gvCmpnyCd);

	/**
	 * 세부항목의 AI 분석 지정(AI_ANALYZE_YN)만 갱신.
	 * <p>사용 중(잠긴) 교육자료도 이 경로로 AI 지정을 켜고/끌 수 있다. 잠금 검증은 생략하되
	 * WHERE 절 회사 스코프로 IDOR 을 차단한다. 해제(Y→N) 시 AI_STATUS 는 'NONE' 리셋
	 * (mergeTbmEduItemInfo 와 동일 규칙), 재체크(=Y)면 기존 상태 보존.
	 */
	int updateTbmEduItemAiAnalyze(TbmEduAiAnalyzeCommand command);
}
