package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * BW-12(§7-1, 2026-09-04): 휴게 미이용 <b>상시</b> 요청 현행값 응답(앱 마이페이지).
 *
 * <p>조회(GET)·저장(PUT) 이 같은 응답을 쓴다 — 저장 후 화면이 재조회 없이 상태·시각을 갱신한다.
 */
@Getter
@Builder
public class BrkWaiveStandingResponse {

    /** 현행값 'Y'(상시 요청) / 'N'(요청 없음). NULL 컬럼은 'N' 으로 정규화해 내린다. */
    private final String standingYn;

    /** 최근 변경 시각(서버 포맷 'yyyy-MM-dd HH:mm'). 한 번도 바꾼 적 없으면 null. */
    private final String standingDtime;

    /**
     * 노출 조건(plan §7 Q-8): 정규직(EMPLOYMENT_TYPE='REGULAR') 이고 기본 근무타입의 소정근로가
     * 정확히 240분·휴게 0 이면 'Y'. 'N' 이면 앱이 행 자체를 노출하지 않는다.
     *
     * <p>안내/노출 조건일 뿐 저장 게이트가 아니다 — 저장 게이트는 서버가 별도로 강제한다(DAILY 거부 등).
     */
    private final String eligibleYn;
}
