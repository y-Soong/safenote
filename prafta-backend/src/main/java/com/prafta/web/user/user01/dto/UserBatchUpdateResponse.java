package com.prafta.web.user.user01.dto;

import java.util.List;

public record UserBatchUpdateResponse(
        boolean success,          // fails가 있으면 false
        int totalCount,
        int successCount,
        int failCount,
        List<UserUpdateFailItem> fails
) {}