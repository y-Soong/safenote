package com.prafta.web.baim.baim05.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 관리자 QR 발급 전 휴대폰 중복 사전확인 응답 (baim05-qr-phone-precheck).
 *
 * <p>duplicateType:
 * <ul>
 *   <li>ACTIVE — 같은 회사에 해당 휴대폰의 활성 계정 존재(일용직 또는 통합 사용자). 발급 시 서버가 BAIM_400_003 으로 차단.</li>
 *   <li>REACTIVATABLE — 활성 계정은 없으나 비활성 일용직 계정 존재. 발급 시 그 계정이 재활성(재사용)됨 — 관리자 confirm 안내용.</li>
 *   <li>NONE — 중복 없음(신규 발급).</li>
 * </ul>
 * maskedUserNm 은 REACTIVATABLE 일 때만 재활성 대상 계정의 마스킹 이름(예: 홍*동). 그 외 null.
 */
@Value
@Builder
public class CheckDailyUserPhoneResponse {
	String duplicateType;
	String maskedUserNm;
}
