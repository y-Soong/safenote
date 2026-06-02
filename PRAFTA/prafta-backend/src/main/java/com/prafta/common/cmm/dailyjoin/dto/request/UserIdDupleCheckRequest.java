package com.prafta.common.cmm.dailyjoin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일일사용자 회원가입 - 사용자ID 중복체크 요청.
 * TB_DAILY_USER 대상, USE_YN='Y' 행만 카운트.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserIdDupleCheckRequest {
    private String cmpnyCd;
    private String userId;
}
