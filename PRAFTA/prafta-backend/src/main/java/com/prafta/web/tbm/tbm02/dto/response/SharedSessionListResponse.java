package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.SharedSessionResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동받은 교육 목록 응답(PRAFTA-SUBCON-T5 D2).
 *
 * <p>비개설사(지정받은 회사) 전용 목록. 재지정(체인 관리) 진입점 제공이 목적이며, 세션 상세/콘솔/
 * 참석자 명단으로는 진입할 수 없다(개설사 전용 게이트 loadGuard 는 그대로 유지).
 *
 * <p>회사명은 <b>나를 지정한 회사</b>만 노출한다(개설사 비노출 — 마스터 §1-3).
 */
@Getter
@Builder
public class SharedSessionListResponse {
	private List<SharedSessionResult> sessionList;
	private int totalCount;
	private int page;
	private int pageSize;
}
