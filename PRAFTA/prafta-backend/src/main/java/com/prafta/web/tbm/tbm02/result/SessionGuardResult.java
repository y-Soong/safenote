package com.prafta.web.tbm.tbm02.result;

/**
 * 상태/스코프 게이트 검증용 경량 조회 결과.
 *
 * <p>수정/취소/비번재발급 전, 현재 상태와 사업장/개설자를 확인해 비즈니스 룰과
 * 스코프 격리를 서버에서 재검증한다.
 */
public record SessionGuardResult(
	String sessionCd
	, String siteCd
	, String statusCd
	, String managerUserCd
){

}
