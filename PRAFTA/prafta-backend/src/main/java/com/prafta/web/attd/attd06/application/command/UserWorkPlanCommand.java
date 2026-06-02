package com.prafta.web.attd.attd06.application.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd06.application.model.UserWorkPlanModel;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftUserSchInfosParam;

public record UserWorkPlanCommand(
    List<UserWorkPlanModel> userWorkPlanModelList
) {
	public static UserWorkPlanCommand from(
			ShiftSchInfosParam.MemberParam memberParam
	        , ShiftSchInfosParam.ShiftMetaParam shiftMeta
	        , List<String> schCdPattern
	        , String teamIdx
	        , String gvCmpnyCd
	        , String gvUserCd) {

	    if (memberParam == null)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    if (shiftMeta == null)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    if (schCdPattern == null || schCdPattern.isEmpty())
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    if (teamIdx == null || teamIdx.isBlank())
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    int teamIdxInt;
	    try {
	        teamIdxInt = Integer.parseInt(teamIdx.trim());
	    } catch (NumberFormatException e) {
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    }

	    if (teamIdxInt < 1)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    int patternSize = schCdPattern.size();

	    if (teamIdxInt > patternSize)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    DateTimeFormatter fmt = DateTimeFormatter.BASIC_ISO_DATE;

	    LocalDate start;
	    LocalDate end;
	    try {
	        start = LocalDate.parse(shiftMeta.startDate(), fmt);
	        end   = LocalDate.parse(shiftMeta.endDate(),   fmt);
	    } catch (Exception e) {
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    }

	    if (start.isAfter(end))
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    List<UserWorkPlanModel> models = new ArrayList<>();
	    int offset = teamIdxInt - 1;
	    int dayIndex = 0;

	    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1), dayIndex++) {
	        int patternIndex = ((dayIndex - offset) % patternSize + patternSize) % patternSize;
	        String rawSchCd = schCdPattern.get(patternIndex);

	        // OFF / blank → null 로 정규화하여 upsert
	        String schCd;
	        if (rawSchCd == null || rawSchCd.isBlank() || "OFF".equalsIgnoreCase(rawSchCd)) {
	            schCd = null;
	        } else {
	            schCd = rawSchCd;
	        }

	        models.add(new UserWorkPlanModel(
	            gvCmpnyCd
	            , shiftMeta.siteCd()
	            , memberParam.userCd()
	            , d.format(fmt)
	            , schCd
	            , gvUserCd
	        ));
	    }

	    return new UserWorkPlanCommand(models);
	}

	public static UserWorkPlanCommand from(
			ShiftUserSchInfosParam.MemberParam memberParam
	        , ShiftUserSchInfosParam.ShiftMetaParam shiftMeta
	        , List<String> schCdPattern
	        , String teamIdx
	        , String gvCmpnyCd
	        , String gvUserCd) {

	    if (memberParam == null)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    if (shiftMeta == null)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    if (schCdPattern == null || schCdPattern.isEmpty())
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    if (teamIdx == null || teamIdx.isBlank())
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    int teamIdxInt;
	    try {
	        teamIdxInt = Integer.parseInt(teamIdx.trim());
	    } catch (NumberFormatException e) {
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    }

	    if (teamIdxInt < 1)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    int patternSize = schCdPattern.size();

	    if (teamIdxInt > patternSize)
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    DateTimeFormatter fmt = DateTimeFormatter.BASIC_ISO_DATE;

	    LocalDate start;
	    LocalDate end;
	    try {
	        start = LocalDate.parse(shiftMeta.startDate(), fmt);
	        end   = LocalDate.parse(shiftMeta.endDate(),   fmt);
	    } catch (Exception e) {
	        throw new ApiException(CommonErrorCode.COMMON_400_001);
	    }

	    if (start.isAfter(end))
	        throw new ApiException(CommonErrorCode.COMMON_400_001);

	    List<UserWorkPlanModel> models = new ArrayList<>();
	    int offset = teamIdxInt - 1;
	    int dayIndex = 0;

	    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1), dayIndex++) {
	        int patternIndex = ((dayIndex - offset) % patternSize + patternSize) % patternSize;
	        String rawSchCd = schCdPattern.get(patternIndex);

	        // OFF / blank → null 로 정규화하여 upsert
	        String schCd;
	        if (rawSchCd == null || rawSchCd.isBlank() || "OFF".equalsIgnoreCase(rawSchCd)) {
	            schCd = null;
	        } else {
	            schCd = rawSchCd;
	        }

	        models.add(new UserWorkPlanModel(
	            gvCmpnyCd
	            , shiftMeta.siteCd()
	            , memberParam.userCd()
	            , d.format(fmt)
	            , schCd
	            , gvUserCd
	        ));
	    }

	    return new UserWorkPlanCommand(models);
	}
}