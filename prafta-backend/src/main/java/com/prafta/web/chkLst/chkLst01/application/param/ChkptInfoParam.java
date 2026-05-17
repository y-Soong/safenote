package com.prafta.web.chkLst.chkLst01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst01.application.model.ChkptInfoModel;
import com.prafta.web.chkLst.chkLst01.dto.request.ChkptInfoRequest;

public record ChkptInfoParam(
	List<ChkptInfoModel> chkptInfoModelList
){
	public static ChkptInfoParam from(List<ChkptInfoRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<ChkptInfoModel> models = requests.stream()
            .map(req -> {
                return new ChkptInfoModel(
                    req.getChk()
                    , req.getSiteCd()
                    , req.getSiteNm()
                    , req.getChkLstType()
                    , req.getChkptCd()
                    , req.getChkptNm()
                    , req.getChkptDesc()
                    , req.getUseYn()
                    , req.getMgmtUserCd()
                    , req.getMgmtUserNm()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new ChkptInfoParam(models);
    }
}