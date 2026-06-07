package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재해자 검색 요청 (정규 tb_user + 일용 tb_daily_user 양 풀, 사업장 스코프).
 */
@Getter
@Setter
@NoArgsConstructor
public class VictimSearchRequest {
    private String siteCd;
    private String userNm;     // 이름 부분검색
    private String userTypeCd; // 선택 필터: REGULAR/DAILY (미지정 시 양 풀)
}
