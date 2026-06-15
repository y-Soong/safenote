package com.prafta.web.attd.attd13.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 변경 요청 목록 조회 검색 조건 (PRAFTA-COM-008-C, GET /webApi/attd13/change-requests).
 *
 * <p>식별값(회사)은 토큰에서 강제하며, 본 DTO 는 검색 필터만 받는다.
 * siteCd 는 토큰 사업장과 일치해야 한다(Param 에서 IDOR 가드).
 */
@Getter
@Setter
public class ChangeRequestListRequest {

    /** 사업장 코드 (토큰 사업장과 일치 강제). */
    private String SITE_CD;

    /** 소속부서 노드 코드 필터 (NULL/빈값이면 전체). */
    private String NODE_CD;

    /** 하위부서 포함 여부 'Y'/'N'. */
    private String INC_SUB_NODE_YN;

    /** 사용자명 LIKE 검색어 (NULL/빈값이면 전체). */
    private String USER_NM;

    /** 요청 상태 필터[SYS072] (NULL/빈값이면 전체). */
    private String REQ_STATUS;
}
