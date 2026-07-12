package com.prafta.web.user.user06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 블랙리스트 목록 검색 요청.
 *
 * <p>조회조건: 전화번호(mblNo, 서버에서 정규화→HMAC/LAST4 검색), 사용여부(useYn: ''/Y/N).
 * 회사코드는 클라가 보내지 않는다(서버 JWT 클레임 사용).
 */
@Getter
@Setter
@NoArgsConstructor
public class BlacklistListRequest {
    private String mblNo;   // 평문 입력. 서버에서 정규화 후 HMAC/LAST4 로만 검색(평문 비교 금지).
    private String useYn;   // ''(전체) / 'Y'(사용) / 'N'(해제)
}
