package com.prafta.web.user.user01.upload.service;

import java.util.List;

import com.prafta.web.user.user01.application.param.UserCreateParam;

/**
 * 사용자 일괄 생성 비동기 실행 계층 (PRAFTA-037-F6).
 *
 * <p>{@code @Async} 가 정상 동작하려면 동일 Bean 자기 호출은 프록시를 거치지 않으므로,
 * {@link UploadJobService} 의 동기 진입과 분리해 별도 Bean 으로 운반한다.
 */
public interface UploadJobAsyncRunner {

    /**
     * 행 처리 비동기 실행 — RUNNING 으로 전이 후 행별 INSERT, 종료 시 SUCCESS/PARTIAL 로 최종화.
     * 치명 예외 시 FAILED + errorMsg.
     */
    void runAsync(String jobId, String cmpnyCd, String userCd, List<UserCreateParam> params);
}
