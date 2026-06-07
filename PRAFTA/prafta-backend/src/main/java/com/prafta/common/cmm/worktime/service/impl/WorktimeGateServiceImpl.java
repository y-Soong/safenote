package com.prafta.common.cmm.worktime.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.worktime.mapper.WorktimeGateMapper;
import com.prafta.common.cmm.worktime.service.WorktimeGateService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.worktime.WorktimeErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-022: 근무중 게이트(WorktimeGate) 공용 서비스 구현.
 *
 * <p>오늘 일자는 DB NOW() 기준으로 조회하여(자정 경계 일관) 당일 본인 열린 근태 건수로 판정한다.
 * 식별값(cmpnyCd/siteCd/userCd)은 전부 {@link TokenInfo} 출처만 사용한다(IDOR 차단).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorktimeGateServiceImpl implements WorktimeGateService {

    private final WorktimeGateMapper worktimeGateMapper;

    @Override
    public boolean isWorking(TokenInfo token) {

        if (token == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        String cmpnyCd = token.gv_cmpnyCd();
        String siteCd = token.gv_siteCd();
        String userCd = token.gv_userCd();

        // 사업장 미선택/토큰 손상이면 식별 불가 → 근무중으로 볼 수 없음(차단 측 안전).
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd) || !StringUtils.hasText(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        // 오늘 일자는 DB NOW() 기준(자정 경계에서 판정/데이터 일자 불일치 방지).
        String todayYmd = worktimeGateMapper.selectTodayYmd();

        int openCount = worktimeGateMapper.countOpenAttdToday(cmpnyCd, siteCd, userCd, todayYmd);
        return openCount > 0;
    }

    @Override
    public void assertWorking(TokenInfo token) {

        if (!isWorking(token)) {
            // PII(이름 등) 미노출 — userCd 만 로깅.
            log.info("[worktimeGate] 근무중 아님 차단: userCd={}", token == null ? null : token.gv_userCd());
            throw new ApiException(WorktimeErrorCode.WORKTIME_403_001);
        }
    }
}
