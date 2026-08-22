package com.prafta.web.attd.attd09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 입사일 기준 차액 조회 목록 요청 (경력인정 이원화 Phase 2 §2-2).
 * GET /attd09/leave-dashboard/shortfall/list?siteCd=&nodeCd=&incSubNodeYn=&userNm=&baseYmd=&page=&size=.
 */
@Getter
@Setter
@NoArgsConstructor
public class ShortfallListRequest {

    /** 사업장 코드 필터 (빈값=전체) */
    private String siteCd;

    /** 소속부서 노드 코드 필터 (빈값=전체) */
    private String nodeCd;

    /** 하위부서 포함 여부 Y/N (기본 N) */
    private String incSubNodeYn;

    /** 사용자명 검색어 (빈값=전체) */
    private String userNm;

    /** 조회 기준일 (YYYYMMDD, 필수) — 퇴사(예정)일 입력 시 퇴직정산 참고 조회 */
    private String baseYmd;

    /** 1-based 페이지 번호 (기본 1) */
    private Integer page;

    /** 페이지 크기 (1~100, 기본 20) */
    private Integer size;
}
