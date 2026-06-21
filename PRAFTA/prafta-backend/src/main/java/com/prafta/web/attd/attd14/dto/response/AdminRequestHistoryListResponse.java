package com.prafta.web.attd.attd14.dto.response;

import java.util.List;

import com.prafta.web.attd.attd14.result.AdminRequestHistoryRowResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 발신 연차 변경 요청 이력 목록 응답 (prafta-com-016-H).
 */
@Getter
@Builder
public class AdminRequestHistoryListResponse {

    /** 요청 이력 목록(현재 페이지). */
    private final List<AdminRequestHistoryRowResult> list;

    /** 검색 조건 총 건수(페이징 계산용). */
    private final int totalCnt;
}
