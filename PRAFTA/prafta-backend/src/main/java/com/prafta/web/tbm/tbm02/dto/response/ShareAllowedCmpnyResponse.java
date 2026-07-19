package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.common.cmm.tbmshare.result.AllowedCmpnyResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 입실 대상 회사 목록 응답(PRAFTA-SUBCON-T5) — 웹 대리입실 팝업의 "대상 회사" 셀렉트 소스.
 *
 * <p>개설사 + 지정 체인. 회사명은 서버 relabel 값(2차 이하는 1차 회사명으로 접힘)이며,
 * 프론트가 회사코드로 이름을 재조립하는 것을 금지한다.
 */
@Getter
@Builder
public class ShareAllowedCmpnyResponse {
	private List<AllowedCmpnyResult> cmpnyList;
}
