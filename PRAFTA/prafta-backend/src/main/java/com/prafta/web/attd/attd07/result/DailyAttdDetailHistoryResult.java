package com.prafta.web.attd.attd07.result;

public record DailyAttdDetailHistoryResult(
	String cmpnyCd
	, String histId
	, String attdId
	, String histType
	, String histTypeNm
	, String workSeq
	
	, String befCheckInDate
	, String befCheckInTime
	, String befCheckOutDate
	, String befCheckOutTime

	, String aftCheckInDate
	, String aftCheckInTime
	, String aftCheckOutDate
	, String aftCheckOutTime

	, String processReason
	, String insertNo
	, String insertNm
	, String insertDate

	// ────────────────────────────────────────────────────────────────
	// PRAFTA-APP-007-WEB-6 + D15: 스케줄 수정(10) 처리 이력의 "변경 전→후 스케줄" 원시 시각.
	//   이 record 는 3개 쿼리가 공유한다(selectDailyAttdDetailHistory / selectDailyLeaveApprovalHistory /
	//   신규 selectDailySchedModifyHistory). ⚠️ MyBatis 는 컬럼을 "순서(위치)"로 생성자 인자에 바인딩하므로
	//   아래 8필드는 반드시 record 의 "맨 끝"에 있어야 하고, 세 쿼리 SELECT 모두 동일 위치(맨 끝)에
	//   같은 순서로 컬럼을 둔다(기존 두 쿼리는 해당 자리에 NULL).
	//   라벨은 프론트가 schedLabel 로 조립하므로 원시 HHmm 문자열만 내린다.
	//   스케줄 수정 승인 행만 채워지고(반려/근태/OT/연차 이력은 전부 NULL → 프론트 "시각 이력 아님" 분기).
	// ────────────────────────────────────────────────────────────────

	/** 변경 전 스케줄 1구간 시작 시각 (HHmm) — 스케줄 수정(10) 승인 이력만 채움 */
	, String befSchedFstStrTime
	/** 변경 전 스케줄 1구간 종료 시각 (HHmm) */
	, String befSchedFstEndTime
	/** 변경 전 스케줄 2구간 시작 시각 (HHmm, 1구간/없음이면 NULL) */
	, String befSchedSecStrTime
	/** 변경 전 스케줄 2구간 종료 시각 (HHmm) */
	, String befSchedSecEndTime

	/** 변경 후 스케줄 1구간 시작 시각 (HHmm) — 스케줄 수정(10) 승인 이력만 채움 */
	, String aftSchedFstStrTime
	/** 변경 후 스케줄 1구간 종료 시각 (HHmm) */
	, String aftSchedFstEndTime
	/** 변경 후 스케줄 2구간 시작 시각 (HHmm, 1구간/없음이면 NULL) */
	, String aftSchedSecStrTime
	/** 변경 후 스케줄 2구간 종료 시각 (HHmm) */
	, String aftSchedSecEndTime

	// ────────────────────────────────────────────────────────────────
	// com-013 #3+#4: 처리 이력 컬럼 확장. 위 8필드와 동일하게 record 의 "맨 끝"에 둔다
	//   (MyBatis 위치 기반 매핑 — 세 쿼리 SELECT 모두 동일 순서·위치). 미해당 쿼리는 NULL alias.
	// ────────────────────────────────────────────────────────────────

	/** 요청 유형(SYS032 코드). 스케줄 수정 이력은 '10', 그 외(근태/OT/연차)는 NULL.
	 *  프론트가 스케줄 이력(승인+반려)을 일반화 식별하는 데 사용한다(com-013 #3). */
	, String reqType

	/** 근로자가 요청 시 입력한 사유(TB_USER_ATTD_REQ.REQ_REASON). REQ 연결이 없는
	 *  관리자 직접수정/orphan 이력은 NULL. 관리자 처리사유(processReason)와는 별개 컬럼(com-013 #4). */
	, String reqReason
) {
}
