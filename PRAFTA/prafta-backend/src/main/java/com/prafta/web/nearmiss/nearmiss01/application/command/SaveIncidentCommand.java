package com.prafta.web.nearmiss.nearmiss01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.SaveIncidentParam;

/**
 * E4 정밀조사 저장 Command (원인/재발방지/즉시조치/임시조치).
 *
 * <p>{@code adminTempActionDesc} 는 <b>null 이면 매퍼가 SET 자체를 하지 않는다</b>(미변경).
 *    빈 문자열은 "명시적 삭제" 로 그대로 저장된다. 종결(완료 300 / 미처리대상 400) 건은
 *    {@link #from(SaveIncidentParam, boolean)} 이 null 로 눌러 읽기전용을 서버에서 강제한다
 *    (화면 readonly 는 우회 가능하므로 최종 판단은 서버).
 */
public record SaveIncidentCommand(
    String siteCd
    , String nearMissId
    , String causeDesc
    , String preventionDesc
    , String immediateActionDesc
    , String adminTempActionDesc
    , String gvCmpnyCd
    , String gvUserCd
){
    public static SaveIncidentCommand from(SaveIncidentParam param) {
        return from(param, true);
    }

    /**
     * @param tempActionEditable false 면 임시조치 메모를 무시한다(종결 건 읽기전용).
     */
    public static SaveIncidentCommand from(SaveIncidentParam param, boolean tempActionEditable) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SaveIncidentCommand(
            param.siteCd()
            , param.nearMissId()
            , param.causeDesc()
            , param.preventionDesc()
            , param.immediateActionDesc()
            , tempActionEditable ? param.adminTempActionDesc() : null
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
