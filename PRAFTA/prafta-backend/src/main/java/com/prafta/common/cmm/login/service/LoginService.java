package com.prafta.common.cmm.login.service;

import com.prafta.common.cmm.login.application.param.AuthMenuInfoParam;
import com.prafta.common.cmm.login.application.param.LoginParam;
import com.prafta.common.cmm.login.application.param.LogoutParam;
import com.prafta.common.cmm.login.application.param.UserJoinParam;
import com.prafta.common.cmm.login.application.param.UserTermsAgreementCheckParam;
import com.prafta.common.cmm.login.application.param.VerifyPhoneAuthParam;
import com.prafta.common.cmm.login.dto.response.LoginResponse;
import com.prafta.common.cmm.login.dto.response.UserTermsAgreementCheckResponse;

public interface LoginService {
	LoginResponse Login(LoginParam param);

	void logout(LogoutParam param);

	void insertUserInfo(UserJoinParam param);

	UserTermsAgreementCheckResponse userTermsAgrementCheck(UserTermsAgreementCheckParam param);

//	List<Map<String, Object>> selectUserTermsAgrChk(LoginRequest dto);

	void updateAuthMenuInfo(AuthMenuInfoParam param);

	// ===== PRAFTA-036 - 휴대폰 인증대기 계정 활성화 =====
	LoginResponse verifyPhoneAuth(VerifyPhoneAuthParam param);

	// ===== PRAFTA-COM-008-E-8 - 기본 근무타입 로그인 게이트 통과(설정 저장 + 즉시 생성 + 정식 토큰) =====
	LoginResponse setDefaultSch(String gvCmpnyCd, String gvUserCd, String defaultSchCd, String clientType);
}
