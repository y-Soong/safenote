package com.prafta.common.cmm.menu.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 즐겨찾기 토글 요청 body. menuDId 만 받는다.
 * CMPNY_CD/USER_CD 는 클라가 보낼 수 없으며 JWT 도출값만 신뢰한다(IDOR 방지).
 */
@Getter
@Setter
@NoArgsConstructor
public class MenuFavoriteToggleRequest {
	private String menuDId;
}
