package com.prafta.common.cmm.dailyuser.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.dailyuser.mapper.DailyUserMapper;
import com.prafta.common.cmm.dailyuser.service.DailyUserExpireService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-app-027-1 — 일용직 만료 계정 일괄 비활성 서비스 구현.
 *
 * <p>정책서: {@code .claude/context/policies/common/03-account-auth.md} §3.5(미사용=로그인 차단),
 * {@code common/05-slot-management.md} §5.6(자정 만료 배치).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyUserExpireServiceImpl implements DailyUserExpireService {

    private final DailyUserMapper dailyUserMapper;

    private static final String SYSTEM_UPDATE_NO = "SYSTEM";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public int expireOverdueDailyUsers() {
        // 서버 기준 오늘(Asia/Seoul) — WORK_EXPIRE_DATE 가 오늘보다 과거인 계정이 대상.
        String todayYmd = LocalDate.now().format(YMD);

        // 만료 시 자원 반납 + 비활성 전이를 한 트랜잭션 안에서 처리.
        // 만료 대상은 모두 TB_DAILY_USER.WORK_EXPIRE_DATE < 오늘 으로 판정하므로(D 의 USE_YN 전이와 무관),
        // 슬롯/TB_USER/SITE_AUTH 반납을 먼저 수행한 뒤 마지막에 TB_DAILY_USER 를 비활성 전이한다.

        // 1) 슬롯 반납(고정슬롯 제외, 멱등).
        int slotReleased = dailyUserMapper.releaseExpiredDailyUserSlots(todayYmd, SYSTEM_UPDATE_NO);

        // 2) 통합형 — TB_USER 비활성(EMPLOYMENT_TYPE='DAILY' 가드, 멱등).
        int tbUserAffected = dailyUserMapper.deactivateExpiredTbUser(todayYmd, SYSTEM_UPDATE_NO);

        // 3) 통합형 — TB_USER_SITE_AUTH 비활성(EMPLOYMENT_TYPE='DAILY' 가드, 멱등).
        int siteAuthAffected = dailyUserMapper.deactivateExpiredTbUserSiteAuth(todayYmd, SYSTEM_UPDATE_NO);

        // 4) TB_DAILY_USER 비활성 전이(USE_YN='N' + ACCOUNT_STATUS='05', 멱등).
        int dailyAffected = dailyUserMapper.updateExpireDailyUsers(todayYmd, SYSTEM_UPDATE_NO);

        log.info("일용직 만료 처리 — 기준일={}, 슬롯반납={}, TB_USER 비활성={}, SITE_AUTH 비활성={}, TB_DAILY_USER 비활성={}",
                todayYmd, slotReleased, tbUserAffected, siteAuthAffected, dailyAffected);
        // 반환은 기존대로 TB_DAILY_USER 처리 건수 유지.
        return dailyAffected;
    }
}
