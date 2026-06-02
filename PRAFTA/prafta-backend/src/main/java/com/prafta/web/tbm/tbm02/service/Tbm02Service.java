package com.prafta.web.tbm.tbm02.service;

import com.prafta.web.tbm.tbm02.application.param.OptionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionCancelParam;
import com.prafta.web.tbm.tbm02.application.param.SessionDetailParam;
import com.prafta.web.tbm.tbm02.application.param.SessionListParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPwdParam;
import com.prafta.web.tbm.tbm02.application.param.SessionSaveParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;
import com.prafta.web.tbm.tbm02.dto.response.ContentOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.RiskOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionDetailResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionSaveResponse;
import com.prafta.web.tbm.tbm02.dto.response.SiteOptionResponse;

public interface Tbm02Service {

	/** W-04 세션 목록(필터/페이징/집계). */
	SessionListResponse selectSessionList(SessionListParam param);

	/** W-06 세션 상세(콘텐츠/위험성평가 매핑, 비번 노출 게이트). */
	SessionDetailResponse selectSessionDetail(SessionDetailParam param);

	/** W-05 개설(OPENED) / 임시저장(DRAFT). */
	SessionSaveResponse saveSession(SessionSaveParam param);

	/** W-06 수정(DRAFT/OPENED만). */
	void updateSession(SessionUpdateParam param);

	/** W-06 취소(DRAFT/OPENED만). */
	void cancelSession(SessionCancelParam param);

	/** W-06 비밀번호 재발급(OPENED만). */
	SessionPwdResponse regeneratePasswords(SessionPwdParam param);

	/** 콘텐츠 선택 모달 옵션. */
	ContentOptionResponse selectContentOptions(OptionParam param);

	/** 위험성평가 선택 모달 옵션. */
	RiskOptionResponse selectRiskOptions(OptionParam param);

	/** 사업장 선택 드롭다운 옵션. */
	SiteOptionResponse selectSiteOptions(OptionParam param);
}
