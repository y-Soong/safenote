package com.prafta.web.tbm.tbm02.dto.request;

import java.util.List;

import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 교육준비(OPENED) 전이 요청(prafta-051-03).
 *
 * <p>DRAFT 세션을 교육준비 상태로 전이하며, 이 시점에 관리자 GPS 중심좌표(웹 위치권한)와
 * GPS 검증 설정을 확정한다. 교육 내용/콘텐츠/위험성 매핑도 함께 최종 반영할 수 있다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionPrepareRequest {
	private String sessionCd;
	private String title;
	private String contentBody;
	private String gpsVerifyTypeCd;
	private String managerGpsLat;
	private String managerGpsLon;
	private Integer gpsVerifyRadiusM;
	private String gpsManualConfirmYn;
	private List<SessionContentModel> contents;
	private List<SessionRiskModel> risks;
}
