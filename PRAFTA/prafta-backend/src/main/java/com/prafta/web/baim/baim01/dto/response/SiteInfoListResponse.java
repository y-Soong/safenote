package com.prafta.web.baim.baim01.dto.response;

import java.util.List;

import com.prafta.web.baim.baim01.result.SiteInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfoListResponse {
	List<SiteInfoResult> siteInfoList;

	/**
	 * 회사 통상근로자 주 소정근로 기준값(분) — 사업장 오버라이드의 <b>상속 기준</b>.
	 *
	 * <p>사업장 팝업이 "회사 기본값 사용 (주 40시간)" 라벨을 하드코딩 없이 그리기 위한 값이다.
	 * 회사 기준값 행이 없으면 코드 폴백 2400분이 실린다(항상 값 존재).
	 */
	Integer cmpnyWeekStdMinutes;
}
