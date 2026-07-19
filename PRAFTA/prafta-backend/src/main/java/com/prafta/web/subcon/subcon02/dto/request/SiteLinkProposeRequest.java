package com.prafta.web.subcon.subcon02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사업장 연동 제안 요청(PRAFTA-SUBCON-T2 §5-3).
 *
 * <p>제안측 회사(SRC_CMPNY_CD)는 클라가 보내지 않는다(서버 JWT 클레임 gv_cmpnyCd 사용).
 * siteCd 는 제안측 소유 사업장(원본 또는 받은 미러 — n차 체인), tgtCmpnyCd 는 관계 ACCEPTED 상대 회사.
 */
@Getter
@Setter
@NoArgsConstructor
public class SiteLinkProposeRequest {
    private String tgtCmpnyCd;   // 수신측 회사코드(관계 ACCEPTED 전제 — 서버 재검증)
    private String siteCd;       // 제안 사업장코드(제안측 소유 — 서버 검증)
}
