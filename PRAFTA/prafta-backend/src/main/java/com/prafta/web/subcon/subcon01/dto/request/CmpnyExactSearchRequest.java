package com.prafta.web.subcon.subcon01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회사 정확일치 조회 요청.
 *
 * <p>cmpnyCd 는 상대 회사코드 전체 문자열(정확일치 전용 — 부분검색/LIKE 금지, 열거 방지).
 * 자기 회사코드는 클라가 보내지 않는다(서버 JWT 클레임 사용).
 */
@Getter
@Setter
@NoArgsConstructor
public class CmpnyExactSearchRequest {
    private String cmpnyCd;   // 조회 대상(상대) 회사코드 — 등호 비교만.
}
