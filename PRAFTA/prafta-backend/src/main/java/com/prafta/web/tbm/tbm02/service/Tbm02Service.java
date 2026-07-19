package com.prafta.web.tbm.tbm02.service;

import com.prafta.web.tbm.tbm02.application.param.EjectAttendanceParam;
import com.prafta.web.tbm.tbm02.application.param.EntryCandidateParam;
import com.prafta.web.tbm.tbm02.application.param.ManagerEnterParam;
import com.prafta.web.tbm.tbm02.application.param.OptionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionCancelParam;
import com.prafta.web.tbm.tbm02.application.param.SessionDetailParam;
import com.prafta.web.tbm.tbm02.application.param.SessionListParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPrepareParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPwdParam;
import com.prafta.web.tbm.tbm02.application.param.SessionSaveParam;
import com.prafta.web.tbm.tbm02.application.param.SessionShareParam;
import com.prafta.web.tbm.tbm02.application.param.SharedSessionListParam;
import com.prafta.web.tbm.tbm02.dto.response.SharedSessionListResponse;
import com.prafta.web.tbm.tbm02.application.param.SessionTransitionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;
import com.prafta.web.tbm.tbm02.dto.response.ContentOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionShareCandidateResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionShareListResponse;
import com.prafta.web.tbm.tbm02.dto.response.ShareAllowedCmpnyResponse;
import com.prafta.web.tbm.tbm02.dto.response.EntryCandidateResponse;
import com.prafta.web.tbm.tbm02.dto.response.ManagerEnterResponse;
import com.prafta.web.tbm.tbm02.dto.response.RiskOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionAttendanceListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionCompleteResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionDetailResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionExitPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPrepareResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionSaveResponse;
import com.prafta.web.tbm.tbm02.dto.response.SiteOptionResponse;

public interface Tbm02Service {

	/** W-04 세션 목록(필터/페이징/집계). */
	SessionListResponse selectSessionList(SessionListParam param);

	/** W-06 세션 상세(콘텐츠/위험성평가 매핑, 비번 노출 게이트). */
	SessionDetailResponse selectSessionDetail(SessionDetailParam param);

	/** W-05 개설(DRAFT 고정, prafta-051 C2). */
	SessionSaveResponse saveSession(SessionSaveParam param);

	/** W-06 수정(DRAFT/OPENED만). */
	void updateSession(SessionUpdateParam param);

	/** W-06 취소(DRAFT/OPENED만). */
	void cancelSession(SessionCancelParam param);

	/** 입실 비밀번호 재발급(OPENED만, 입실비번 전용). */
	SessionPwdResponse regeneratePasswords(SessionPwdParam param);

	/** 교육준비(OPENED) 전이 + 입실비번 발급 + GPS 중심좌표(prafta-051-03). */
	SessionPrepareResponse prepareSession(SessionPrepareParam param);

	/** 교육시작(IN_PROGRESS) 수동 전이(prafta-051-04). */
	void startSession(SessionTransitionParam param);

	/** 교육준비 연장(PREP_START_AT 리셋, prafta-051-04). */
	void extendPrep(SessionTransitionParam param);

	/** 교육종료(COMPLETED) 전이 + 종료비번 발급(prafta-051-05). */
	SessionCompleteResponse completeSession(SessionTransitionParam param);

	/** 종료 비밀번호 재발급(COMPLETED만, 종료비번 전용, prafta-051-02). */
	SessionExitPwdResponse regenerateExitPassword(SessionTransitionParam param);

	/** 입실 후보 검색(정규직/일용직, prafta-051-11). */
	EntryCandidateResponse selectEntryCandidates(EntryCandidateParam param);

	/** 관리자 직접 입실(MANAGER_DIRECT, prafta-051-11). */
	ManagerEnterResponse managerEnter(ManagerEnterParam param);

	/** 교육준비 단계 입실자 명단(거리/입실유형, prafta-051-12). */
	SessionAttendanceListResponse selectSessionAttendances(SessionDetailParam param);

	/** 입실자 내보내기(soft delete, prafta-051-12). */
	void ejectAttendance(EjectAttendanceParam param);

	/* ===== PRAFTA-SUBCON-T5 연동 회사 지정 ===== */

	/** D2: 연동받은 교육 목록(비개설사 전용, 헤더 최소 필드 + 재지정 진입점). */
	SharedSessionListResponse selectSharedSessionList(SharedSessionListParam param);

	/** 연동 회사 지정 후보(관계 ACCEPTED − 개설사 − 이미 체인에 있는 회사). */
	SessionShareCandidateResponse selectShareCandidates(SessionShareParam param);

	/** 연동 회사 지정 현황(내가 직접 지정한 회사 + 하위 재지정 개사 수). */
	SessionShareListResponse selectSessionShares(SessionShareParam param);

	/** 입실 대상 회사 목록(개설사 + 지정 체인, relabel 적용). */
	ShareAllowedCmpnyResponse selectAllowedCmpnys(SessionShareParam param);

	/** 연동 회사 지정(DRAFT/OPENED 만). */
	void designateShare(SessionShareParam param);

	/** 연동 회사 지정 해제(자기 지정분만) + 하위 캐스케이드. */
	void releaseShare(SessionShareParam param);

	/** 콘텐츠 선택 모달 옵션. */
	ContentOptionResponse selectContentOptions(OptionParam param);

	/** 위험성평가 선택 모달 옵션. */
	RiskOptionResponse selectRiskOptions(OptionParam param);

	/** 사업장 선택 드롭다운 옵션. */
	SiteOptionResponse selectSiteOptions(OptionParam param);
}
