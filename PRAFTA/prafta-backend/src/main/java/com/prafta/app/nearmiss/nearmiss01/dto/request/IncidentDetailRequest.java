package com.prafta.app.nearmiss.nearmiss01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A5 사건 단건 상세 조회 요청.
 * 접근 주체는 관리자(사업장 권한) 또는 본인 보고건(REPORTER_ID=JWT userCd) — 서비스에서 판정(D-A2).
 */
@Getter
@Setter
@NoArgsConstructor
public class IncidentDetailRequest {
    private String nearMissId;
}
