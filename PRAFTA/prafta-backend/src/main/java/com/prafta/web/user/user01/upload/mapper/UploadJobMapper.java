package com.prafta.web.user.user01.upload.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user01.upload.application.command.UploadJobInsertCommand;
import com.prafta.web.user.user01.upload.result.UploadJobResult;

/**
 * 사용자 일괄 생성 잡 매퍼 (PRAFTA-037-F6).
 */
@Mapper
public interface UploadJobMapper {

    /** JOB_ID 채번. {@code 'U' + YYYYMMDD + FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'USER_UPLOAD_JOB_ID')}. */
    String selectNextJobId(@Param("cmpnyCd") String cmpnyCd);

    /** 신규 잡 INSERT (PENDING 상태로 시작). */
    int insertUploadJob(UploadJobInsertCommand command);

    /** 회사 스코프 + 잡 조회. 없으면 null. */
    UploadJobResult selectUploadJob(@Param("cmpnyCd") String cmpnyCd, @Param("jobId") String jobId);

    /**
     * 진행률 UPDATE — 매 행 처리 직후 호출.
     * {@code processedRows += 1}, 실패 시 {@code failCount += 1}, 성공 시 {@code successCount += 1}.
     * 상태도 함께 갱신(RUNNING 진입 또는 유지).
     */
    int updateUploadJobProgress(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("jobId") String jobId
            , @Param("status") String status
            , @Param("successInc") int successInc
            , @Param("failInc") int failInc
            , @Param("updateNo") String updateNo
    );

    /**
     * 최종 상태 UPDATE — 잡 종료 시 1회 호출.
     * 상태(SUCCESS/PARTIAL/FAILED) + fails 직렬화 JSON + errorMsg(선택).
     */
    int updateUploadJobFinal(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("jobId") String jobId
            , @Param("status") String status
            , @Param("failsJson") String failsJson
            , @Param("errorMsg") String errorMsg
            , @Param("updateNo") String updateNo
    );

    // selectMyUploadJobs (사용자별 본인 잡 목록 조회) — 본 작업 범위 밖, follow-up §7.
}
