package com.prafta.web.tbm.tbm04.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * TBM 증빙 교육일지(건별) 상세 조회 요청 — 체크된 세션코드 목록(청크 최대 50건).
 *
 * <p>sessionCd 는 리소스 키일 뿐 인가 근거가 아니다 — 서버가 "자사 개설 세션(스코프 내)
 * 또는 자사 근로자 참석 세션"만 남기고 나머지는 조용히 제외한다(IDOR 차단).
 */
@Getter
@Setter
public class EvidenceSessionDetailRequest {
    private List<String> sessionCds;
}
