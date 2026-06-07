package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 직접 입실 요청(prafta-051-11). POST /tbm02/manager-enter.
 *
 * <p>관리자가 후보 검색 결과에서 특정 사용자를 직접 입실 처리한다. GPS/비밀번호 검증 없이
 * ENTRY_TYPE_CD='MANAGER_DIRECT' 로 기록되며, 회사/권한 식별자는 모두 JWT 에서 도출한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ManagerEnterRequest {
	private String sessionCd;	// 대상 세션
	private String userTypeCd;	// REGULAR | DAILY
	private String userCd;		// 입실 대상 사용자코드
}
