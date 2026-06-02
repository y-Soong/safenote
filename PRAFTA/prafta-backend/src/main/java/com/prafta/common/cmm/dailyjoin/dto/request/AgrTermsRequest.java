package com.prafta.common.cmm.dailyjoin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일일사용자 회원가입 - 약관 동의 항목.
 * 약관 버전은 클라이언트 신뢰값이 아닌 서버 조회값을 저장하므로 termsId 만 수신한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AgrTermsRequest {
    private String termsId;
}
