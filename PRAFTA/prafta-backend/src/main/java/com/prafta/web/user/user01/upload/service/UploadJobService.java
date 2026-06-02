package com.prafta.web.user.user01.upload.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user01.upload.dto.response.UserUploadJobStartResponse;
import com.prafta.web.user.user01.upload.dto.response.UserUploadJobStatusResponse;

/**
 * 사용자 일괄 생성 비동기 잡 서비스 (PRAFTA-037-F6).
 *
 * <p>{@link #startUpload} 가 동기 검증/파싱/잡 INSERT 후 즉시 jobId 응답을 반환하고,
 * 내부에서 {@link UploadJobAsyncRunner#runAsync} 를 호출해 행 처리를 비동기 실행한다.
 * {@link #getStatus} 는 폴링용 상태 조회.
 */
public interface UploadJobService {

    /**
     * 업로드 시작 — 권한/파일/파싱 검증을 동기로 처리하고 잡 ID 응답.
     * 이후 행 처리는 {@code @Async} 로 분리.
     */
    UserUploadJobStartResponse startUpload(MultipartFile file, TokenInfo tokenInfo);

    /**
     * 잡 상태 조회 — 폴링 응답.
     * 본인 잡 아니면 {@code USER_404_002} (회사/존재 노출 차단).
     */
    UserUploadJobStatusResponse getStatus(String jobId, TokenInfo tokenInfo);
}
