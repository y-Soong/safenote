package com.prafta.common.cmm.dailyjoin.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 일일사용자 회원가입 응답.
 * 발급된 사용자ID를 반환한다.
 */
@Value
@Builder
public class InsertDailyUserResponse {
    String userId;
}
