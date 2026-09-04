package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * BW-12(§7-1, 2026-09-04): 휴게 미이용 <b>상시</b> 요청 저장 요청(앱 마이페이지).
 *
 * <p>근기법 제54조① 단서 — 소정근로가 4시간(240분)이고 휴게가 없는 단시간 근로자가
 * "휴게를 이용하지 않겠다"는 의사를 상시로 표시한다.
 *
 * <p>대상 회사/사용자는 세션 토큰에서만 도출한다(관리자 대리 불가) — 여기서는 값만 받는다.
 * 값 검증(Y/N)은 Service 가 수행한다(ATTD_400_220).
 */
@Data
public class BrkWaiveStandingRequest {

    /** 'Y'(상시 요청) / 'N'(요청 해제). 그 외 값은 거부. */
    private String standingYn;
}
