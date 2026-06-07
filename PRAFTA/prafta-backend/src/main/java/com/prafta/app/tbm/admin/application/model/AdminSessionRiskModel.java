package com.prafta.app.tbm.admin.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 세션-위험성평가 매핑 입력 모델(개설/수정 요청에 포함). */
@Getter
@Setter
@NoArgsConstructor
public class AdminSessionRiskModel {
    private String siteCd;          // 위험성평가 사업장코드
    private String processCd;       // 위험성평가 공정코드[COM002]
    private String assessmentCd;    // 위험성평가 평가코드
    private Integer displayOrder;   // 표시 순서
}
