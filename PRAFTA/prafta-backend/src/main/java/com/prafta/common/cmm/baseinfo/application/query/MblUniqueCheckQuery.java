package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 휴대폰번호 중복검사 Query.
 *
 * <p>★테넌트 격리: 실제 UNIQUE 제약은 {@code UX_TB_USER_MBL_NO = (CMPNY_CD, MBL_NO_HMAC)} 로
 *   <b>회사 안에서만</b> 유일하다. 종전 쿼리는 회사 조건 없이 전사를 검사해
 *   ① 다른 회사에 이미 등록된 번호라는 이유로 정상 가입이 막히고
 *   ② 타사 계정의 존재 여부가 노출(oracle)되는 문제가 있었다.
 *   검사 범위를 제약과 동일하게 회사 단위로 맞춘다.
 */
public record MblUniqueCheckQuery(
		String cmpnyCd       // 검사 대상 회사(요청 회사)
		, String mblNoHmac   // 사용자가 입력한 번호의 HMAC
) {
	public static MblUniqueCheckQuery from(String cmpnyCd, String mblNoHmac) {

		if(cmpnyCd == null || cmpnyCd.isBlank() || mblNoHmac == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new MblUniqueCheckQuery(
				cmpnyCd
				, mblNoHmac
		);
	}
}
