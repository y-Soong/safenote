package com.prafta.common.cmm.login.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 셀프가입 - 약관 동의 항목.
 *
 * <p>약관 버전은 클라이언트 신뢰값이 아닌 서버 조회값(TB_TERMS.TERMS_VERSION)을 저장하므로
 * termsId 만 수신한다(일용직 {@code dailyjoin.dto.request.AgrTermsRequest} 와 동일 계약).
 */
@Getter
@Setter
@NoArgsConstructor
public class AgrTermsRequest {
	private String termsId;
}
