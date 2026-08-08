package com.prafta.common.cmm.baseinfo.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SmsAuthNoCommand(
	String mblNoEnc
    , String mblNoHmac
    , String certNo
    // SMS-PPURIO-04: 발송 추적키(TB_SMS_AUTH_CODE.SEND_REF_KEY).
    // record 는 setter 가 없어 useGeneratedKeys 로 PK 를 되받을 수 없으므로,
    // INSERT 전에 생성한 refKey 를 함께 저장해 발송 결과 UPDATE 의 조인키로 쓴다.
    , String sendRefKey
    // SMS2-B4: 요청 IP 해시(TB_SMS_AUTH_CODE.SEND_IP_HASH). IP 축 상한 카운트 재료.
    // ★평문 IP 가 아니라 HMAC 해시다(공통 정책서 §11.1 최소 수집). 확정 불가 시 null 허용.
    , String sendIpHash
) {
	public static SmsAuthNoCommand from(String mblNoEnc, String mblNoHmac, String certNo, String sendRefKey, String sendIpHash) {

		if(mblNoEnc == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(certNo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(sendRefKey == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		// ★sendIpHash 는 null 을 허용한다(fail-open). null 체크를 넣으면 IP 미확정 시 발송이 통째로 막힌다.

		return new SmsAuthNoCommand(
			mblNoEnc
			, mblNoHmac
			, certNo
			, sendRefKey
			, sendIpHash
		);
	}
}
