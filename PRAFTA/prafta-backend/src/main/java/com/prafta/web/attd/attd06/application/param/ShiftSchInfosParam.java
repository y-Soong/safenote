package com.prafta.web.attd.attd06.application.param;

import java.util.List;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.dto.request.ShiftSchInfosRequest;

public record ShiftSchInfosParam(
    ShiftMetaParam shiftMeta
    , List<TeamParam> teamList
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
) {
    public static ShiftSchInfosParam from(ShiftSchInfosRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ShiftSchInfosParam(
            ShiftMetaParam.from(request.getShiftMeta())
            , TeamParam.fromList(request.getTeamList())
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }

    public record ShiftMetaParam(
        String siteCd
        , String shiftCd
        , String shiftTeamNm
        , String startDate
        , String endDate
    ) {
        public static ShiftMetaParam from(ShiftSchInfosRequest.ShiftMeta obj) {
            if (obj == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return new ShiftMetaParam(
                obj.getSiteCd()
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
        public static TeamParam from(ShiftSchInfosRequest.Team obj) {
            if (obj == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return new TeamParam(
                obj.getTeamIdx()
                , obj.getTeamNm()
                , MemberParam.fromList(obj.getMembers())
            );
        }

        public static List<TeamParam> fromList(List<ShiftSchInfosRequest.Team> list) {
            if (list == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return list.stream().map(TeamParam::from).toList();
        }
    }

    public record MemberParam(
        String userCd
        , String leaderYn
    ) {
        public static MemberParam from(ShiftSchInfosRequest.Member obj) {
            if (obj == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return new MemberParam(
                obj.getUserCd()
                , obj.getLeaderYn()
            );
        }

        public static List<MemberParam> fromList(List<ShiftSchInfosRequest.Member> list) {
            if (list == null)
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            return list.stream().map(MemberParam::from).toList();
        }
    }
}