package com.prafta.web.subcon.subcon01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연동 관계 요청 생성 요청.
 *
 * <p>요청측 회사코드는 클라가 보내지 않는다(서버 JWT 클레임 gv_cmpnyCd 사용 — 바디 회사코드 불신).
 */
@Getter
@Setter
@NoArgsConstructor
public class RelationCreateRequest {
    private String tgtCmpnyCd;   // 상대측 회사코드(정확일치 조회로 확인된 값)
}
