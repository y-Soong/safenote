package com.prafta.app.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A6 1차 확인 상태전환 요청 (json).
 *
 * <p>앱 관리자 조치 범위 한정(plan §4.5): 정방향 전이는 100->200 만 허용(웹이 200->300->400 담당).
 *    반려(900)는 사유 필수. adminTempActionDesc 는 전이와 함께 ADMIN_TEMP_ACTION_DESC 에 기록.
 * <p>cmpnyCd/userCd/siteCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangeStatusRequest {
    private String nearMissId;
    private String reportStatusCd;      // '200'(검토중) 또는 '900'(반려)
    private String adminTempActionDesc; // 임시조치 메모(선택, 200 전환 시)
    private String rejectReason;        // 반려 사유(900 시 필수)
}
