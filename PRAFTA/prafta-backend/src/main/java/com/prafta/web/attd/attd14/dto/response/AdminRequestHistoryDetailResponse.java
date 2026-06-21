package com.prafta.web.attd.attd14.dto.response;

import com.prafta.web.attd.attd14.result.AdminRequestHistoryRowResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 발신 연차 변경 요청 이력 단건 상세 응답 (prafta-com-016-H, 읽기 전용).
 */
@Getter
@Builder
public class AdminRequestHistoryDetailResponse {

    /** 요청 1건(코드값 — 라벨 매핑은 프론트). */
    private final AdminRequestHistoryRowResult detail;
}
