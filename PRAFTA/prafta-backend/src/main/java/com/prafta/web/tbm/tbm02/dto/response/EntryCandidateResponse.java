package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.EntryCandidateResult;

import lombok.Builder;
import lombok.Getter;

/** 입실 후보 검색 응답(prafta-051-11). */
@Getter
@Builder
public class EntryCandidateResponse {
	private String userTypeCd;
	private List<EntryCandidateResult> candidateList;
}
