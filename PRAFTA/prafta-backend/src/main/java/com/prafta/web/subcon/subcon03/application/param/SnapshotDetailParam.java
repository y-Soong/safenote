package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 수신 스냅샷 상세(읽기전용) 조회 파라미터(PRAFTA-SUBCON-T3 §5-8).
 *
 * <p>소유 검증(OWNER_CMPNY_CD = gvCmpnyCd)은 조회 SQL 안에서 강제한다(서비스 분기 누락 방지).
 */
public record SnapshotDetailParam(
    Long snapshotId
    , Integer page
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SnapshotDetailParam from(Long snapshotId, Integer page, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new SnapshotDetailParam(
            snapshotId
            , page
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
