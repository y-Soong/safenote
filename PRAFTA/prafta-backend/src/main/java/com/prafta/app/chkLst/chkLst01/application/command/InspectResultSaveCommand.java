package com.prafta.app.chkLst.chkLst01.application.command;

import com.prafta.app.chkLst.chkLst01.application.model.InspectAnswerItemModel;
import com.prafta.app.chkLst.chkLst01.application.param.InspectResultSaveParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B1: 점검결과 저장 Command (mapper.mergeChkptInspectAnswer 진입).
 * prafta-app-011: chkptCd 필드 추가 -- TB_CHKPT_INSPECT_ANSWER PK 구성 요소.
 */
public record InspectResultSaveCommand(
    String cmpnyCd
    , String siteCd
    , String chkptCd
    , String inspectItemCd
    , String workDate
    , String inspectAnswerType
    , String answerDesc
    , String fileMgmtCd
) {
    public static InspectResultSaveCommand from(
            InspectResultSaveParam param
            , InspectAnswerItemModel item
            , String fileMgmtCd
    ) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (item == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InspectResultSaveCommand(
            param.cmpnyCd()
            , param.siteCd()
            , param.chkptCd()
            , item.itemCd()
            , param.workDate()
            , item.inspectValue()
            , item.answerDesc()
            , fileMgmtCd
        );
    }
}
