package com.prafta.app.attd.attd01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-app-002: 일자 상세 조회 요청.
 * <p>@ModelAttribute 바인딩용 POJO. workYmd 만 받는다(USER_CD 는 JWT, IDOR 가드).
 */
@Getter
@Setter
@NoArgsConstructor
public class DayDetailRequest {
    private String workYmd; // YYYYMMDD
}
