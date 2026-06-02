package com.prafta.web.attd.attd05.result;

import java.util.List;

/**
 * 근무타입(SCH_CD)별 검증 메타.
 * - createDt : 최초 생성일 = MIN(APPLY_DATE) (현재본 + 이력본 합집합)
 * - versionList : APPLY_DATE 오름차순 정렬된 effective-dating 버전 목록
 *   (프론트가 임의 날짜의 차단 여부를 클라이언트에서 계산하기 위해 사용)
 */
public record SchTypeValidMeta(
	String schCd
	, String createDt
	, List<SchTypeUseYnResult> versionList
) {

}
