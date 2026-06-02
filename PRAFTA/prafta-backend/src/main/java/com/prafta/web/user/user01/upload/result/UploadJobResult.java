package com.prafta.web.user.user01.upload.result;

/**
 * 사용자 일괄 생성 잡 조회 결과 (PRAFTA-037-F6).
 *
 * <p>{@code tb_user_upload_job} 1행을 그대로 매핑한다. 프론트 폴링 응답으로 변환되어 노출된다.
 * MyBatis 가 record 캐노니컬 생성자로 매핑할 수 있도록 컬럼명과 1:1 대응한다.
 */
public record UploadJobResult(
        String jobId
        , String cmpnyCd
        , String userCd
        , String fileName
        , Long fileSize
        , int totalRows
        , int processedRows
        , int successCount
        , int failCount
        , String failsJson
        , String status
        , String errorMsg
) {
}
