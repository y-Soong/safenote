package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import com.prafta.app.tbm.admin.result.AdminSessionContentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * T-A2 세션 상세 응답.
 *
 * <p>비밀번호는 서비스가 상태/권한 게이트(OPENED/IN_PROGRESS + 관리자)를 통과한 경우에만 채운다.
 * risks 는 표시명(displayName) 합성을 포함한 항목 목록.
 */
@Getter
@Builder
public class AdminSessionDetailResponse {
    private SessionDetailItem session;
    private List<AdminSessionContentResult> contents;
    private List<SessionRiskItem> risks;

    @Getter
    @Builder
    public static class SessionDetailItem {
        private String sessionCd;
        private String siteCd;
        private String siteNm;
        private String eduTypeCd;
        private String title;
        private String contentBody;
        private String contentFormatCd;
        private String statusCd;
        private String statusNm;
        private String entryPwd;        // 게이트 통과 시에만(OPENED/IN_PROGRESS + 관리자)
        private String exitPwd;
        private boolean pwdVisible;     // 비밀번호 노출 여부(프론트 표시 제어)
        private String managerUserCd;
        private String managerUserNm;
        private String managerGpsLat;
        private String managerGpsLon;
        private String gpsVerifyTypeCd;
        private Integer gpsVerifyRadiusM;
        private String gpsManualConfirmYn;
        private String openedAt;
        private String startedAt;
        private String endedAt;
        private String cancelledAt;
        private String cancelReason;
        private String insertNm;
        private String insertDate;
    }

    @Getter
    @Builder
    public static class SessionRiskItem {
        private String siteCd;
        private String processCd;
        private String processNm;
        private String riskTypeCd;
        private String riskTypeNm;
        private String hazardCd;
        private String hazardNm;
        private String assessmentCd;
        private String assessmentStatus;
        private String assessmentStatusNm;
        private String displayName;     // 공정명/위험요인/유해요인 합성
        private int displayOrder;
    }
}
