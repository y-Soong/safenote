package com.prafta.app.leave.leaveflow.dto.response;

import java.util.List;

/**
 * prafta-app-018-A: 연차 신청 폼 메타 응답 (GET /appApi/leaveflow/apply-meta).
 *
 * <p>키명은 018-C(FE)가 그대로 소비하므로 임의 변경 금지. record 사용으로 boolean is- 접두 탈락 이슈 없음.
 */
public record LeaveApplyMetaResponse(
      List<LeaveTypeItem> leaveTypes
) {
    /**
     * 신청 가능 연차종류 1건.
     *
     * <ul>
     *   <li>{@code systemYn} : 법정여부 'Y'/'N'(원본 문자열).</li>
     *   <li>{@code aprvRequired} : 결재필요(법정=policy.APRV_USE_YN / 비법정=type.APRV_USE_YN, 'Y'→true).</li>
     *   <li>{@code allowedUnits} : 허용 사용단위 SYS025 코드 목록(D2-a 계층, 굵→잘게: 00,01,02,03,04 부분집합).</li>
     *   <li>{@code balanceDays} : 현재 잔여(부여-사용 합, 활성집합, 소수1자리).</li>
     *   <li>{@code applicable} : 신청가능(잔여>0). false 면 FE disabled.</li>
     * </ul>
     */
    public record LeaveTypeItem(
          String leaveCd
        , String leaveNm
        , String systemYn
        , boolean aprvRequired
        , List<String> allowedUnits
        , double balanceDays
        , boolean applicable
    ) {
    }
}
