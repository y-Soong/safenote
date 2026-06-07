package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.AttendanceLinkResult;
import com.prafta.web.acct.acct01.result.ScheduleLinkResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 근태 연계 조회 응답.
 * 일용직은 스케줄이 없어 hasSchedule=false + scheduleNote 안내. 실근태가 비어도 정상 상태.
 * occurTime 은 발생시각 마커(FE 타임라인 좌표용).
 */
@Getter
@Builder
public class AttendanceLinkResponse {
    private boolean hasSchedule;
    private String scheduleNote;
    private ScheduleLinkResult schedule;
    private List<AttendanceLinkResult> records;
    private String occurTime;
    private String notice;
}
