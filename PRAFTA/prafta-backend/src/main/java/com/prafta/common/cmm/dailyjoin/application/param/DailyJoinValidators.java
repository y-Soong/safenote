package com.prafta.common.cmm.dailyjoin.application.param;

import com.prafta.common.error.dailyjoin.DailyJoinErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.normalize.Normalizers;

/**
 * 일일사용자 회원가입 입력값 형식/길이 서버측 검증 헬퍼.
 * dailyjoin 모듈 전용(가입 폼 규칙)이므로 common.util 로 승격하지 않는다.
 */
final class DailyJoinValidators {

    private DailyJoinValidators() {
    }

    /** 회사코드 : blank 아님 + varchar(50) 상한. 자릿수 고정 검증은 하지 않는다. */
    static void validateCmpnyCd(String cmpnyCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || cmpnyCd.length() > 50) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }

    /** 사업장코드 : joinCd 규칙상 5자리 고정. */
    static void validateSiteCd(String siteCd) {
        if (siteCd == null || siteCd.length() != 5) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }

    /** 사용자ID : 영숫자 4~10자. */
    static void validateUserId(String userId) {
        if (userId == null || !userId.matches("^[A-Za-z0-9]{4,10}$")) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }

    /**
     * 비밀번호 : 6~15자 + 영문/숫자/특수문자 중 2종 이상(정규 사용자 규칙 미러, 공통 정책 §3.1).
     */
    static void validatePassword(String userPw) {
        if (userPw == null) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
        int len = userPw.length();
        if (len < 6 || len > 15) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
        int typeCount = 0;
        if (userPw.matches(".*[0-9].*")) {
            typeCount++;
        }
        if (userPw.matches(".*[a-zA-Z].*")) {
            typeCount++;
        }
        if (userPw.matches(".*[^a-zA-Z0-9].*")) {
            typeCount++;
        }
        if (typeCount < 2) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }

    /** 사용자명 : blank 아님 + 최대 15자. */
    static void validateUserNm(String userNm) {
        if (userNm == null || userNm.isBlank() || userNm.length() > 15) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }

    /** 인증번호 : 숫자 6자리. */
    static void validateCertNo(String certNo) {
        if (certNo == null || !certNo.matches("^\\d{6}$")) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }

    /** 휴대폰번호 : 숫자 정규화 후 11자리 상한. */
    static void validateMblNo(String mblNo) {
        String norm = Normalizers.normalizePhone(mblNo);
        if (norm == null || norm.length() > 11) {
            throw new ApiException(DailyJoinErrorCode.DAILYJOIN_400_006);
        }
    }
}
