package com.prafta.web.attd.attd05.result;

/**
 * prafta-com-008-D-5: Attd_05 그리드 교대 잠금 오버레이 단건.
 *
 * <p>교대팀 소속 구간(잠금)인 (userCd, workYmd) 셀을 프론트가 비활성/자물쇠 표시하도록 펼쳐 반환한다.
 * 판정은 D-1 경계 술어(팀 STR_DATE inclusive ~ END_DATE inclusive ∩ 멤버십 행 존재 ∩ LEAVE_TEAM_YMD 미포함)를
 * 그리드 조회월 범위로 확장한 단일 출처를 사용한다(연차 오버레이 selectLeaveOverlayList 스코프 미러).
 *
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record ShiftLockOverlayResult(
        String userCd
        , String workYmd
) {
}
