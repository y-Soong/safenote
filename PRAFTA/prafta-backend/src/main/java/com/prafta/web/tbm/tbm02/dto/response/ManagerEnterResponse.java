package com.prafta.web.tbm.tbm02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 직접 입실 응답(prafta-051-11).
 *
 * <p>restored=true 이면 내보내기 후 재입실(기존 행 복구), false 이면 신규 입실.
 */
@Getter
@Builder
public class ManagerEnterResponse {
	private String sessionCd;
	private String userTypeCd;
	private String userCd;
	private boolean restored;
}
