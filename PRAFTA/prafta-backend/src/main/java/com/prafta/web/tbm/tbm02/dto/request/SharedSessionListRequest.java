package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연동받은 교육 목록 요청(PRAFTA-SUBCON-T5 D2). GET /tbm02/shared-sessions.
 *
 * <p>회사 식별자는 JWT 에서만 도출한다. 세션 범위는 서버가 SHARE 테이블로 결정하므로 클라이언트가
 * 세션코드/회사코드를 지정하는 파라미터는 없다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SharedSessionListRequest {
	private String statusCd;		// 상태 필터(옵션)
	private String searchKeyword;	// 교육 제목 검색(옵션)
	private Integer page;
	private Integer pageSize;
}
