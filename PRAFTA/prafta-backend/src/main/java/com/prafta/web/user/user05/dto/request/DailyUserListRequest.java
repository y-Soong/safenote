package com.prafta.web.user.user05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일일사용자 관리(조회) 화면 검색 요청.
 *
 * <p>조회조건: 사업장(siteCd), 소속부서(nodeCd + 하위부서 포함 incSubNodeYn),
 * 사용자명(userNm, 부분일치), 전화번호(mblNo, 서버에서 정규화→HMAC/LAST4 검색),
 * 슬롯 점유일시 기간(occupyFrom ~ occupyTo, yyyy-MM-dd).
 */
@Getter
@Setter
@NoArgsConstructor
public class DailyUserListRequest {
    private String siteCd;
    private String nodeCd;
    private String incSubNodeYn;
    private String userNm;
    private String mblNo;       // 평문 입력. 서버에서 정규화 후 HMAC/LAST4 로만 검색(평문 비교 금지).
    private String occupyFrom;  // yyyy-MM-dd (포함)
    private String occupyTo;    // yyyy-MM-dd (포함)
}
