package com.prafta.app.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.AppArchiveConstants;
import com.prafta.app.notice.notice02.application.param.AppArchiveTypeParam;

/**
 * 앱 자료타입 드롭다운 조회 쿼리(tb_baim_val_d). baimValCd 는 코드그룹 상수(COM008).
 */
public record AppArchiveTypeQuery(
    String gvCmpnyCd
    , String baimValCd
){
    public static AppArchiveTypeQuery from(AppArchiveTypeParam param, String baimValCd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppArchiveTypeQuery(
            param.gvCmpnyCd()
            , baimValCd
        );
    }

    /** 상수 주입 편의 팩토리. */
    public static AppArchiveTypeQuery from(AppArchiveTypeParam param) {
        return from(param, AppArchiveConstants.ARCHIVE_BAIM_VAL_CD);
    }
}
