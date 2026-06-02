package com.prafta.web.user.user01.upload.dto.response;

import java.util.List;

import com.prafta.web.user.user01.dto.UserUpdateFailItem;

/**
 * 비동기 업로드 진행률/결과 응답 (PRAFTA-037-F6).
 *
 * <p>폴링 중에는 상태/진행률만 의미 있고, 완료 상태(SUCCESS/PARTIAL/FAILED) 에 도달하면
 * {@code fails} 가 채워진다(JSON 직렬화에서 파싱). FAILED 상태에서는 {@code errorMsg} 활용.
 */
public record UserUploadJobStatusResponse(
        String jobId
        , String status
        , int totalRows
        , int processedRows
        , int successCount
        , int failCount
        , List<UserUpdateFailItem> fails
        , String errorMsg
) {
}
