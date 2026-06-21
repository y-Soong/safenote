package com.prafta.web.baim.baim05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-055-3 — 슬롯 사용 이력 조회 파라미터. siteCd/slotNo 는 클라 RequestParam, 나머지는 JWT 도출.
 */
public record SlotHisParam(
    String siteCd
    , String slotNo
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static SlotHisParam from(String siteCd, String slotNo, TokenInfo tokenInfo) {

        if (siteCd == null || siteCd.isBlank()
            || slotNo == null || !slotNo.matches("\\d{1,4}")) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new SlotHisParam(
            siteCd
            , slotNo
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
