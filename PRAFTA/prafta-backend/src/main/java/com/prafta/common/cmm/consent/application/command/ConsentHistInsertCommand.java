package com.prafta.common.cmm.consent.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 약관 동의/철회 전이 이력 INSERT Command(TB_TERMS_USER_AGR_HIST) — PRAFTA-SUBCON-T4-02.
 *
 * <p>append-only. UPDATE/DELETE 경로는 존재하지 않는다(plan D8).
 * <p>beforeAgrYn 은 최초 응답이면 null(기록 없음). ACTION_DTIME 은 매퍼가 서버 NOW() 로 채운다(클라 시각 불신).
 * <p>actor* 는 변경 주체(현재는 항상 본인 = JWT 클레임). 관리자 대행 도입 대비로 별도 컬럼을 둔다.
 */
public record ConsentHistInsertCommand(
        String cmpnyCd
        , String userCd
        , String termsId
        , String termsVersion
        , String beforeAgrYn
        , String afterAgrYn
        , String agrSource
        , String actorCmpnyCd
        , String actorUserCd
) {
    public static ConsentHistInsertCommand of(
            String cmpnyCd
            , String userCd
            , String termsId
            , String termsVersion
            , String beforeAgrYn
            , String afterAgrYn
            , String agrSource
            , String actorCmpnyCd
            , String actorUserCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || userCd == null || userCd.isBlank()
                || termsId == null || termsId.isBlank()
                || termsVersion == null || termsVersion.isBlank()
                || afterAgrYn == null || afterAgrYn.isBlank()
                || agrSource == null || agrSource.isBlank()
                || actorCmpnyCd == null || actorCmpnyCd.isBlank()
                || actorUserCd == null || actorUserCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ConsentHistInsertCommand(
                cmpnyCd, userCd, termsId, termsVersion, beforeAgrYn, afterAgrYn, agrSource, actorCmpnyCd, actorUserCd);
    }
}
