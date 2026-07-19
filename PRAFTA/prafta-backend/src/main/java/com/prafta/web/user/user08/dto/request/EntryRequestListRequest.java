package com.prafta.web.user.user08.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 입장 승인요청 목록 조회 요청 (User_08 탭1 검색 — 사업장/상태/유형/요청일).
 * 회사 스코프는 JWT 클레임(gv_cmpnyCd)으로만 강제한다.
 */
@Getter
@Setter
public class EntryRequestListRequest {
    /** 사업장코드(필수 — 사업장 인가 가드 대상). */
    private String siteCd;
    /** 요청상태 필터 [SYS082] (선택). */
    private String reqStatus;
    /** 요청유형 필터 [SYS081] (선택). */
    private String reqType;
    /** 요청일 필터 YYYYMMDD (선택 — 화면 기본값 오늘). */
    private String reqDate;
}
