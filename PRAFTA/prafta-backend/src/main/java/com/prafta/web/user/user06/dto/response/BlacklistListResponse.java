package com.prafta.web.user.user06.dto.response;

import java.util.List;

import com.prafta.web.user.user06.result.BlacklistResult;

import lombok.Builder;
import lombok.Value;

/**
 * 블랙리스트 목록 응답.
 */
@Value
@Builder
public class BlacklistListResponse {
    List<BlacklistResult> blacklist;
}
