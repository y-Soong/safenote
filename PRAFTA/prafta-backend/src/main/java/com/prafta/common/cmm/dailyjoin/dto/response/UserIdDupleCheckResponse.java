package com.prafta.common.cmm.dailyjoin.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 일일사용자 회원가입 - 사용자ID 중복체크 응답.
 * uniqueYn : 'Y'(사용 가능) / 'N'(중복)
 */
@Value
@Builder
public class UserIdDupleCheckResponse {
    String uniqueYn;
}
