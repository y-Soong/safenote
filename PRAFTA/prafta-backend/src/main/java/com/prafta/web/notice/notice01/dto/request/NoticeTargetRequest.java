package com.prafta.web.notice.notice01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공지 대상 1건 요청(다건 리스트 요소).
 * SITE 스코프 = nodeCd null(사업장 전체). NODE 스코프 = nodeCd 지정.
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeTargetRequest {
    private String siteCd;
    private String nodeCd;
    private String includeDescendantsYn; // Y/N (NODE 일 때만 의미)
}
