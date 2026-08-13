package com.prafta.common.cmm.baseinfo.dto.response;

import java.util.List;

import com.prafta.common.cmm.baseinfo.result.JoinTermsResult;

import lombok.Builder;
import lombok.Value;

/** 회원가입 필수약관 목록 응답. 약관 전문은 싣지 않는다(전문은 terms-detail-infos). */
@Value
@Builder
public class JoinTermsListResponse {
	List<JoinTermsResult> joinTermsList;
}
