package com.prafta.web.chkLst.chkLst05.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst05.result.AnswerHistResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerHistListResponse {
	private List<AnswerHistResult> answerHistList;
}
