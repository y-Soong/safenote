package com.prafta.web.attd.attd13.dto.response;

import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 변경 요청 단건 상세 응답 (PRAFTA-COM-008-C, 확인/반려 팝업용).
 */
@Getter
@Builder
public class ChangeRequestDetailResponse {

    /** 변경 요청 1건(코드값 — 라벨 매핑은 프론트). */
    private final LeaveChangeRequestRowResult detail;
}
