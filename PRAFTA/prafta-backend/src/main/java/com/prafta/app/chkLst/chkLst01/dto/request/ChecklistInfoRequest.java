package com.prafta.app.chkLst.chkLst01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-036-B1: 체크리스트 정보 조회 요청.
 * <p>@ModelAttribute 바인딩을 위해 표준 POJO(getter/setter/기본생성자) 형태.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChecklistInfoRequest {
    private String cmpnyCd;
    private String siteCd;
    private String chkptCd;
    private String chkptNm;
}
