package com.prafta.web.user.user06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 블랙리스트 등록 요청.
 *
 * <p>mblNo 는 평문 휴대폰(서버에서 정규화/HMAC/ENC/LAST4 변환), reason 은 등록 사유(필수, 최대 200자).
 * 회사코드는 클라가 보내지 않는다(서버 JWT 클레임 사용).
 */
@Getter
@Setter
@NoArgsConstructor
public class BlacklistRegRequest {
    private String mblNo;
    private String reason;
}
