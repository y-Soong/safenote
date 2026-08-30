package com.prafta.app.siteops.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 응답.
 *
 * <p>PII 평문 금지: 이름은 마스킹된 값(userNmMasked)만 내려준다. 휴대폰(MBL_NO/_ENC/_LAST4)은
 * 응답에 포함하지 않는다. 현장 토스트에는 마스킹 이름 + 처리시각(HHMM)만으로 충분하다.
 */
@Getter
@Builder
public class SiteOpsAttendanceResponse {

    /**
     * 처리 결과: CHECKED_IN(출근) / CHECKED_OUT(퇴근) /
     * SIGN_REQUIRED(현장 계약서 서명 필요 — 출근 미등록, 서명 완료 후 재요청).
     */
    private final String result;

    /** 대상 일용직 USER_CD. */
    private final String userCd;

    /** 마스킹된 일용직 이름(예: 홍*동). */
    private final String userNmMasked;

    /** 처리 시각 HHMM(출근=출근시각 / 퇴근=퇴근시각). 서버 NOW raw(표준화 미적용). */
    private final String processedTime;

    /** SIGN_REQUIRED 전용: 서명 대상 계약서 버전(그 외 결과에서는 null). */
    private final Integer contractVer;

    /** SIGN_REQUIRED 전용: 서명 대상 계약서명(그 외 결과에서는 null). */
    private final String contractNm;
}
