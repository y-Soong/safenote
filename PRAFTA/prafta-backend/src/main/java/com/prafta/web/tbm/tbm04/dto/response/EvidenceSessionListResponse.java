package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.EvidenceSessionResult;

import lombok.Builder;
import lombok.Value;

/** TBM 증빙 반기 세션 목록 응답 (GET /webApi/tbm04/evidence-sessions). */
@Value
@Builder
public class EvidenceSessionListResponse {
    /** 자사 개설(기간·스코프·사업장 필터) + 공유 세션(자사 참석분, 사업장 필터 미적용). */
    List<EvidenceSessionResult> sessionList;
    /** 대상 기간 시작/종료(YYYY-MM-DD) — 엑셀 머리말 표기용(서버 파생 단일 출처). */
    String fromDate;
    String toDate;
    /** 자사 회사명(TB_CMPNY) — 엑셀 머리말 표기용(웹 세션에 회사명 미보유). */
    String cmpnyNm;
}
