package com.prafta.common.cmm.login.service;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.login.application.param.AuthMenuInfoParam;
import com.prafta.common.cmm.login.application.param.LoginParam;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;
import com.prafta.common.cmm.login.application.param.VerifyPhoneAuthParam;
import com.prafta.common.cmm.login.dto.response.LoginResponse;
import com.prafta.common.cmm.login.dto.response.UserJoinResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;

public interface LoginService {
	LoginResponse Login(LoginParam param);

	void logout(LogoutParam param);

	/**
	 * 셀프가입(회원가입) 접수 — 소정-04 이후 <b>관리자 승인 대기('06' + USE_YN='N')</b>로 끝난다.
	 *
	 * <p>동일 아이디/휴대폰이 거부('07') 행과 일치하면 그 행을 재활용해 재가입시킨다(plan §8 Q2).
	 *
	 * <p>[security H-1] 진입부에서 서버측 휴대폰 본인인증(인증 완료 기록 조회 + 소비)을 강제한다.
	 * [security C-1] 권한은 서버가 '99999'(일반사원)로 고정한다 — 요청 바디의 권한은 받지 않는다.
	 *
	 * @param auditContext 감사 컨텍스트(IP/UA) — 재가입 재활용 적재에 쓴다. 없으면 null 허용.
	 * @return 승인 대기 신호(클라이언트가 "로그인 해주세요" 대신 승인대기 화면으로 분기)
	 */
	UserJoinResponse insertUserInfo(UserJoinParam param, AuditContext auditContext);

	UserTermsAgreementCheckResponse userTermsAgrementCheck(UserTermsAgreementCheckParam param);

//	List<Map<String, Object>> selectUserTermsAgrChk(LoginRequest dto);

	void updateAuthMenuInfo(AuthMenuInfoParam param);

	// ===== PRAFTA-036 - 휴대폰 인증대기 계정 활성화 =====
	LoginResponse verifyPhoneAuth(VerifyPhoneAuthParam param);

	/** 휴대폰 본인인증 팝업 자동기입용 — PHONE_AUTH 토큰 식별 사용자('04')의 등록 휴대폰(복호) 반환. 미등록이면 빈 문자열. */
	String getPhoneAuthTargetPhone(String cmpnyCd, String userCd);

	// ===== PRAFTA-COM-008-E-8 - 기본 근무타입 로그인 게이트 통과(설정 저장 + 즉시 생성 + 정식 토큰) =====
	LoginResponse setDefaultSch(String gvCmpnyCd, String gvUserCd, String defaultSchCd, String clientType);
}
