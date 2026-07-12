package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연계 도메인(근태/순회점검/위험성평가/TBM) 사고일 기준 조회 공통 요청.
 * acctId 로 사고 헤더(발생일/시각/재해자/사업장)를 서버에서 도출하므로
 * 조회 범위/매칭키는 body siteCd 가 아니라 사고 헤더에서 가져온다(IDOR 차단).
 *
 * <p>각 도메인의 선택 필터(좁히기)는 아래 옵션 필드로 받는다(미입력 시 전체).
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkQueryRequest {
    private String siteCd;
    private String acctId;

    // 순회점검 선택 필터
    private String chklstType; // COM001
    private String chkptCd;    // 점검대상(단건 좁히기; 다건은 FE 반복 호출)

    // 위험성평가 3계층 선택 필터(0~3 부분입력)
    private String processCd;
    private String riskTypeCd;
    private String hazardCd;
}
