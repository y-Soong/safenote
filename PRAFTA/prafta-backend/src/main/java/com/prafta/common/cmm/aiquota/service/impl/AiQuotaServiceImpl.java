package com.prafta.common.cmm.aiquota.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.aiquota.application.result.AiQuotaStatusResult;
import com.prafta.common.cmm.aiquota.mapper.AiQuotaMapper;
import com.prafta.common.cmm.aiquota.service.AiQuotaService;
import com.prafta.common.error.ai.AiErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회사별 월간 AI 토큰 쿼터 서비스 구현.
 *
 * <p>★USE_YM 은 명시적 KST 로 산출한다(서버 TZ 무관 — WORK_DATE UTC 사고 전례:
 *    메모리 project_prafta_chklst_workdate_utc_bug).
 * <p>★동시 호출 소프트 리밋: check 와 record 사이 비원자성으로 한도를 약간 초과할 수 있으나
 *    이는 명세(§5)다. 행 잠금/직렬화를 하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuotaServiceImpl implements AiQuotaService {

    private final AiQuotaMapper aiQuotaMapper;

    /** 월 기본 한도(QUOTA 행 미존재 시 적용 — 요청서 §2-1/9). */
    private static final long DEFAULT_MONTHLY_TOKEN_LIMIT = 800_000L;

    /** 사용 연월 기준 타임존(KST 고정). */
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    /** USE_YM 포맷(YYYYMM). */
    private static final DateTimeFormatter USE_YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public void checkOrThrow(String cmpnyCd) {
        if (isExceeded(cmpnyCd)) {
            throw new ApiException(AiErrorCode.AI_429_001);
        }
    }

    @Override
    public boolean isExceeded(String cmpnyCd) {
        String useYm = currentUseYm();
        AiQuotaStatusResult status = aiQuotaMapper.selectQuotaStatus(cmpnyCd, useYm);

        // 유효한도: QUOTA 행 존재 → 그 값(-1 무제한 / 0 즉시 차단 / 양수), 미존재 → 기본 800,000.
        long effectiveLimit = (status == null || status.monthlyTokenLimit() == null)
            ? DEFAULT_MONTHLY_TOKEN_LIMIT
            : status.monthlyTokenLimit();
        if (effectiveLimit < 0) {
            return false;   // -1 무제한
        }

        long usedTokens = (status == null || status.usedTokens() == null) ? 0L : status.usedTokens();
        // 차단 조건: 사용량이 한도 이상(§5 경계: 한도 직전 사용량이면 호출 허용 → 다음 호출부터 차단.
        //   한도 0 이면 0 이상 → 즉시 차단).
        boolean exceeded = usedTokens >= effectiveLimit;
        if (exceeded) {
            log.info("AI 토큰 쿼터 소진 판정 - cmpnyCd={}, useYm={}, used={}, limit={}",
                cmpnyCd, useYm, usedTokens, effectiveLimit);
        }
        return exceeded;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String cmpnyCd, long inputTokens, long outputTokens) {
        try {
            aiQuotaMapper.upsertUsage(cmpnyCd, currentUseYm(), inputTokens, outputTokens);
        } catch (Exception e) {
            // ★호출자 응답 훼손 금지 — 삼킨다. 단 이 WARN 프리픽스는 §6 가시화 요건: grep 가능 고정 문구 유지.
            log.warn("AI 토큰 사용량 기록 실패(쿼터 무력화 위험) - cmpnyCd={}, in={}, out={}, 원인={}",
                cmpnyCd, inputTokens, outputTokens, e.getMessage());
        }
    }

    /** 당월 사용 연월(YYYYMM, KST). */
    private String currentUseYm() {
        return LocalDate.now(ZONE_KST).format(USE_YM_FMT);
    }
}
