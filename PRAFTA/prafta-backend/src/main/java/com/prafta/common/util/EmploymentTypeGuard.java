package com.prafta.common.util;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 고용형태 기반 기능 접근 가드의 단일 출처(SSOT).
 *
 * <p>prafta-app-027 follow-up: 통합형 일용직(JWT {@code gv_employmentType='DAILY'})은
 * 스케줄/근무수정요청/초과근무/연차 신청이 해당없으므로(J1-4), 화면 표시 숨김만으로는
 * 토큰 직접 호출(서버 우회)을 막지 못한다. 본 가드를 해당 쓰기 endpoint 진입부에 삽입해
 * 서버에서 차단한다.
 *
 * <p>신뢰 출처는 토큰 도출 {@code employmentType} 뿐이다(클라 바디 신뢰 금지). 정규 사용자
 * (employmentType != 'DAILY')는 전부 통과하므로 회귀가 없다.
 */
public final class EmploymentTypeGuard {

    private EmploymentTypeGuard() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 일용직이면 403({@link CommonErrorCode#COMMON_403_002})을 던진다. 정규 사용자는 통과.
     *
     * @param tokenInfo JWT 도출 토큰 정보 (null 이면 통과 — 인증 가드는 AuthAspect/별도 책임)
     */
    public static void assertNotDailyWorker(TokenInfo tokenInfo) {
        if (tokenInfo == null) {
            return;
        }
        assertNotDailyWorker(tokenInfo.gv_employmentType());
    }

    /**
     * 고용형태가 일용직이면 403({@link CommonErrorCode#COMMON_403_002})을 던진다.
     *
     * @param employmentType JWT 클레임 {@code gv_employmentType} 도출 값
     */
    public static void assertNotDailyWorker(String employmentType) {
        if (AuthRoleUtils.isDailyWorker(employmentType)) {
            throw new ApiException(CommonErrorCode.COMMON_403_002);
        }
    }
}
