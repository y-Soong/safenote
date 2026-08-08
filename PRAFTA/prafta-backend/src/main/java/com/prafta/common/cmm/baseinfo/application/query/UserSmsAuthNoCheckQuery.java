package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 인증번호 검증 조회 조건.
 *
 * @param verifyFailLimit [3차] 대입 실패 허용 횟수(정책값 {@code TB_SMS_SEND_POLICY.VERIFY_FAIL_LIMIT}).
 *                        {@code SmsVerifyGuard.beforeVerify} 가 돌려준 값을 그대로 싣는다(4차 개명).
 *                        ★코드에 임계값을 두지 않기 위해 쿼리 파라미터로 왕복시킨다.
 */
public record UserSmsAuthNoCheckQuery(
	String cmpnyCd
    , String mblNoHmac
    , String certNo
    , int verifyFailLimit
) {
	public static UserSmsAuthNoCheckQuery from(UserSmsAuthNoCheckParam param, String mblNoHmac, int verifyFailLimit) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new UserSmsAuthNoCheckQuery(
				param.cmpnyCd()
				, mblNoHmac
				, param.certNo()
				, verifyFailLimit
		);
	}
}
