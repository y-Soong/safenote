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
        // 위치정보 동의철회·중지 S2: 4-state 전이 기록. null = 상태 관리 대상이 아닌 약관.
        //   ★AGR_YN 만으로는 SUSPENDED→WITHDRAWN 같은 전이('N'→'N')가 이력에 남지 않는다.
        , String beforeState
        , String afterState
) {
    /** 상태 관리 대상이 아닌 약관용(종전 호출부 전부). state 는 null 로 둔다. */
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

        return of(cmpnyCd, userCd, termsId, termsVersion, beforeAgrYn, afterAgrYn
                , agrSource, actorCmpnyCd, actorUserCd, null, null);
    }

    /** 상태 관리 대상 약관용(위치기반서비스 005). */
    public static ConsentHistInsertCommand of(
            String cmpnyCd
            , String userCd
            , String termsId
            , String termsVersion
            , String beforeAgrYn
            , String afterAgrYn
            , String agrSource
            , String actorCmpnyCd
            , String actorUserCd
            , String beforeState
            , String afterState) {

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
                cmpnyCd, userCd, termsId, termsVersion, beforeAgrYn, afterAgrYn, agrSource, actorCmpnyCd, actorUserCd
                , beforeState, afterState);
    }
}
