package com.prafta.app.selfjoin.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 관리자 모드 — 셀프가입 거부 요청.
 *
 * <p>거부 사유 길이 상한(200자)은 웹 {@code User09ServiceImpl.REJECT_REASON_MAX_LEN} 이
 * 단일 출처로 강제한다. 앱에서 다시 정의하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppSelfJoinRejectRequest {

    /** 거부 대상 사용자 코드 (필수). */
    private String userCd;

    /** 거부 사유 (필수, 200자 이내 — 서버 강제). */
    private String rejectReason;
}
