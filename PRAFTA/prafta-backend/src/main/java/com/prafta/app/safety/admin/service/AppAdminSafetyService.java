package com.prafta.app.safety.admin.service;

import com.prafta.app.safety.admin.application.param.InspectionDetailParam;
import com.prafta.app.safety.admin.application.param.InspectionListParam;
import com.prafta.app.safety.admin.application.param.RiskDetailParam;
import com.prafta.app.safety.admin.application.param.RiskFindingListParam;
import com.prafta.app.safety.admin.application.param.RiskStatusParam;
import com.prafta.app.safety.admin.dto.response.InspectionDetailResponse;
import com.prafta.app.safety.admin.dto.response.InspectionListResponse;
import com.prafta.app.safety.admin.dto.response.RiskDetailResponse;
import com.prafta.app.safety.admin.dto.response.RiskFindingListResponse;

/**
 * 관리자 안전 관리(순회점검 결과/위험성평가) 서비스 (prafta-app-025 J1-6).
 *
 * <p>모든 EP 선두에서 SAFETY 진입 게이트(master ∥ safe ∥ nodeAdmin-in-site)와 사업장 접근을
 *    서버에서 재강제한다(A-1 비상속). 위험성평가 상태전환은 허용 전이만, 동시성은 현재 상태 가드로 직렬화한다.
 */
public interface AppAdminSafetyService {

    /** H1 순회점검 결과 리스트(단일 사업장 스코프 + 월 필터). */
    InspectionListResponse selectInspectionList(InspectionListParam param);

    /** H2 순회점검 상세(일자별 답변 + 불량 사진/비고). */
    InspectionDetailResponse selectInspectionDetail(InspectionDetailParam param);

    /** H3 위험성평가 목록(사업장 스코프 + 필터). */
    RiskFindingListResponse selectRiskFindings(RiskFindingListParam param);

    /** H4 위험성평가 상세 단건. */
    RiskDetailResponse selectRiskDetail(RiskDetailParam param);

    /** H5 위험성평가 상태전환(허용 전이만, 현재 상태 가드). */
    void changeRiskStatus(RiskStatusParam param);
}
