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
) {
    public static ShiftSchInfosParam from(ShiftSchInfosRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - ShiftSchInfosRequest");
        if (tokenInfo == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - TokenInfo");

        return new ShiftSchInfosParam(
            ShiftMetaParam.from(request.getShiftMeta())
            , TeamParam.fromList(request.getTeamList())
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
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
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - ShiftMeta");
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
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - Team");
            return new TeamParam(
                obj.getTeamIdx()
                , obj.getTeamNm()
                , MemberParam.fromList(obj.getMembers())
            );
        }

        public static List<TeamParam> fromList(List<ShiftSchInfosRequest.Team> list) {
            if (list == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - TeamList");
            return list.stream().map(TeamParam::from).toList();
        }
    }

    public record MemberParam(
        String userCd
        , String leaderYn
    ) {
        public static MemberParam from(ShiftSchInfosRequest.Member obj) {
            if (obj == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - Member");
            return new MemberParam(
                obj.getUserCd()
                , obj.getLeaderYn()
            );
        }

        public static List<MemberParam> fromList(List<ShiftSchInfosRequest.Member> list) {
            if (list == null)
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\nRequired param missing - MemberList");
            return list.stream().map(MemberParam::from).toList();
        }
    }
}