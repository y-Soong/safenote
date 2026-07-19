package com.prafta.app.dailycontract.dailycontract01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 서명 저장 응답 (POST /appApi/dailycontract01/sign).
 */
@Value
@Builder
public class ContractSignResponse {
    String signId;
    int contractVer;
    String firstWorkDate;   // YYYYMMDD (=서명일, D1)
    String signDtime;       // YYYY-MM-DD HH:mm:ss (서버 시각)
}
