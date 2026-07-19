package com.prafta.web.subcon.subcon02.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 순회점검 구성 연동 실행/해제 요청(PRAFTA-SUBCON-T6-02).
 *
 * <p>회사 스코프는 JWT 클레임으로만 강제한다(클라 바디의 회사코드 불신) — linkId 만 수신한다.
 */
@Getter
@Setter
public class ChkptLinkRequest {

    /** 대상 사업장 연동 링크 ID(TB_SITE_LINK.LINK_ID). */
    private Long linkId;
}
