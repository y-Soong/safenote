package com.prafta.web.user.user07.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 계약서 사용중지 요청 (POST /webApi/user07/contract-stop).
 * 회사 스코프는 JWT 클레임(gv_cmpnyCd)으로만 강제한다.
 */
@Getter
@Setter
public class ContractStopRequest {
    /** 사업장코드(필수 — 사업장 인가 가드 대상). */
    private String siteCd;
}
