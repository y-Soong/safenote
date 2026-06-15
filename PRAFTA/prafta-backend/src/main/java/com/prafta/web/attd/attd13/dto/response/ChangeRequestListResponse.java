package com.prafta.web.attd.attd13.dto.response;

import java.util.List;

import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 변경 요청 목록 응답 (PRAFTA-COM-008-C).
 */
@Getter
@Builder
public class ChangeRequestListResponse {

    /** 변경 요청 목록. */
    private final List<LeaveChangeRequestRowResult> list;

    /** 총 건수. */
    private final int totalCnt;
}
