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

	/**
	 * prafta-com-013-05-2: 교대팀 기간 연장 시 연장 구간만 근무계획을 생성하기 위한 팩토리.
	 *   - 교대 패턴의 순환 위상(offset)은 반드시 팀의 "원래 시작일(cycleAnchorYmd)" 기준으로 계산해야
	 *     기존 구간과 패턴이 어긋나지 않는다(genStartYmd 기준이 아님).
	 *   - 생성 모델은 [genStartYmd, genEndYmd] 구간만 포함한다.
	 *   - schCd 정규화(OFF/blank -> null)는 기존 from 메서드와 동일.
	 */
	public static UserWorkPlanCommand fromRange(
			String userCd
			, String siteCd
			, String cycleAnchorYmd
			, String genStartYmd
			, String genEndYmd
			, List<String> schCdPattern
			, String teamIdx
			, String gvCmpnyCd
			, String gvUserCd) {

		if (userCd == null || userCd.isBlank())
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (siteCd == null || siteCd.isBlank())
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

		LocalDate anchor;
		LocalDate genStart;
		LocalDate genEnd;
		try {
			anchor   = LocalDate.parse(cycleAnchorYmd, fmt);
			genStart = LocalDate.parse(genStartYmd,    fmt);
			genEnd   = LocalDate.parse(genEndYmd,      fmt);
		} catch (Exception e) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		if (genStart.isAfter(genEnd))
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		List<UserWorkPlanModel> models = new ArrayList<>();
		int offset = teamIdxInt - 1;

		for (LocalDate d = genStart; !d.isAfter(genEnd); d = d.plusDays(1)) {
			// 순환 위상 = 원래 시작일(anchor)로부터의 경과 일수.
			//   genStart 가 anchor 보다 이전(앞쪽 연장)이면 dayIndex 가 음수가 될 수 있으나
			//   아래 모듈러 식이 음수도 정상 처리하므로 기존 구간과 패턴 위상이 일치한다.
			long dayIndex = java.time.temporal.ChronoUnit.DAYS.between(anchor, d);
			int patternIndex = (int) (((dayIndex - offset) % patternSize + patternSize) % patternSize);
			String rawSchCd = schCdPattern.get(patternIndex);

			// OFF / blank -> null 로 정규화하여 upsert
			String schCd;
			if (rawSchCd == null || rawSchCd.isBlank() || "OFF".equalsIgnoreCase(rawSchCd)) {
				schCd = null;
			} else {
				schCd = rawSchCd;
			}

			models.add(new UserWorkPlanModel(
				gvCmpnyCd
				, siteCd
				, userCd
				, d.format(fmt)
				, schCd
				, gvUserCd
			));
		}

		return new UserWorkPlanCommand(models);
	}
}