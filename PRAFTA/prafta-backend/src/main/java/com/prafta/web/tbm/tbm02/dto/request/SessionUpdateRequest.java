package com.prafta.web.tbm.tbm02.dto.request;

import java.util.List;

import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * W-06 세션 수정 요청(DRAFT/OPENED 상태만 허용, 서버 게이트).
 *
 * <p>입실/종료 비밀번호는 본 요청으로 수정하지 않는다(재발급 API 별도).
 */
@Getter
@Setter
@NoArgsConstructor
public class SessionUpdateRequest {
	private String sessionCd;
	private String title;
	private String contentBody;
	private String gpsVerifyTypeCd;
	private String managerGpsLat;
	private String managerGpsLon;
	private Integer gpsVerifyRadiusM;
	private Integer eduMinutes;				// 교육 인정시간(분, 1~60). 수정은 NULL 허용
	private String gpsManualConfirmYn;
	private List<SessionContentModel> contents;
	private List<SessionRiskModel> risks;
}
