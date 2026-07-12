package com.prafta.web.user.user05.dto.response;

import java.util.List;

import com.prafta.web.user.user05.result.DailyUserListResult;

import lombok.Builder;
import lombok.Value;

/**
 * 일일사용자 관리(조회) 목록 응답.
 */
@Value
@Builder
public class DailyUserListResponse {
    List<DailyUserListResult> dailyUserList;
}
