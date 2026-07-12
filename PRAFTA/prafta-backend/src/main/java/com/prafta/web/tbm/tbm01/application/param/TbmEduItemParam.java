package com.prafta.web.tbm.tbm01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduItemRequest;

public record TbmEduItemParam(
	List<TbmEduItemModel> tbmEduItemModelList
	// 삭제 대상 항목의 회사 스코프(IDOR 방어). 마스터(TB_TBM_EDU_MTRL) 조인 시 CMPNY_CD 가드에 사용.
	, String gvCmpnyCd
){
	public static TbmEduItemParam from(List<TbmEduItemRequest> requests, TokenInfo tokenInfo) {

		// 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<TbmEduItemModel> models = requests.stream()
            .map(req -> {
                return new TbmEduItemModel(
                    req.getMtrlItemCd()
                );
            })
            .toList();

        return new TbmEduItemParam(models, tokenInfo.gv_cmpnyCd());
	}
}
