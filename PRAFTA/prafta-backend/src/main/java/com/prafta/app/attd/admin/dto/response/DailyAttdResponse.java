package com.prafta.app.attd.admin.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * J1-5: 일자 근태 현황 응답.
 *
 * <p>직원별 출/퇴근/지각/조퇴/외근 요약 리스트(노드 스코프). 시각은 HHMM 문자열/null.
 * PII(휴대폰/이메일) 미포함 — 이름·노드명만.
 */
@Getter
@Builder
public class DailyAttdResponse {

    private final List<DailyItem> items;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class DailyItem {
        private final String userCd;
        private final String userNm;
        private final String nodeNm;
        /** 출근 HHMM(첫 차수). 미출근이면 null. */
        private final String checkInTime;
        /** 퇴근 HHMM(마지막 차수). 미퇴근이면 null. */
        private final String checkOutTime;
        // Lombok+Jackson 의 is-접두 직렬화 함정 방지: @JsonProperty 로 키를 isXxx 로 고정한다.
        //   (미지정 시 isLate→"late" 로 떨어져 프론트(item.isLate)와 계약 불일치 — 메모리 feedback_lombok_jackson_boolean_is_prefix)
        /** 지각 여부(어느 차수든 지각이면 true) — raw 일시 stamp 판정. */
        @JsonProperty("isLate")
        private final boolean isLate;
        /** 조퇴 여부(어느 차수든 조퇴면 true) — raw 일시 stamp 판정(자정 넘김 보정). */
        @JsonProperty("isEarly")
        private final boolean isEarly;
        /** 외근 여부(해당 일자 GPS 행 존재). */
        @JsonProperty("isOffsite")
        private final boolean isOffsite;
        /** 근무 분(휴게 제외, 차수 합산, 음수 0). */
        private final long workMinutes;
        /** 출근 차수 수(1 또는 2). */
        private final int slotCount;
        /**
         * PRAFTA-FIXEDOT-3(정책 ①): 그날 고정연장 실적 분(실근태 ∩ 고정연장 — 파생 계산, 저장 없음).
         * 고정연장 없는 근무타입은 0.
         */
        private final long fixedOtMinutes;
        /**
         * PRAFTA-FIXEDOT-3(정책 ②③): "연장 미이행" 배지 — 후방 고정연장을 다 채우지 못한 퇴근.
         * 조퇴(isEarly)와 완전 분리된 별도 표식이며, 연차 계열 사용일·미퇴근에는 발화하지 않는다.
         */
        @JsonProperty("isFixedOtUnmet")
        private final boolean isFixedOtUnmet;
    }
}
