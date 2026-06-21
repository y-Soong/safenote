package com.prafta.web.attd.attd06.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

/**
 * 교대 근무계획 자동 생성/재생성 결과 응답.
 *   blockedList: 덮어쓰기에서 제외(보존)된 (사용자, 날짜) 목록.
 *
 * <p>prafta-com-016-D-0 + 공통 가드①: 보존 판정을 공통 {@code ScheduleChangeGuardService} 기반으로
 *   일반화한다. 종일/반차/시간차 연차(USE_UNIT_TYPE 무관) + OT 가 있는 (사용자, 날짜)는 근로일/휴무일을
 *   불문하고 덮어쓰기에서 제외하고 기존 스케줄·연차를 유지한다. 제외된 항목은 016-D-4 안내 팝업에서
 *   필터(전체/연차-근로일/연차-휴무일/초과근무)·사용단위와 함께 표시하기 위해
 *   {@code reason}(LEAVE/OT), {@code dayType}(WORK/OFF), {@code leaveUseUnitType}(연차 사용단위) 를 함께 담는다.
 */
@Value
@Builder
public class ShiftSchSaveResponse {
    List<BlockedWorkPlan> blockedList;

    @Value
    @Builder
    public static class BlockedWorkPlan {
        String userCd;
        String workYmd;

        /** 보존 사유: "LEAVE"(연차) / "OT"(초과근무). */
        String reason;

        /**
         * 그 날의 교대 패턴상 근로일/휴무일 구분: "WORK"(근로일=schCd 있음) / "OFF"(휴무일=schCd null).
         * 016-D-4 팝업의 "연차-근로일 / 연차-휴무일" 필터에 사용.
         */
        String dayType;

        /**
         * 연차 보존일 때의 사용단위 코드(USE_UNIT_TYPE — 종일 '00' / 반차 / 시간차 등). OT 보존이면 null.
         * 016-D-4 팝업에서 "연차 2시간차 ..." 처럼 사용단위 표시에 사용.
         */
        String leaveUseUnitType;
    }
}
