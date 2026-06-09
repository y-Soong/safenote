package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 교육준비(OPENED) 전이 요청 바디(prafta-051 R-A, E2).
 *
 * <p>관리자 현재 좌표만 받는다. GPS_VERIFY_TYPE_CD='AUTO' 세션이면 서버가 좌표 필수를 강제한다.
 * 그 외 식별자/세션상태는 JWT/세션에서 도출한다(IDOR 차단).
 */
@Getter
@Setter
public class AdminSessionPrepareRequest {
    private String managerGpsLat;
    private String managerGpsLon;
}
