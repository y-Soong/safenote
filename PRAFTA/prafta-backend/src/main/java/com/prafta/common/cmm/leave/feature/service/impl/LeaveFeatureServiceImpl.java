package com.prafta.common.cmm.leave.feature.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.feature.mapper.LeaveFeatureMapper;
import com.prafta.common.cmm.leave.feature.service.LeaveFeatureService;
import com.prafta.common.cmm.leave.feature.vo.LeaveFeatureVisibilityVO;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursSummaryVO;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveFeatureService} 구현체 (소정-06).
 *
 * <p>읽기 전용이므로 {@code @Transactional} 을 부여하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveFeatureServiceImpl implements LeaveFeatureService {

    private final LeaveFeatureMapper leaveFeatureMapper;
    private final LeavePolicyService leavePolicyService;
    private final StdWorkHoursService stdWorkHoursService;

    @Override
    public LeaveFeatureVisibilityVO resolveVisibility(String cmpnyCd) {
        requireCmpnyCd(cmpnyCd);

        // ★기본값 가드: 활성 정책이 없거나 컬럼이 'N' 이 아니면 true(기존 동작).
        boolean autoGrantEnabled = leavePolicyService.isStatutoryAutoGrantEnabled(cmpnyCd);

        // 토글이 켜져 있으면 이력을 조회할 필요가 없다(항상 노출) — 불필요한 쿼리 1회를 줄인다.
        boolean grantHistoryExists = autoGrantEnabled || leaveFeatureMapper.existsLeaveGrant(cmpnyCd) > 0;

        // 숨김 = 토글 off AND 부여 이력 0 (지시서 표 그대로).
        boolean visible = autoGrantEnabled || grantHistoryExists;

        if (!visible) {
            log.info("연차 기능 노출 판정 — 숨김(자동 부여 off + 부여 이력 0). cmpnyCd={}", cmpnyCd);
        }

        return LeaveFeatureVisibilityVO.builder()
                .cmpnyCd(cmpnyCd)
                .statutoryAutoGrantEnabled(autoGrantEnabled)
                .grantHistoryExists(grantHistoryExists)
                .leaveFeatureVisible(visible)
                .build();
    }

    @Override
    public StdWorkHoursSummaryVO resolveMyStdWorkSummary(String cmpnyCd, String userCd, String baseYmd) {
        requireCmpnyCd(cmpnyCd);
        if (userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        // 0단계 경계: 판정·부여 로직에 연결하지 않고 소정-02 서비스의 해석 결과를 그대로 노출한다.
        return stdWorkHoursService.resolveSummary(cmpnyCd, userCd, normalizeBaseYmd(baseYmd));
    }

    private void requireCmpnyCd(String cmpnyCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /** 기준일 정규화 — null/공백/형식오류(YYYYMMDD 8자리 숫자 아님)는 오늘로 대체한다. */
    private String normalizeBaseYmd(String baseYmd) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (baseYmd == null || baseYmd.isBlank()) {
            return today;
        }
        String trimmed = baseYmd.trim();
        if (trimmed.length() != 8 || !trimmed.chars().allMatch(Character::isDigit)) {
            return today;
        }
        try {
            LocalDate.parse(trimmed, DateTimeFormatter.BASIC_ISO_DATE);
            return trimmed;
        } catch (Exception ignore) {
            return today;
        }
    }
}
