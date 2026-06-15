package com.prafta.common.cmm.login.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRAFTA-COM-008-E-8 — 기본 근무타입 게이트 저장 요청.
 *
 * <p>식별값(cmpnyCd/userCd)은 scope=DEFAULT_SCH 임시 토큰 claim 에서만 도출한다(IDOR 방지).
 * 본 요청에는 선택한 근무타입(defaultSchCd)만 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetDefaultSchRequest {

    /** 선택한 기본 근무타입(tb_sch_mgmt.SCH_CD). 서버에서 사업장 스코프 + USE_YN='Y' 화이트리스트 검증. */
    private String defaultSchCd;
}
