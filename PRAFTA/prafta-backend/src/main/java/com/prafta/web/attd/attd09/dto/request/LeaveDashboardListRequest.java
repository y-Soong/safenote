package com.prafta.web.attd.attd09.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연차 현황 대시보드 목록 조회 요청.
 * GET /attd09/leave-dashboard/list?siteCd=&nodeCd=&incSubNodeYn=&userNm=&page=&size=.
 */
@Getter
@Setter
@NoArgsConstructor
public class LeaveDashboardListRequest {

    /** 사업장 코드 필터 (빈값=전체) */
    private String siteCd;

    /** 소속부서 노드 코드 필터 (빈값=전체) */
    private String nodeCd;

    /** 하위부서 포함 여부 Y/N (기본 N). Y면 nodeCd 서브트리 포함 (attd08 패턴) */
    private String incSubNodeYn;

    /** 사용자명 검색어 (빈값=전체) */
    private String userNm;

    /** 1-based 페이지 번호 (기본 1) */
    private Integer page;

    /** 페이지 크기 (1~100, 기본 20) */
    private Integer size;
}
