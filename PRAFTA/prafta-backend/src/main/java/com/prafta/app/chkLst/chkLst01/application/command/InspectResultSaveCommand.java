package com.prafta.app.chkLst.chkLst01.application.command;

import com.prafta.app.chkLst.chkLst01.application.model.InspectAnswerItemModel;
import com.prafta.app.chkLst.chkLst01.application.param.InspectResultSaveParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B1: 점검결과 저장 Command (mapper.mergeChkptInspectAnswer 진입).
 * prafta-app-011: chkptCd 필드 추가 -- TB_CHKPT_INSPECT_ANSWER PK 구성 요소.
 *
 * <p>[정책 변경] 후행 덮어쓰기(last-writer-wins) 전환으로 소유 판정용 performKey 는 폐기됐다.
 * 저장은 무조건 UPSERT(덮어쓰기)이며, 수행자 스냅샷(PERFORM_*)은 매퍼가 토큰 클레임으로 채운다.
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
