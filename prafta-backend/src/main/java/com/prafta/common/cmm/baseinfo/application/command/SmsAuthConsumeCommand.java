package com.prafta.common.cmm.baseinfo.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * SMS 인증 레코드 소비(consume) 처리 Command.
 * 비밀번호 재설정 검증에 사용된 인증 레코드를 재사용 불가 상태로 전환한다.
 */
public record SmsAuthConsumeCommand(
	String smsId
) {
	public static SmsAuthConsumeCommand from(String smsId) {

		if(smsId == null || smsId.isBlank())
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new SmsAuthConsumeCommand(smsId);
	}
}
