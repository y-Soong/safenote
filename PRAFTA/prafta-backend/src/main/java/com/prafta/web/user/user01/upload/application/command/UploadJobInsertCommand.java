package com.prafta.web.user.user01.upload.application.command;

import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 일괄 생성 잡 INSERT 운반체 (PRAFTA-037-F6).
 *
 * <p>{@code tb_user_upload_job} 신규 행 적재. PENDING 상태 + 파싱된 totalRows 만 세팅하고
 * processedRows/successCount/failCount/fails 는 0/null 시작.
 */
@Getter
@Setter
public class UploadJobInsertCommand {

    /** 잡 ID (PK) — 서비스에서 채번하여 세팅 */
    private String jobId;
    private String cmpnyCd;
    private String userCd;
    private String fileName;
    private Long fileSize;
    private int totalRows;
    private String insertNo;
}
