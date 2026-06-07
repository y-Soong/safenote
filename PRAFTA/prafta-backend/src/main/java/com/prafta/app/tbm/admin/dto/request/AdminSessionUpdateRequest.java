package com.prafta.app.tbm.admin.dto.request;

import java.util.List;

import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;
import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * T-A4 관리자 TBM 세션 수정 요청(DRAFT/OPENED 상태만 허용, 서버 게이트).
 *
 * <p>입실/종료 비밀번호는 본 요청으로 수정하지 않는다(재발급 API 별도). sessionCd 는 path 에서 받는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminSessionUpdateRequest {
    private String title;
    private String contentBody;
    private String gpsVerifyTypeCd;
    private String managerGpsLat;
    private String managerGpsLon;
    private Integer gpsVerifyRadiusM;
    private String gpsManualConfirmYn;
    private List<AdminSessionContentModel> contents;
    private List<AdminSessionRiskModel> risks;
}
