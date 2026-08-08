package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * F-8-2: 본인 기본 근무타입 자기변경 요청(앱 마이페이지).
 * 대상 회사/사용자/사업장은 세션 토큰에서만 도출한다 — 여기서는 값만 받는다.
 */
@Data
public class UpdateDefaultSchRequest {
    private String defaultSchCd;
}
