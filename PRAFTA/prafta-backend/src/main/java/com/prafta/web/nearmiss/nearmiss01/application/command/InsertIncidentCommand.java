package com.prafta.web.nearmiss.nearmiss01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.ReclassifyParam;

/**
 * 재분류(E6) 시 tb_near_miss INSERT 커맨드.
 * nearMissId 는 서비스에서 채번 후 주입. reportStatusCd 는 접수(100) 고정.
 * srcProcessCd/srcAssessmentCd 로 원 위험성평가 건 추적.
 */
public record InsertIncidentCommand(
    String siteCd
    , String nearMissId
    , String incidentTypeCd
    , String processCd
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String immediateActionDesc
    , String srcProcessCd
    , String srcAssessmentCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static InsertIncidentCommand from(ReclassifyParam param, String nearMissId) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (nearMissId == null || nearMissId.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertIncidentCommand(
            param.siteCd()
            , nearMissId
            , param.incidentTypeCd()
            , param.processCd()
            , param.occurDtime()
            , param.locationDesc()
            , param.description()
            , param.potentialSeverityCd()
            , param.immediateActionDesc()
            , param.srcProcessCd()
            , param.srcAssessmentCd()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }

    /**
     * description 만 교체한 사본을 반환한다.
     * tb_near_miss.DESCRIPTION 은 NOT NULL 이므로, 원 평가건 경위가 비어있을 때 기본 문구로 대체하기 위함.
     */
    public static InsertIncidentCommand withDescription(InsertIncidentCommand source, String description) {

        if (source == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertIncidentCommand(
            source.siteCd()
            , source.nearMissId()
            , source.incidentTypeCd()
            , source.processCd()
            , source.occurDtime()
            , source.locationDesc()
            , description
            , source.potentialSeverityCd()
            , source.immediateActionDesc()
            , source.srcProcessCd()
            , source.srcAssessmentCd()
            , source.gvCmpnyCd()
            , source.gvUserCd()
        );
    }
}
