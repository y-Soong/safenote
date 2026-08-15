package com.prafta.web.user.user01.upload.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.dto.UserUpdateFailItem;
import com.prafta.web.user.user01.upload.application.command.UploadJobInsertCommand;
import com.prafta.web.user.user01.upload.dto.response.UserUploadJobStartResponse;
import com.prafta.web.user.user01.upload.dto.response.UserUploadJobStatusResponse;
import com.prafta.web.user.user01.upload.mapper.UploadJobMapper;
import com.prafta.web.user.user01.upload.result.UploadJobResult;
import com.prafta.web.user.user01.upload.service.UploadJobAsyncRunner;
import com.prafta.web.user.user01.upload.service.UploadJobService;
import com.prafta.web.user.user01.util.UserExcelRowParser;
import com.prafta.web.user.user01.util.UserExcelValueRestorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-037-F6 — 사용자 일괄 생성 비동기 잡 서비스 구현.
 *
 * <p>{@link #startUpload} 는 동기로 권한/파일/파싱 검증 + 잡 INSERT 후 즉시 응답.
 * 비동기 행 처리는 {@link UploadJobAsyncRunner} 에 위임한다(Spring 프록시 자기호출 회피 — D14).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadJobServiceImpl implements UploadJobService {

    // 동기 endpoint 와 동일한 업로드 한도 (D9).
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int MAX_DATA_ROWS = 1000;

    private final UploadJobMapper uploadJobMapper;
    private final UploadJobAsyncRunner uploadJobAsyncRunner;
    private final ObjectMapper objectMapper;
    // prafta-052(보안): FAILS_JSON at-rest 암호문 복호화용(폴링 응답 빌드 시 평문 복원).
    private final AesGcmCrypto aesGcmCrypto;

    @Override
    public UserUploadJobStartResponse startUpload(MultipartFile file, TokenInfo tokenInfo) {

        // 1) 권한 가드 (동기 endpoint 와 동일).
        if (tokenInfo == null
                || tokenInfo.gv_authCd() == null
                || !AuthRoleUtils.isManager(tokenInfo.gv_authCd())) {
            throw new ApiException(UserErrorCode.USER_403_001);
        }

        // 2) 파일 존재 / 형식 / 크기 검증.
        if (file == null || file.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".xlsx")) {
            throw new ApiException(UserErrorCode.USER_400_050);
        }
        long fileSize = file.getSize();
        if (fileSize > MAX_UPLOAD_BYTES) {
            throw new ApiException(UserErrorCode.USER_400_051);
        }

        // 3) POI 시트 파싱 (동기).
        //   서식 유실로 앞자리 0 이 떨어진 값(휴대폰·생년월일)은 파서가 복원하고, 그 내역을 여기 모은다.
        //   파싱이 동기 구간이라 보정 내역은 시작 응답으로 바로 내려줄 수 있다(잡 테이블 확장 불필요).
        List<UserExcelValueRestorer.Adjustment> adjustments = new ArrayList<>();
        List<UserCreateParam> params;
        try (InputStream in = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(UserErrorCode.USER_400_053);
            }
            Sheet sheet = workbook.getSheetAt(0);
            params = UserExcelRowParser.parse(sheet, tokenInfo, adjustments);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("엑셀 비동기 업로드 파싱 실패 - file={}, size={}", originalName, fileSize, e);
            throw new ApiException(UserErrorCode.USER_400_053);
        }

        // 4) 데이터 행 수 검증.
        if (params.size() > MAX_DATA_ROWS) {
            throw new ApiException(UserErrorCode.USER_400_052);
        }

        // 5) 잡 INSERT (PENDING).
        String jobId = uploadJobMapper.selectNextJobId(tokenInfo.gv_cmpnyCd());

        UploadJobInsertCommand vo = new UploadJobInsertCommand();
        vo.setJobId(jobId);
        vo.setCmpnyCd(tokenInfo.gv_cmpnyCd());
        vo.setUserCd(tokenInfo.gv_userCd());
        vo.setFileName(originalName);
        vo.setFileSize(fileSize);
        vo.setTotalRows(params.size());
        vo.setInsertNo(tokenInfo.gv_userCd());
        uploadJobMapper.insertUploadJob(vo);

        log.info("엑셀 비동기 업로드 시작 - jobId={}, 요청자={}, 파일={}, totalRows={}",
                jobId, tokenInfo.gv_userCd(), originalName, params.size());

        // 6) 비동기 실행 (별도 Bean 호출 → 프록시 정상 통과).
        uploadJobAsyncRunner.runAsync(jobId, tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), params);

        if (!adjustments.isEmpty()) {
            log.info("엑셀 업로드 서식 보정 - jobId={}, 보정건수={}", jobId, adjustments.size());
        }

        // 7) 즉시 응답.
        return new UserUploadJobStartResponse(jobId, params.size(), adjustments);
    }

    @Override
    public UserUploadJobStatusResponse getStatus(String jobId, TokenInfo tokenInfo) {

        // 1) 권한 가드.
        if (tokenInfo == null
                || tokenInfo.gv_authCd() == null
                || !AuthRoleUtils.isManager(tokenInfo.gv_authCd())) {
            throw new ApiException(UserErrorCode.USER_403_001);
        }
        if (jobId == null || jobId.isBlank()) {
            throw new ApiException(UserErrorCode.USER_404_002);
        }

        UploadJobResult job = uploadJobMapper.selectUploadJob(tokenInfo.gv_cmpnyCd(), jobId);
        if (job == null) {
            throw new ApiException(UserErrorCode.USER_404_002);
        }
        // 2) IDOR 가드 — 본인 잡만 (D7). 다른 사용자 잡은 존재 노출하지 않도록 동일 404 메시지.
        if (!tokenInfo.gv_userCd().equals(job.userCd())) {
            log.warn("타인 업로드 잡 조회 차단 - 요청자={}, 잡소유자={}, jobId={}",
                    tokenInfo.gv_userCd(), job.userCd(), jobId);
            throw new ApiException(UserErrorCode.USER_404_002);
        }

        // 3) fails JSON 파싱 (최종 상태일 때만 실질 데이터). 파싱 실패 시 빈 리스트.
        List<UserUpdateFailItem> fails = parseFails(job.failsJson(), jobId);

        return new UserUploadJobStatusResponse(
                job.jobId()
                , job.status()
                , job.totalRows()
                , job.processedRows()
                , job.successCount()
                , job.failCount()
                , fails
                , job.errorMsg()
        );
    }

    /**
     * 저장된 FAILS_JSON 을 실패 항목 목록으로 역직렬화한다.
     *
     * <p>prafta-052(보안): 비동기 경로는 평문 PII 노출을 막기 위해 페이로드를 AES-GCM 으로
     * 암호화("v1.*")하여 저장한다. 따라서 값이 "v1." 로 시작하면 먼저 복호화한 뒤 역직렬화하고,
     * 그렇지 않으면(prafta-052 이전 평문 json 으로 저장된 레거시 행) 기존처럼 직접 역직렬화한다.
     * 복호화/파싱 실패 시 기존과 동일하게 빈 리스트로 폴백한다(jobId 만 로깅 — 본문/PII 비기록).
     */
    private List<UserUpdateFailItem> parseFails(String json, String jobId) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            // "v1." prefix = AES-GCM 암호문 → 복호화 후 역직렬화. 아니면 레거시 평문 json 직접 역직렬화.
            String plainJson = json.startsWith("v1.") ? aesGcmCrypto.decrypt(json) : json;
            if (plainJson == null || plainJson.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(plainJson, new TypeReference<List<UserUpdateFailItem>>() {});
        } catch (Exception e) {
            log.error("실패 목록 복호화/역직렬화 실패 - jobId={}", jobId, e);
            return new ArrayList<>();
        }
    }
}
