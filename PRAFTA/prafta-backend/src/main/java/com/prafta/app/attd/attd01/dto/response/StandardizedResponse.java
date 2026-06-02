package com.prafta.app.attd.attd01.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 슬롯(구간)별 표준화 정산 시간 응답 (계약 §3.1 slots[].standardized).
 *
 * <p>출퇴근이 모두 등록된 경우에만 산출(attd §10.2). 출퇴근 미완료(미등록)면 슬롯의 standardized 는 null.
 *
 * <p>표준화 방향(회사 유리): 출근은 올림(ceil — 더 늦은 경계), 퇴근은 내림(floor — 더 이른 경계).
 *   이렇게 적용했을 때 표준 출근/퇴근의 시간 순서가 원본 일자경계 방향과 모순되면(예: 출퇴근 시각이
 *   같거나 매우 짧아 표준 출근 &ge; 표준 퇴근) 표준화를 적용하지 않는다 → {@code applied=false},
 *   이때 startTime/endTime/settledMinutes 는 null(프론트는 원본 근태값을 그대로 표시).
 *
 * <ul>
 *   <li>applied=true: 표준화 적용. startTime/endTime(HHMM), settledMinutes(근무분) 채워짐.</li>
 *   <li>applied=false: 조건 미충족으로 표준화 미적용. 나머지 필드 null.</li>
 * </ul>
 */
@Getter
@Builder
public class StandardizedResponse {
    /** 표준화 적용 여부(false=조건 미충족으로 미적용). */
    @JsonProperty("applied")
    private final boolean applied;
    private final String startTime;       // HHMM (표준화 적용, applied=false 면 null)
    private final String endTime;         // HHMM (표준화 적용, applied=false 면 null)
    private final Integer settledMinutes; // 근무분 (applied=false 면 null)
}
