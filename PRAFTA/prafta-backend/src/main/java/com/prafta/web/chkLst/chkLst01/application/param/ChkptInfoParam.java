package com.prafta.web.chkLst.chkLst01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst01.application.model.ChkptInfoModel;
import com.prafta.web.chkLst.chkLst01.dto.request.ChkptInfoRequest;

/**
 * 점검대상 저장/삭제 입력.
 *
 * <p>[보안검토 High-2] {@code gvAuthCd} 는 메뉴/버튼 권한 게이트(ChkLst_01 · SAVE/DELT) 판정용이며
 * JWT 클레임에서만 채운다(요청 본문 미신뢰).
 */
public record ChkptInfoParam(
	List<ChkptInfoModel> chkptInfoModelList
	, String gvAuthCd
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

        return new ChkptInfoParam(models, tokenInfo.gv_authCd());
    }
}