package com.prafta.web.chkLst.chkLst02.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst02.application.model.ChkptInspectItemModel;
import com.prafta.web.chkLst.chkLst02.dto.request.ChkptInspectItemRequest;

public record ChkptInspectItemParam(
	List<ChkptInspectItemModel> chkptInspectItemModelList
) {
	public static ChkptInspectItemParam from(List<ChkptInspectItemRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - ChkptInspectItemRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<ChkptInspectItemModel> models = requests.stream()
            .map(req -> {
                return new ChkptInspectItemModel(
                    req.getCmpnyCd()
                    , req.getSiteCd()
                    , req.getChkLstType()
                    , req.getInspectItemCd()
                    , req.getInspectItemSubj()
                    , req.getSortIdx()
                    , req.getStrDate()
                    , req.getUseYn()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new ChkptInspectItemParam(models);
    }
}
