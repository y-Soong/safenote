package com.prafta.web.tbm.tbm02.dto.request;

import java.util.List;

import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * W-05 세션 개설(OPENED) / 임시저장(DRAFT) 요청.
 *
 * <p>{@code saveMode}: OPENED(개설하기) / DRAFT(임시저장). 서버가 최종 권위로 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionSaveRequest {
	private String saveMode;				// OPENED(개설) / DRAFT(임시저장)
	private String siteCd;
	private String title;
	private String contentBody;				// 리치 HTML
	private String gpsVerifyTypeCd;			// AUTO / MANUAL / DISABLED
	private String managerGpsLat;
	private String managerGpsLon;
	private Integer gpsVerifyRadiusM;		// 50~1000(기본 100)
	private String gpsManualConfirmYn;		// MANUAL 시 'Y' 필수
	private List<SessionContentModel> contents;	// 콘텐츠 묶음 매핑
	private List<SessionRiskModel> risks;		// 위험성평가 매핑(옵션)
}
