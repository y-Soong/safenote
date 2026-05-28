package com.prafta.web.baim.baim07.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 정책 변경 이력 페이징 조회 요청.
 * GET /baim07/policy/history?page=1&size=20.
 */
@Getter
@Setter
@NoArgsConstructor
public class LeavePolicyHistoryListRequest {

    /** 1-based 페이지 번호 (기본 1) */
    private Integer page;

    /** 페이지 크기 (1~100, 기본 20) */
    private Integer size;
}
