package com.prafta.app.req.req07.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-007: 근태 요청 폼 3종 (스케줄 수정 / 근태 보정 / 초과근무) 의 공통 구간 입력.
 *
 * <p>3 endpoint 가 동일 SlotRequest 를 공유한다. 각 endpoint 가 사용하지 않는 필드는 무시한다.
 * <ul>
 *   <li>스케줄 수정: workSeq + schCd 사용. 시각 필드 무시.</li>
 *   <li>근태 보정: workSeq + startDate/startTime/endDate/endTime 사용. schCd 무시.</li>
 *   <li>초과근무: workSeq + startDate/startTime/endDate/endTime 사용. schCd 무시.</li>
 * </ul>
 *
 * <p>prafta-043: 초과근무 유형(OT_TYPE) 전면 파기로 otType 필드 제거.
 *
 * <p>시각 형식 (P15):
 * <ul>
 *   <li>startTime/endTime: HHmm 4자리 (예: "0900", "1830"). 콜론 없음.</li>
 *   <li>startDate/endDate: YYYYMMDD 8자리. 자정 넘김 시 endDate = workYmd + 1 일.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class SlotRequest {

    /** 근무 순번 (1 또는 2). 단일 요청 내 1·2 중복 금지. */
    private Integer workSeq;

    /** 변경 목표 스케줄 코드 (스케줄 수정 전용). */
    private String schCd;

    /** 시작 일자 (YYYYMMDD). 근태 보정 / 초과근무 전용. */
    private String startDate;

    /** 시작 시각 (HHmm). 근태 보정 / 초과근무 전용. */
    private String startTime;

    /** 종료 일자 (YYYYMMDD). 근태 보정 / 초과근무 전용. */
    private String endDate;

    /** 종료 시각 (HHmm). 근태 보정 / 초과근무 전용. */
    private String endTime;
}
