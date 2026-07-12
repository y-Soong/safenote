package com.prafta.app.tbm.tbm01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * [정합성 수정] 본인 출결 상태 조회 응답.
 *
 * <p>GET /appApi/tbm/sessions/{sessionCd}/my-attendance.
 * 스코프는 JWT(userCd)만 신뢰(IDOR 안전). 대기/진행 화면에서 관리자 내보내기(cancel-entry 삭제) /
 * 강제퇴실(force-exit, MANAGER_FORCED) / 본인 완료 여부를 감지하는 데 사용한다.
 *
 * <ul>
 *   <li>present: 본인 출결 행 존재 여부. false 면 관리자 내보내기(입실취소)로 삭제된 상태.</li>
 *   <li>entered: 실입실(ENTRY_AT IS NOT NULL) 여부.</li>
 *   <li>exitAt: 종료/퇴실 시각(yyyy-MM-dd HH:mm:ss). NULL 이면 미종료.</li>
 *   <li>exitTypeCd: SYS052(SELF/MANAGER_FORCED 등). MANAGER_FORCED 면 관리자 강제퇴실.</li>
 *   <li>completionStatusCd: SYS053(COMPLETED/NOT_COMPLETED 또는 NULL=미완료).</li>
 * </ul>
 */
@Getter
@Builder
public class TbmMyAttendanceResponse {
    private final boolean present;
    private final boolean entered;
    private final String exitAt;
    private final String exitTypeCd;
    private final String completionStatusCd;
}
