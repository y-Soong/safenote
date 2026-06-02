package com.prafta.app.attd.attd01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-002: 이번주 조회 요청.
 * <p>@ModelAttribute 바인딩용 POJO. weekStartYmd 만 받는다(USER_CD 는 JWT, IDOR 가드).
 */
@Getter
@Setter
@NoArgsConstructor
public class WeekRequest {
    private String weekStartYmd; // YYYYMMDD
}
