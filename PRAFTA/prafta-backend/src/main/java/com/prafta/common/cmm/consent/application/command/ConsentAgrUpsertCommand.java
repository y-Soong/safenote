package com.prafta.common.cmm.consent.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 약관 동의 현재상태 upsert Command(TB_TERMS_USER_AGR_MGMT) — PRAFTA-SUBCON-T4-02.
 *
 * <p>PK=(CMPNY_CD, USER_CD, TERMS_ID, TERMS_VERSION) 충돌 시 AGR_YN 갱신(스키마에 UPDATE_* 컬럼 없음).
 * <p>★ 이 upsert 는 ConsentHistoryRecorder 를 통해서만 호출한다(이력 우회 경로 금지 — plan §2 T4-03).
 *    terms01 의 기존 upsertTermsAgr 는 본 커맨드/매퍼로 일원화되었다.
 * <p>CMPNY_CD/USER_CD 는 JWT, TERMS_VERSION 은 TB_TERMS 현재버전 — 모두 서버 확정값만 사용한다.
 */
public record ConsentAgrUpsertCommand(
        String cmpnyCd
        , String userCd
        , String termsId
        , String termsVersion
        , String agrYn
) {
    public static ConsentAgrUpsertCommand of(String cmpnyCd, String userCd, String termsId, String termsVersion, String agrYn) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || userCd == null || userCd.isBlank()
                || termsId == null || termsId.isBlank()
                || termsVersion == null || termsVersion.isBlank()
                || agrYn == null || agrYn.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ConsentAgrUpsertCommand(cmpnyCd, userCd, termsId, termsVersion, agrYn);
    }
}
