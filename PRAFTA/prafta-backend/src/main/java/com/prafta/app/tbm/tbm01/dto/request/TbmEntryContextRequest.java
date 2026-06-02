package com.prafta.app.tbm.tbm01.dto.request;

import lombok.Data;

/**
 * prafta-app-004-C3: TBM 입실 컨텍스트 조회 요청.
 * <p>GET 쿼리스트링 바인딩(@ModelAttribute). sessionCd 만 받는다.
 * <p>USER_CD/CMPNY_CD/SITE_CD 는 절대 바디로 받지 않고 JWT(TokenInfo)에서만 도출한다(IDOR 차단).
 */
@Data
public class TbmEntryContextRequest {
    private String sessionCd;
}
