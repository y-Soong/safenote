package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 세션 상태 조회(A5) 결과.
 *
 * <p>관리자 시작/종료 판정 = STATUS_CD(SYS046). startedAt/endedAt 은 표시·감사용 보조.
 * <p>syncStateCd(SYS049 PLAYING/PAUSED)는 슬라이드 동기화 상태이며 시작/종료 판정 기준이 아니다(참고 필드).
 */
@Getter
@Setter
public class TbmSessionStateResult {
    private String statusCd;       // SYS046 OPENED/IN_PROGRESS/COMPLETED
    private String startedAt;      // yyyy-MM-dd HH:mm
    private String endedAt;        // yyyy-MM-dd HH:mm
    private String syncStateCd;    // SYS049 PLAYING/PAUSED (참고 필드)
}
