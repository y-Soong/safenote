package com.prafta.web.subcon.subcon02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사업장 연동 링크 상태 전이(수락/거부/취소/해지) 요청(PRAFTA-SUBCON-T2 §4).
 *
 * <p>행위자 소속 회사는 클라가 보내지 않는다(서버 JWT 클레임 gv_cmpnyCd 사용).
 * comment 는 거부 시 필수(≤500자), 그 외 액션은 선택.
 */
@Getter
@Setter
@NoArgsConstructor
public class SiteLinkProcessRequest {
    private Long linkId;       // 링크ID(tb_site_link PK)
    private String comment;    // 처리 코멘트(거부 사유 등)
}
