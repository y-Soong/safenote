package com.prafta.web.user.user01.upload.dto.response;

/**
 * 비동기 업로드 시작 응답 (PRAFTA-037-F6).
 * 잡 ID 와 파싱된 행 수를 즉시 반환하여 프론트가 폴링을 시작할 수 있게 한다.
 */
public record UserUploadJobStartResponse(
        String jobId
        , int totalRows
) {
}
