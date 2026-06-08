package com.prafta.web.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.param.ArchiveTypeParam;

/**
 * 자료타입 드롭다운 조회 쿼리(tb_baim_val_d). baimValCd 는 코드그룹 상수(YJ 확정·주입 대상).
 */
public record ArchiveTypeQuery(
    String gvCmpnyCd
    , String baimValCd
){
    public static ArchiveTypeQuery from(ArchiveTypeParam param, String baimValCd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveTypeQuery(
            param.gvCmpnyCd()
            , baimValCd
        );
    }
}
