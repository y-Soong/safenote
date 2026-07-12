package com.prafta.common.cmm.leave.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.leave.mapper.LeaveConversionPolicyMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.vo.LeaveConversionPolicyVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveConversionPolicyService} 구현 (연차 시간차 환산 개편 LC-02).
 *
 * <p>출처: 작업지시서_연차-시간차-환산-개편 T0·F4 / plan §1·§2 LC-02·§8-②
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveConversionPolicyServiceImpl implements LeaveConversionPolicyService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final LeaveConversionPolicyMapper leaveConversionPolicyMapper;

    @Override
    public int selectConversionMinutes(String cmpnyCd, String workYmd) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || workYmd == null || workYmd.isBlank()) {
            return DEFAULT_CONV_MINUTES;
        }
        Integer conv = leaveConversionPolicyMapper.selectConversionMinutes(cmpnyCd, workYmd);
        if (conv == null) {
            // 설정 미존재 회사 — 기본 480분(시드 불필요, 8시간 사업장 결과 불변)
            return DEFAULT_CONV_MINUTES;
        }
        // 저장 검증을 우회한 비정상 값(직접 DML 등) 방어 — R2 무반올림이 깨지면 원장 오염이므로 480 폴백
        if (conv < MIN_CONV_MINUTES || conv > MAX_CONV_MINUTES
                || !HourlyLeaveChargeUtils.isTerminatingConvMinutes(conv)) {
            log.warn("[leave-conv] 비정상 환산시간 감지 — 480 폴백. cmpnyCd={}, workYmd={}, conv={}",
                    cmpnyCd, workYmd, conv);
            return DEFAULT_CONV_MINUTES;
        }
        return conv;
    }

    @Override
    public List<LeaveConversionPolicyVO> findHistory(String cmpnyCd, String authCd) {
        ensureManager(authCd, cmpnyCd, "conversion-history");
        if (cmpnyCd == null || cmpnyCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return leaveConversionPolicyMapper.selectConversionHistory(cmpnyCd);
    }

    @Override
    @Transactional
    public void savePolicy(String cmpnyCd, String applyFromDate, Integer dailyConvMinutes,
                           String authCd, String userCd) {
        ensureManager(authCd, cmpnyCd, "conversion-save");
        if (cmpnyCd == null || cmpnyCd.isBlank() || userCd == null || userCd.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 적용일: YYYYMMDD 실존 날짜 + 오늘 이후만 (F4 — 소급 재계산 없음과 정합)
        if (!isValidYyyymmdd(applyFromDate) || applyFromDate.compareTo(todayYmd()) < 0) {
            // 보안리뷰(Low): 검증 실패한 입력 원문은 로그에 남기지 않는다 — 길이/사유 요약만.
            log.info("[leave-conv] 환산시간 저장 거부: 적용일 비정상/과거. cmpnyCd={}, 입력길이={}, 사유={}",
                    cmpnyCd, applyFromDate == null ? 0 : applyFromDate.length(),
                    isValidYyyymmdd(applyFromDate) ? "과거일" : "형식오류");
            throw new ApiException(AttdErrorCode.ATTD_400_190);
        }
        // 유효범위 60~1440분 (plan §8-②)
        if (dailyConvMinutes == null
                || dailyConvMinutes < MIN_CONV_MINUTES || dailyConvMinutes > MAX_CONV_MINUTES) {
            throw new ApiException(AttdErrorCode.ATTD_400_191);
        }
        // R2 무반올림 방어: 30분 단위 신청분이 소수 5자리 안에서 정확히 나누어떨어지는 값만 허용
        if (!HourlyLeaveChargeUtils.isTerminatingConvMinutes(dailyConvMinutes)) {
            log.info("[leave-conv] 환산시간 저장 거부: 비종결 소수 발생 값. cmpnyCd={}, conv={}",
                    cmpnyCd, dailyConvMinutes);
            throw new ApiException(AttdErrorCode.ATTD_400_192);
        }

        // 같은 적용일 재저장은 UPDATE, 없으면 INSERT (PK 경합 시 재-UPDATE 1회)
        int updated = leaveConversionPolicyMapper.updateConversionPolicy(
                cmpnyCd, applyFromDate, dailyConvMinutes, userCd);
        if (updated == 0) {
            try {
                leaveConversionPolicyMapper.insertConversionPolicy(
                        cmpnyCd, applyFromDate, dailyConvMinutes, userCd);
            } catch (DuplicateKeyException e) {
                // 동시 저장 경합(TOCTOU) — 같은 적용일 행이 방금 생겼으므로 UPDATE 로 수렴
                log.info("[leave-conv] 환산시간 저장 PK 경합 — UPDATE 재시도. cmpnyCd={}, applyFromDate={}",
                        cmpnyCd, applyFromDate);
                leaveConversionPolicyMapper.updateConversionPolicy(
                        cmpnyCd, applyFromDate, dailyConvMinutes, userCd);
            }
        }

        log.info("[leave-conv] 환산시간 저장 완료. cmpnyCd={}, applyFromDate={}, conv={}분, 수행자={}",
                cmpnyCd, applyFromDate, dailyConvMinutes, userCd);
    }

    /** 권한 가드 — LeavePolicyServiceImpl.ensureManager 미러(§8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER). */
    private void ensureManager(String authCd, String cmpnyCd, String operation) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("[leave-conv] {} 권한 없음. cmpnyCd={}, authCd={}", operation, cmpnyCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_011);
        }
    }

    /** YYYYMMDD 실존 날짜 여부. */
    private boolean isValidYyyymmdd(String ymd) {
        if (ymd == null || ymd.length() != 8) {
            return false;
        }
        try {
            LocalDate.parse(ymd, YMD);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String todayYmd() {
        return LocalDate.now().format(YMD);
    }
}
