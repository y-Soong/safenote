package com.prafta.web.subcon.subcon03.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon03.dto.request.ShareReqApproveRequest;

/**
 * 데이터 공유 요청 승인(= 스냅샷 생성) 파라미터(PRAFTA-SUBCON-T3 §5-6).
 *
 * <p>승인자 소속 회사(gvCmpnyCd)는 JWT 클레임에서만 도출하며, 당사자 조건(PRV_CMPNY_CD=gv)은
 * 조건부 UPDATE 로 선점 검증한다. bundleSnapshotIds 는 서버가 4조건 재검증 후에만 사용한다.
 */
public record ShareReqApproveParam(
    Long shareReqId
    , List<Long> bundleSnapshotIds
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ShareReqApproveParam from(ShareReqApproveRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ShareReqApproveParam(
            request.getShareReqId()
            , request.getBundleSnapshotIds()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
