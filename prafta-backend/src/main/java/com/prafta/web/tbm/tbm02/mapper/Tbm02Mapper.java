package com.prafta.web.tbm.tbm02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbm02.application.command.SessionCancelCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionContentCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionPwdCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionRiskCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionStateCommand;
import com.prafta.web.tbm.tbm02.application.query.OptionQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionDetailQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionListQuery;
import com.prafta.web.tbm.tbm02.result.ContentOptionResult;
import com.prafta.web.tbm.tbm02.result.RiskOptionResult;
import com.prafta.web.tbm.tbm02.result.SessionContentResult;
import com.prafta.web.tbm.tbm02.result.SessionGuardResult;
import com.prafta.web.tbm.tbm02.result.SessionListResult;
import com.prafta.web.tbm.tbm02.result.SessionResult;
import com.prafta.web.tbm.tbm02.result.SessionRiskResult;
import com.prafta.web.tbm.tbm02.result.SiteOptionResult;

@Mapper
public interface Tbm02Mapper {

	/* ===== 채번 ===== */
	/** 세션코드 채번 (T + YYYYMMDD + SEQ). */
	String selectSessionCd(@Param("gvCmpnyCd") String gvCmpnyCd);

	/* ===== W-04 목록 ===== */
	List<SessionListResult> selectSessionList(SessionListQuery query);

	int selectSessionListCount(SessionListQuery query);

	/* ===== W-06 상세 ===== */
	SessionResult selectSessionDetail(SessionDetailQuery query);

	List<SessionContentResult> selectSessionContents(SessionDetailQuery query);

	List<SessionRiskResult> selectSessionRisks(SessionDetailQuery query);

	/** 상태/스코프 게이트 검증용 경량 조회. */
	SessionGuardResult selectSessionGuard(SessionDetailQuery query);

	/* ===== W-05/06 쓰기 ===== */
	void insertSession(SessionCommand command);

	void updateSession(SessionCommand command);

	void insertSessionContent(SessionContentCommand command);

	void deleteSessionContents(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("sessionCd") String sessionCd);

	void insertSessionRisk(SessionRiskCommand command);

	void deleteSessionRisks(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("sessionCd") String sessionCd);

	void upsertSessionState(SessionStateCommand command);

	void cancelSession(SessionCancelCommand command);

	void updateSessionPwd(SessionPwdCommand command);

	/* ===== 보조 조회 ===== */
	List<ContentOptionResult> selectContentOptions(OptionQuery query);

	List<RiskOptionResult> selectRiskOptions(OptionQuery query);

	List<SiteOptionResult> selectSiteOptions(@Param("gvCmpnyCd") String gvCmpnyCd);
}
