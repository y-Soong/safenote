package com.prafta.web.user.user01.upload.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.error.ApiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.dto.UserUpdateFailItem;
import com.prafta.web.user.user01.service.User01Service;
import com.prafta.web.user.user01.upload.constant.UploadJobStatus;
import com.prafta.web.user.user01.upload.mapper.UploadJobMapper;
import com.prafta.web.user.user01.upload.service.UploadJobAsyncRunner;
import com.prafta.web.user.user01.util.UserExcelRowParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-037-F6 — 사용자 일괄 생성 비동기 실행 구현.
 *
 * <p>Spring 기본 {@code SimpleAsyncTaskExecutor} 위에서 동작한다(D10). 행 처리 트랜잭션은
 * 기존 {@link User01Service#insertUserOne} 의 {@code REQUIRES_NEW} 가 그대로 적용된다.
 * 진행률/최종 상태 UPDATE 도 별도 {@code REQUIRES_NEW} 로 분리해 즉시 가시화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadJobAsyncRunnerImpl implements UploadJobAsyncRunner {

    private final User01Service user01Service;
    private final UploadJobMapper uploadJobMapper;
    private final ObjectMapper objectMapper;
    // prafta-052(보안): 실패 항목(평문 PII 포함) FAILS_JSON at-rest 암호화용.
    private final AesGcmCrypto aesGcmCrypto;

    @Override
    @Async
    public void runAsync(String jobId, String cmpnyCd, String userCd, List<UserCreateParam> params) {

        try {
            transitionToRunning(cmpnyCd, jobId, userCd);

            List<UserUpdateFailItem> fails = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int total = params == null ? 0 : params.size();

            for (int i = 0; i < total; i++) {
                UserCreateParam p = params.get(i);
                boolean ok = false;
                String message = null;
                String errorCode = null;
                try {
                    user01Service.insertUserOne(p); // REQUIRES_NEW 트랜잭션
                    ok = true;
                } catch (ApiException e) {
                    ApiErrorCode code = e.getErrorCode();
                    errorCode = code != null ? code.code() : CommonErrorCode.COMMON_500_001.code();
                    message = e.getResolvedMessage();
                } catch (Exception e) {
                    log.error("엑셀 비동기 업로드 행 처리 실패 - jobId={}, index={}, userId={}",
                            jobId, i, p == null ? null : p.userId(), e);
                    errorCode = CommonErrorCode.COMMON_500_001.code();
                    message = "처리 중 오류가 발생했습니다.\n관리자에게 문의해주세요.";
                }

                if (ok) {
                    successCount++;
                    incrementProgress(cmpnyCd, jobId, userCd, 1, 0);
                } else {
                    failCount++;
                    // prafta-052: 실패 행 재업로드용 원본값 보존. toSourceRow 는 p==null 가드 내장(빈 리스트).
                    fails.add(new UserUpdateFailItem(i, p == null ? null : p.userId(), errorCode, message,
                            UserExcelRowParser.toSourceRow(p)));
                    incrementProgress(cmpnyCd, jobId, userCd, 0, 1);
                }
            }

            String finalStatus = (failCount == 0) ? UploadJobStatus.SUCCESS : UploadJobStatus.PARTIAL;
            String failsJson = serializeFails(fails);
            finalizeJob(cmpnyCd, jobId, userCd, finalStatus, failsJson, null);

            log.info("엑셀 비동기 업로드 완료 - jobId={}, status={}, 성공={}, 실패={}",
                    jobId, finalStatus, successCount, failCount);

        } catch (Exception fatal) {
            // 치명 예외 — RUNNING 전이도 실패하거나 finalize 단계 자체 실패.
            log.error("엑셀 비동기 업로드 치명 예외 - jobId={}", jobId, fatal);
            try {
                finalizeJob(cmpnyCd, jobId, userCd, UploadJobStatus.FAILED, null,
                        "처리 중 치명 오류가 발생했습니다.\n관리자에게 문의해주세요.");
            } catch (Exception ignore) {
                // 마지막 UPDATE 도 실패하면 application log 만 남기고 종료.
            }
        }
    }

    /**
     * 진행률/상태 UPDATE 는 별도 트랜잭션 어노테이션 없이 mapper 직접 호출한다.
     * - Spring MyBatis 가 SqlSession 단위로 자동 commit/flush (DataSource auto-commit 또는 mapper 단발 트랜잭션) 처리.
     * - 동일 Bean 자기 호출은 프록시를 거치지 않아 REQUIRES_NEW 효과가 없으므로,
     *   자기호출 회피 + 별도 Bean 신설 부담 사이에서 짧은 UPDATE 1건의 단순성을 우선.
     * - 폴링 응답은 즉시 갱신된 행을 본다(별도 트랜잭션 격리 없음).
     */
    private void transitionToRunning(String cmpnyCd, String jobId, String userCd) {
        uploadJobMapper.updateUploadJobProgress(cmpnyCd, jobId, UploadJobStatus.RUNNING, 0, 0, userCd);
    }

    private void incrementProgress(String cmpnyCd, String jobId, String userCd, int successInc, int failInc) {
        uploadJobMapper.updateUploadJobProgress(cmpnyCd, jobId, UploadJobStatus.RUNNING, successInc, failInc, userCd);
    }

    private void finalizeJob(String cmpnyCd, String jobId, String userCd, String status, String failsJson, String errorMsg) {
        uploadJobMapper.updateUploadJobFinal(cmpnyCd, jobId, status, failsJson, errorMsg, userCd);
    }

    /**
     * 실패 항목 목록을 직렬화한다.
     *
     * <p>prafta-052(보안): 실패 행 원본값(sourceRow)에는 휴대폰/이메일/생년월일 등 평문 PII 가
     * 포함되므로, 직렬화한 JSON 페이로드 전체를 AES-GCM 으로 암호화("v1.*")하여 DB(FAILS_JSON)에
     * 평문이 저장되지 않게 한다. 폴링 응답 빌드 시 {@code parseFails} 가 복호화한다.
     * 실패가 없으면 저장할 페이로드가 없으므로 null 을 저장한다(빈 "[]" 도 암호화 대상 아님).
     * 로그에는 PII/본문(JSON·암호문)을 절대 남기지 않는다.
     */
    private String serializeFails(List<UserUpdateFailItem> fails) {
        if (fails == null || fails.isEmpty()) return null;
        try {
            String json = objectMapper.writeValueAsString(fails);
            return aesGcmCrypto.encrypt(json); // at-rest 암호화 ("v1.<base64url>")
        } catch (Exception e) {
            log.error("실패 목록 직렬화/암호화 실패 — failsJson=null 로 저장", e);
            return null;
        }
    }
}
