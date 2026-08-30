package com.prafta.web.tbm.tbm04.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * TBM 증빙자료(반기) 조회 요청 (Tbm_04 "TBM 증빙자료 출력" 팝업).
 *
 * <p>세션 목록·근로자별 이수현황 조회가 동일 조건을 공유한다.
 */
@Getter
@Setter
public class EvidenceSessionListRequest {
    /** 대상 년도(YYYY). */
    private String year;
    /** 반기: H1(1/1~6/30) | H2(7/1~12/31). */
    private String half;
    /** 자사 사업장 필터(선택 — 빈값=전체). 공유 세션에는 미적용(권장안). */
    private String siteCd;
}
