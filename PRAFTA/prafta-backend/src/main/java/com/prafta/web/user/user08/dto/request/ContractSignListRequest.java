package com.prafta.web.user.user08.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 서명 이력 목록 조회 요청 (User_08 탭2 검색 — 사업장/기간/이름).
 * 회사 스코프는 JWT 클레임(gv_cmpnyCd)으로만 강제한다.
 */
@Getter
@Setter
public class ContractSignListRequest {
    /** 사업장코드(필수 — 사업장 인가 가드 대상). */
    private String siteCd;
    /** 서명일 시작 YYYYMMDD (선택). */
    private String fromDate;
    /** 서명일 종료 YYYYMMDD (선택). */
    private String toDate;
    /** 이름 스냅샷 부분 검색 (선택). */
    private String userNm;
}
