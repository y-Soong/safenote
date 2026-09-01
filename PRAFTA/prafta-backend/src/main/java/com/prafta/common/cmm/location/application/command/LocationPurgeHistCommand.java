package com.prafta.common.cmm.location.application.command;

import com.prafta.common.cmm.location.mapper.result.LocationPurgeScopeResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 위치정보 파기 이력 INSERT Command({@code TB_LOCATION_PURGE_HIST}) — 위치정보 동의철회·중지 S3.
 *
 * <p>append-only. UPDATE/DELETE 경로는 만들지 않는다.
 *
 * <p>★★<b>좌표 값을 담는 필드를 절대 추가하지 말 것.</b> 원본은 물론 해시·마스킹·부분값 등
 * 어떤 파생 형태도 남기면 파기가 파기가 아니게 된다. 남기는 것은 "누가·언제·무엇을·몇 건·왜" 뿐이다.
 *
 * <p>{@code ACTION_DTIME} 은 매퍼가 서버 {@code NOW()} 로 채운다(클라 시각 불신).
 */
public record LocationPurgeHistCommand(
        String cmpnyCd
        , String userCd
        , String userTypeCd
        , String purgeReasonCd
        , String termsVersion
        , int attdGpsRows
        , int tbmAttendanceRows
        , int tbmSessionRows
        , String oldestCollected
        , String latestCollected
        , String actorCmpnyCd
        , String actorUserCd
) {
    /**
     * 파기 실행 결과로부터 이력 커맨드를 만든다.
     *
     * @param scope         파기 <b>전</b>에 집계한 대상(기간 산출용)
     * @param purgedAttd    실제 파기된 출퇴근 좌표 행 수
     * @param purgedTbmAtt  실제 파기된 TBM 입실 좌표 행 수
     * @param purgedTbmSes  실제 파기된 TBM 개설자 좌표 행 수
     */
    public static LocationPurgeHistCommand of(
            String cmpnyCd
            , String userCd
            , String userTypeCd
            , String purgeReasonCd
            , String termsVersion
            , LocationPurgeScopeResult scope
            , int purgedAttd
            , int purgedTbmAtt
            , int purgedTbmSes
            , String actorCmpnyCd
            , String actorUserCd) {

        if (cmpnyCd == null || cmpnyCd.isBlank()
                || userCd == null || userCd.isBlank()
                || userTypeCd == null || userTypeCd.isBlank()
                || purgeReasonCd == null || purgeReasonCd.isBlank()
                || actorCmpnyCd == null || actorCmpnyCd.isBlank()
                || actorUserCd == null || actorUserCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LocationPurgeHistCommand(
                cmpnyCd, userCd, userTypeCd, purgeReasonCd, termsVersion
                , purgedAttd, purgedTbmAtt, purgedTbmSes
                , scope == null ? null : scope.oldestCollected()
                , scope == null ? null : scope.latestCollected()
                , actorCmpnyCd, actorUserCd);
    }
}
