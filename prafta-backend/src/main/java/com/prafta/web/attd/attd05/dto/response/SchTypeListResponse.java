package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.SchTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeValidMeta;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchTypeListResponse {
	List<SchTypeResult> schTypeResultList;

	/**
	 * 근무타입(SCH_CD)별 검증 메타 목록.
	 * 프론트가 셀(날짜)×근무타입 지정 가능 여부를 클라이언트에서 판정하는 데 사용한다.
	 */
	List<SchTypeValidMeta> schTypeValidMetaList;
}
