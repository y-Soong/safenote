package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.common.cmm.tbmshare.result.ShareCandidateResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동 회사 지정 후보 응답(PRAFTA-SUBCON-T5).
 *
 * <p>행위자 회사와 관계 ACCEPTED 인 회사 − 개설사 − 이미 이 세션 체인에 있는 회사.
 */
@Getter
@Builder
public class SessionShareCandidateResponse {
	private List<ShareCandidateResult> candidateList;
}
