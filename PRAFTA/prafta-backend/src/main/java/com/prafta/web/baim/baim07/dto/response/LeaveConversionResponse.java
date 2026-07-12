package com.prafta.web.baim.baim07.dto.response;

import java.util.List;

import com.prafta.common.cmm.leave.vo.LeaveConversionPolicyVO;

import lombok.Builder;
import lombok.Value;

/**
 * 시간차 1일 환산시간 조회 응답 (LC-02). baim07 GET /conversion.
 *
 * <p>현재 적용값(오늘 기준 유효 행, 미설정이면 기본 480) + 변경 이력을 함께 싣는다.
 */
@Value
@Builder
public class LeaveConversionResponse {

    /** 오늘 기준 유효 환산시간(분). 설정 미존재 회사는 기본 480. */
    int currentConvMinutes;

    /** 현재 적용값의 적용 시작일(YYYYMMDD). 설정 미존재(기본 480 적용 중)면 null. */
    String currentApplyFromDate;

    /** 변경 이력(적용일 내림차순 — 미래 예약분 포함). */
    List<LeaveConversionPolicyVO> history;
}
