package com.prafta.app.tbm.tbm01.dto.request;

import lombok.Data;

/**
 * prafta-app-tbm: 탭별 세션 리스트(A1/A2/A3) 조회 요청.
 * <p>GET 쿼리스트링 바인딩(@ModelAttribute). tab(AVAILABLE/IN_PROGRESS/COMPLETED)만 받는다.
 * <p>CMPNY_CD/SITE_CD/USER_CD 는 절대 쿼리로 받지 않고 JWT 에서만 도출한다(IDOR 차단).
 */
@Data
public class TbmSessionListRequest {
    private String tab;
}
