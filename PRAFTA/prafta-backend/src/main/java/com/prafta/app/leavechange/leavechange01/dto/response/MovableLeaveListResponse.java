package com.prafta.app.leavechange.leavechange01.dto.response;

import java.util.List;

import com.prafta.web.attd.attd13.result.MovableLeaveResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 근로자 이동 가능 연차일 목록 응답 (PRAFTA-COM-008-C, C-5a).
 */
@Getter
@Builder
public class MovableLeaveListResponse {

    /** 이동 가능 연차일 목록. */
    private final List<MovableLeaveResult> list;

    /** 총 건수. */
    private final int totalCnt;
}
