package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.ContentOptionResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentOptionResponse {
	private List<ContentOptionResult> contentList;
}
