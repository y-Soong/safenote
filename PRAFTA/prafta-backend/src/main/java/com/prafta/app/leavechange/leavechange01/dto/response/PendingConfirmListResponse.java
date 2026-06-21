package com.prafta.app.leavechange.leavechange01.dto.response;

import java.util.List;

import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자(앱) 확인 대기(AGREED) 연차 변경/삭제 요청 목록 응답 (PRAFTA-COM-008-C).
 *
 * <p>근로자가 동의(AGREED)한 뒤 관리자의 최종 확인을 기다리는 요청 목록. 웹 Attd_13 의 앱 미러.
 */
@Getter
@Builder
public class PendingConfirmListResponse {

    /** 관리자 스코프 내 AGREED(확인 대기) 요청 목록. */
    private final List<LeaveChangeRequestRowResult> list;

    /** 총 건수. */
    private final int totalCnt;
}
