package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminAttendeeResult;

import lombok.Builder;
import lombok.Getter;

/** R3 출결 리스트 응답(진행 LIVE / 종료 COMPLETED). */
@Getter
@Builder
public class AdminAttendeeListResponse {
    private List<AdminAttendeeResult> attendees;
    private int totalCount;
}
