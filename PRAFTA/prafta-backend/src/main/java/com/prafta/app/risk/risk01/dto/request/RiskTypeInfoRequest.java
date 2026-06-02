package com.prafta.app.risk.risk01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-036-B2: 위험성평가 구분/분류/발생상황 조회 요청.
 * <p>@ModelAttribute 바인딩을 위해 표준 POJO(getter/setter/기본생성자) 형태.
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskTypeInfoRequest {
    private String siteCd;
}
