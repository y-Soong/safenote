package com.prafta.web.attd.attd06.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.ShiftUserSchInfosRequest;

public record ShiftUserSchInfosParam(
    ShiftMetaParam shiftMeta
    , List<TeamParam> teamList
    , String gvCmpnyCd
    , String gvUserCd
) {
    public static ShiftUserSchInfosParam from(ShiftUserSchInfosRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftUserSchInfosParam(
            ShiftMetaParam.from(request.getShiftMeta())
            , TeamParam.fromList(request.getTeamList())
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
        );
    }

    public record ShiftMetaParam(
   		String shiftTeamId
        , String siteCd
        , String shiftCd
        , String shiftTeamNm
        , String startDate
        , String endDate
    ) {
        public static ShiftMetaParam from(ShiftUserSchInfosRequest.ShiftMeta obj) {
            if (obj == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return new ShiftMetaParam(
            	obj.getShiftTeamId()
                , obj.getSiteCd()
                , obj.getShiftCd()
                , obj.getShiftTeamNm()
                , obj.getStartDate()
                , obj.getEndDate()
            );
        }
    }

    public record TeamParam(
        String teamIdx
        , String teamNm
        , List<MemberParam> memberList
    ) {
        public static TeamParam from(ShiftUserSchInfosRequest.Team obj) {
            if (obj == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return new TeamParam(
                obj.getTeamIdx()
                , obj.getTeamNm()
                , MemberParam.fromList(obj.getMembers())
            );
        }

        public static List<TeamParam> fromList(List<ShiftUserSchInfosRequest.Team> list) {
            if (list == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return list.stream().map(TeamParam::from).toList();
        }
    }

    public record MemberParam(
        String userCd
    ) {
        public static MemberParam from(ShiftUserSchInfosRequest.Member obj) {
            if (obj == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return new MemberParam(
                obj.getUserCd()
            );
        }

        public static List<MemberParam> fromList(List<ShiftUserSchInfosRequest.Member> list) {
            if (list == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return list.stream().map(MemberParam::from).toList();
        }
    }
}