package com.prafta.common.cmm.leave.feature.dto.response;

import com.prafta.common.cmm.leave.feature.vo.LeaveFeatureVisibilityVO;

import lombok.Builder;
import lombok.Value;

/**
 * 소정-06: 연차 기능 노출 판정 응답.
 *
 * <p>{@code GET /prafta/comApi/leave-feature/visibility}
 *
 * <pre>
 * {
 *   "leaveFeatureVisible": true,      // 화면(연차 카드·신청 진입점) 노출 여부 — FE 는 이 값만 보면 된다
 *   "statutoryAutoGrantYn": "Y",      // 회사 토글 상태(안내 문구 분기용)
 *   "grantHistoryExists": true        // 회사 연차 부여 이력 존재 여부(진단/안내용)
 * }
 * </pre>
 */
@Value
@Builder
public class LeaveFeatureVisibilityResponse {

    /** 연차 기능 노출 여부 (숨김 = 자동 부여 off AND 부여 이력 0) */
    boolean leaveFeatureVisible;

    /** 법정 연차 자동 부여 사용 여부 (Y/N) */
    String statutoryAutoGrantYn;

    /** 회사 연차 부여 이력 존재 여부 */
    boolean grantHistoryExists;

    public static LeaveFeatureVisibilityResponse of(LeaveFeatureVisibilityVO vo) {
        return LeaveFeatureVisibilityResponse.builder()
                .leaveFeatureVisible(vo.isLeaveFeatureVisible())
                .statutoryAutoGrantYn(vo.isStatutoryAutoGrantEnabled() ? "Y" : "N")
                .grantHistoryExists(vo.isGrantHistoryExists())
                .build();
    }
}
