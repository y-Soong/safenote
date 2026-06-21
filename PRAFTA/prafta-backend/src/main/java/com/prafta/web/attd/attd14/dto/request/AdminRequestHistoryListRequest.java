package com.prafta.web.attd.attd14.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 발신 연차 변경 요청 이력 목록 조회 검색 조건 (prafta-com-016-H, GET /webApi/attd14/admin-requests).
 *
 * <p>식별값(회사)은 토큰에서 강제하며, 본 DTO 는 검색 필터/페이징만 받는다.
 * siteCd 는 토큰 사업장과 일치해야 한다(Param 에서 IDOR 가드, attd13 계승).
 * INITIATOR_TYPE 은 서버에서 'ADMIN' 고정(클라가 보내지 않는다).
 */
@Getter
@Setter
public class AdminRequestHistoryListRequest {

    /** 사업장 코드 (토큰 사업장과 일치 강제). */
    private String SITE_CD;

    /** 소속부서 노드 코드 필터 (NULL/빈값이면 전체). */
    private String NODE_CD;

    /** 하위부서 포함 여부 'Y'/'N'. */
    private String INC_SUB_NODE_YN;

    /** 대상 사용자명 LIKE 검색어 (NULL/빈값이면 전체). */
    private String USER_NM;

    /** 요청 유형 필터[SYS071] MOVE/DELETE (NULL/빈값이면 전체). */
    private String REQ_TYPE;

    /** 요청 상태 필터[SYS072] (NULL/빈값이면 전체). */
    private String REQ_STATUS;

    /** 발의 기간 시작(YYYYMMDD, INSERT_DATE 기준). NULL/빈값이면 하한 없음. */
    private String FROM_DATE;

    /** 발의 기간 종료(YYYYMMDD, INSERT_DATE 기준). NULL/빈값이면 상한 없음. */
    private String TO_DATE;

    /** 페이지 번호(1-base). */
    private Integer PAGE;

    /** 페이지 크기. */
    private Integer PAGE_SIZE;
}
