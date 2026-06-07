package com.prafta.app.tbm.admin.dto.request;

import java.util.List;

import com.prafta.app.tbm.admin.application.model.AdminSessionContentModel;
import com.prafta.app.tbm.admin.application.model.AdminSessionRiskModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * T-A3 관리자 TBM 세션 개설(OPENED) / 임시저장(DRAFT) 요청.
 *
 * <p>{@code saveMode}: OPENED(개설하기) / DRAFT(임시저장). 서버가 최종 권위로 검증한다.
 * 식별자(회사/사용자)는 JWT 클레임에서만 도출하며 본 바디에 포함하지 않는다(D1).
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminSessionSaveRequest {
    private String saveMode;                // OPENED(개설) / DRAFT(임시저장)
    private String siteCd;                  // 접근가능 사업장(서버가 USE_YN='Y' 검증)
    private String title;
    private String contentBody;             // plain text(T5: textarea, 줄바꿈 보존)
    private String gpsVerifyTypeCd;         // AUTO / MANUAL / DISABLED
    private String managerGpsLat;
    private String managerGpsLon;
    private Integer gpsVerifyRadiusM;       // 50~1000(기본 100)
    private String gpsManualConfirmYn;      // MANUAL 시 'Y' 필수
    private List<AdminSessionContentModel> contents;    // 콘텐츠 묶음 매핑
    private List<AdminSessionRiskModel> risks;          // 위험성평가 매핑(옵션)
}
