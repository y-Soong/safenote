package com.prafta.common.cmm.site.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.site.mapper.SiteExpireMapper;
import com.prafta.common.cmm.site.service.SiteExpireService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-COM-001-T2-3 — 사업장 만료 일괄 비활성 서비스 구현.
 *
 * <p>정책서: {@code .claude/context/policies/common} §6.1(종료일 경과 시 자동 비활성화),
 * §3.5(미사용=로그인 차단의 데이터 전제).
 *
 * <p>{@code DailyUserExpireServiceImpl} 패턴 미러. 종료일(END_DATE varchar(8) YYYYMMDD)이
 * 오늘 이하인 활성 사업장을 USE_YN='N' 으로 멱등 전이한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteExpireServiceImpl implements SiteExpireService {

    private final SiteExpireMapper siteExpireMapper;

    private static final String SYSTEM_UPDATE_NO = "SYSTEM";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public int expireOverdueSites() {
        // 서버 기준 오늘(Asia/Seoul). END_DATE <= 오늘 인 활성 사업장이 대상(당일 종료 포함, A안과 정합).
        String todayYmd = LocalDate.now().format(YMD);

        int affected = siteExpireMapper.updateExpireOverdueSites(todayYmd, SYSTEM_UPDATE_NO);

        log.info("사업장 만료 비활성 처리 — 기준일={}, 비활성 전이 건수={}", todayYmd, affected);
        return affected;
    }
}
