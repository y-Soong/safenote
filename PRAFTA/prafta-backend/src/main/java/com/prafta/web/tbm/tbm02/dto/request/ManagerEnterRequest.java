package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 직접 입실 요청(prafta-051-11). POST /tbm02/manager-enter.
 *
 * <p>관리자가 후보 검색 결과에서 특정 사용자를 직접 입실 처리한다. GPS/비밀번호 검증 없이
 * ENTRY_TYPE_CD='MANAGER_DIRECT' 로 기록되며, 회사/권한 식별자는 모두 JWT 에서 도출한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ManagerEnterRequest {
	private String sessionCd;	// 대상 세션

	// ===== PRAFTA-SUBCON-T5 M1: 후보 검색 경로(권장) =====
	// 후보 목록 행의 불투명 핸들만 보낸다. 회사코드/사용자코드는 요청에 없다(클라가 알 수 없다).
	private String entryHandle;

	// ===== QR 스캔 경로(일용직) — 핸들이 없는 유일한 경로 =====
	// 물리적 QR 을 스캔해 즉시 입실 처리하므로 후보 목록(=핸들)을 거치지 않는다.
	private String userTypeCd;		// REGULAR | DAILY (핸들 사용 시 무시)
	private String userCd;			// QR 에서 파싱한 사용자코드 (핸들 사용 시 무시)
	// 대상 회사 = 화면에서 고른 개설사/1차 회사(서버가 assertTier1Selectable 로 검증).
	// QR 페이로드의 cmpnyCd 는 targetCmpnyCd 로 쓰지 않는다(체인 멤버십 열거 오라클 차단 — N1).
	private String targetCmpnyCd;
	// QR 페이로드의 회사코드(선택). 서버가 도출한 체인 범위 안에서 <b>동명 USER_CD 를 가르는 힌트로만</b>
	// 사용하며, 범위 밖 값이면 무시된다(신뢰하지 않는다).
	private String qrCmpnyCd;
}
