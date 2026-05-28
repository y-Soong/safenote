package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 연차 수동 부여(단일/일괄) 응답.
 * POST /attd09/leave-grant/manual-grant, /bulk-manual-grant.
 */
@Value
@Builder
public class ManualGrantResponse {

    /** 부여 성공 건수 */
    int grantedCount;

    /** 부여된 사용자 코드 목록 */
    List<String> grantedUserCds;
}
