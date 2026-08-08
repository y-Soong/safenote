package com.prafta.common.cmm.baseinfo.application.command;

import com.prafta.common.cmm.baseinfo.application.param.UserSmsAuthNoCheckParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 인증번호 검증 성공 처리 커맨드.
 *
 * @param verifyFailLimit [3차] 대입 실패 허용 횟수(정책값). select → update 사이 TOCTOU 를 닫는
 *                        {@code AND FAIL_CNT < #{verifyFailLimit}} 조건에 쓴다.
 *                        ★조회({@code UserSmsAuthNoCheckQuery})와 반드시 같은 값이어야 한다.
 */
public record MblUniqueCheckCommand(
	String smsId
    , String mblNoHmac
    , String certNo
    , int verifyFailLimit
) {
	public static MblUniqueCheckCommand from(String smsId, String mblNoHmac, UserSmsAuthNoCheckParam param,
			int verifyFailLimit) {

		if(smsId == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new MblUniqueCheckCommand(
			smsId
			, mblNoHmac
			, param.certNo()
			, verifyFailLimit
		);
	}
}
