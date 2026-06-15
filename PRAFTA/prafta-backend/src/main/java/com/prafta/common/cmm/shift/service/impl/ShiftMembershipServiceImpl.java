package com.prafta.common.cmm.shift.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.shift.mapper.ShiftMembershipMapper;
import com.prafta.common.cmm.shift.service.ShiftMembershipService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 교대팀 소속 일자 판정 구현 (PRAFTA-COM-008-E-7 / D-1·D-2).
 *
 * <p>D-1 정밀화: 팀 마스터 적용기간 ∩ 멤버십 행 존재 ∩ per-user 탈퇴일(LEAVE_TEAM_YMD) 미포함.
 * D-2: 교대 잠금 가드(assertNotShiftLocked / assertNotShiftLockedInMonth)를 추가했다(시그니처 isInShiftTeamOn 불변).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftMembershipServiceImpl implements ShiftMembershipService {

    private final ShiftMembershipMapper shiftMembershipMapper;

    @Override
    public boolean isInShiftTeamOn(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(workYmd)) {
            return false;
        }
        return shiftMembershipMapper.countShiftMembershipOn(cmpnyCd, userCd, workYmd) > 0;
    }

    @Override
    public void assertNotShiftLocked(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        if (isInShiftTeamOn(cmpnyCd, siteCd, userCd, workYmd)) {
            log.info("[prafta-com-008-D] 교대 잠금 차단 — userCd={}, workYmd={}", userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_160);
        }
    }

    @Override
    public void assertNotShiftLockedInMonth(String cmpnyCd, String siteCd, String userCd, String workYm) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd) || !StringUtils.hasText(workYm)) {
            return;
        }
        if (shiftMembershipMapper.existsShiftLockInMonth(cmpnyCd, siteCd, userCd, workYm) > 0) {
            log.info("[prafta-com-008-D] 교대 잠금 차단(월 단위) — userCd={}, workYm={}", userCd, workYm);
            throw new ApiException(AttdErrorCode.ATTD_400_160);
        }
    }
}
