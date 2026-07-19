package com.prafta.app.dailycontract.dailycontract01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 본인 서명 계약서 메타 응답 (GET /appApi/dailycontract01/my-sign — UI-DC-04 메타 카드).
 *
 * <p>signYn='N' 이면 서명본 없음(빈 상태 화면). 파일 경로는 응답에 포함하지 않는다 —
 * 합성본은 GET my-sign-image 스트림으로만 열람한다(교부 의무 §6-1).
 */
@Value
@Builder
public class MySignResponse {
    String signYn;          // 'Y' | 'N'
    String signId;
    Integer contractVer;
    String firstWorkDate;   // YYYYMMDD
    String signDtime;       // YYYY-MM-DD HH:mm:ss
}
