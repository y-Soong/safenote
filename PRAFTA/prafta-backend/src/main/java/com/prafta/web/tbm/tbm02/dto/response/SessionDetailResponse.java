package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.SessionContentResult;

import lombok.Builder;
import lombok.Getter;

/**
 * W-06 세션 상세 응답.
 *
 * <p>session 은 SessionResult 그대로 노출하되, 비밀번호는 서비스가 상태/권한 게이트를
 * 통과한 경우에만 채워서 반환한다. risks 는 표시명(displayName) 합성을 포함한 항목 목록.
 */
@Getter
@Builder
public class SessionDetailResponse {
	private SessionDetailItem session;
	private List<SessionContentResult> contents;
	private List<SessionRiskItem> risks;

	/** 세션 헤더(응답 전용). 비밀번호는 게이트 통과 시에만 채워진다. */
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
		private String entryPwd;		// 게이트 통과 시에만(OPENED/IN_PROGRESS + 관리자)
		private String exitPwd;
		private boolean pwdVisible;		// 비밀번호 노출 여부(프론트 표시 제어)
		private String managerUserCd;
		private String managerUserNm;
		private String managerGpsLat;
		private String managerGpsLon;
		private String gpsVerifyTypeCd;
		private Integer gpsVerifyRadiusM;
		private Integer eduMinutes;		// 교육 인정시간(분, 1~60). 미설정 시 null
		private String gpsManualConfirmYn;
		private String openedAt;
		private String prepStartAt;		// 교육준비 타이머 기준시각(15분 자동 교육시작 기준, FE 카운트다운용)
		private String prepAutoStartAt;	// 자동 교육시작 예정시각(=prepStartAt + 자동시작분, UTC 명시 'Z' 접미사, 서버 산출)
		private String startedAt;
		private String endedAt;
		private String cancelledAt;
		private String cancelReason;
		private String insertNm;
		private String insertDate;
	}

	/** 위험성평가 매핑 항목(표시명 합성 + 평가요청일/자 + 읽기전용 상세 필드 포함). */
	@Getter
	@Builder
	public static class SessionRiskItem {
		private String cmpnyCd;
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
		private String displayName;		// 공정명/위험요인/유해요인 합성(Risk03 규약 재사용)
		private int displayOrder;
		// 6.2-(1)-2: 평가요청일/평가요청자(콘솔 요약 표시)
		private String initAssessDate;
		private String initAssessorNm;
		// 결정#5: 읽기전용 상세 팝업(RiskAssessInfo) 채움용 평가 상세
		private String initLikelihoodScore;
		private String initSeverityScore;
		private String initRiskLv;
		private String initDesc;
		private String initAssessorId;
		private String initFileMgmtCd;
		private String initFilePath;
		private String revalDate;
		private String revalBeforeDesc;
		private String revalLikelihoodScore;
		private String revalSeverityScore;
		private String revalRiskLv;
		private String revalDesc;
		private String revalAssessorId;
		private String revalAssessorNm;
		private String revalAssessDate;
		private String revalFileMgmtCd;
		private String revalFilePath;
	}
}
