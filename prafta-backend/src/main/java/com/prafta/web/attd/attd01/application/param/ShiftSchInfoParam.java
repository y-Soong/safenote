package com.prafta.web.attd.attd01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd01.dto.request.ShiftSchInfoRequest;

public record ShiftSchInfoParam(
	    ShiftTypeParam shiftType
	    , List<ShiftPatternParam> shiftPatternList
	    , List<ShiftTeamParam> shiftTeamList
	    , List<ShiftAssignParam> shiftAssignList
	    , String gvCmpnyCd
	    , String gvUserCd
	) {
	    public static ShiftSchInfoParam from(ShiftSchInfoRequest request, TokenInfo tokenInfo) {

	        if (request == null)
	            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - ShiftSchInfoRequest");
	        if (tokenInfo == null)
	            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - TokenInfo");

	        return new ShiftSchInfoParam(
	            ShiftTypeParam.from(request.getShiftType())
	            , ShiftPatternParam.fromList(request.getShiftPatternList())
	            , ShiftTeamParam.fromList(request.getShiftTeamList())
	            , ShiftAssignParam.fromList(request.getShiftAssignList())
	            , tokenInfo.gv_cmpnyCd()
	            , tokenInfo.gv_userCd()
	        );
	    }

	    public record ShiftTypeParam(
	        String shiftNo
	        , String siteCd
	        , String shiftPtrnCnt
	        , String shiftTeamCnt
	        , String shiftCycleDays
	        , String useYn
	    ) {
	        public static ShiftTypeParam from(ShiftSchInfoRequest.ShiftType shiftType) {
	            if (shiftType == null)
	                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - ShiftType");

	            return new ShiftTypeParam(
	                shiftType.getShiftNo()
	                , shiftType.getSiteCd()
	                , shiftType.getShiftPtrnCnt()
	                , shiftType.getShiftTeamCnt()
	                , shiftType.getShiftCycleDays()
	                , shiftType.getUseYn()
	            );
	        }
	    }

	    public record ShiftPatternParam(
	        String siteCd
	        , String ptrnIdx
	        , String schCd
	    ) {
	        public static ShiftPatternParam from(ShiftSchInfoRequest.ShiftPattern pattern) {
	            if (pattern == null)
	                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - ShiftPattern");

	            return new ShiftPatternParam(
	                pattern.getSiteCd()
	                , pattern.getPtrnIdx()
	                , pattern.getSchCd()
	            );
	        }

	        public static List<ShiftPatternParam> fromList(List<ShiftSchInfoRequest.ShiftPattern> list) {
	            if (list == null) return null; // 정책에 따라 빈 리스트로 바꿔도 됨
	            return list.stream().map(ShiftPatternParam::from).toList();
	        }
	    }

	    public record ShiftTeamParam(
	        String siteCd
	        , String teamIdx
	        , String teamNm
	    ) {
	        public static ShiftTeamParam from(ShiftSchInfoRequest.ShiftTeam team) {
	            if (team == null)
	                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - ShiftTeam");

	            return new ShiftTeamParam(
	                team.getSiteCd()
	                , team.getTeamIdx()
	                , team.getTeamNm()
	            );
	        }

	        public static List<ShiftTeamParam> fromList(List<ShiftSchInfoRequest.ShiftTeam> list) {
	            if (list == null) return null;
	            return list.stream().map(ShiftTeamParam::from).toList();
	        }
	    }

	    public record ShiftAssignParam(
	        String siteCd
	        , String dayNo
	        , String teamIdx
	        , String assignYn
	        , String schCd
	    ) {
	        public static ShiftAssignParam from(ShiftSchInfoRequest.ShiftAssign assign) {
	            if (assign == null)
	                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - ShiftAssign");

	            return new ShiftAssignParam(
	                assign.getSiteCd()
	                , assign.getDayNo()
	                , assign.getTeamIdx()
	                , assign.getAssignYn()
	                , assign.getSchCd()
	            );
	        }

	        public static List<ShiftAssignParam> fromList(List<ShiftSchInfoRequest.ShiftAssign> list) {
	            if (list == null) return null;
	            return list.stream().map(ShiftAssignParam::from).toList();
	        }
	    }
	}
