package com.prafta.web.tbm.tbmai02.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * TBM AI 교육안 생성 요청.
 *
 * <p>회사 스코프(CMPNY_CD)·사용자·권한은 JWT 클레임에서만 도출하고(IDOR 차단),
 *    세션 식별자(sessionCd)만 바디로 받는다.
 * <p>{@code adminContentText} 는 관리자 교육내용 텍스트(선택). generate 에서 프롬프트 최상단에 투입한다.
 */
@Getter
@Setter
public class TbmAi02Request {

    /** TBM 세션 코드(TB_TBM_SESSION.SESSION_CD). */
    private String sessionCd;

    /** 관리자 교육내용 텍스트(generate 전용, 선택). */
    private String adminContentText;
}
