package com.prafta.common.cmm.sch.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper;
import com.prafta.common.cmm.sch.service.DefaultSchOptionService;
import com.prafta.common.cmm.sch.vo.SchOptionVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기본 근무타입 옵션 조회/검증 구현 (PRAFTA-COM-008-E-5/E-8).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSchOptionServiceImpl implements DefaultSchOptionService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DefaultSchGenMapper defaultSchGenMapper;

    @Override
    public List<SchOptionVO> getActiveSchOptions(String cmpnyCd, String siteCd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd)) {
            return List.of();
        }
        return defaultSchGenMapper.selectActiveSchOptions(cmpnyCd, siteCd);
    }

    /**
     * 2026-08-22: 적용일자 검증 추가 — 본 메서드의 전 호출부(계정 생성/수정, 웹·앱 본인
     * 기본근무타입 변경, 로그인 게이트)는 전부 {@code DefaultSchGenServiceImpl.applyDefaultSchChange}
     * 의 "명일(오늘+1)부터 반영" 규칙을 공유하므로, 기준일을 명일로 고정해 판정한다(호출부 파라미터
     * 불필요 — 소속이동처럼 임의 미래일을 받는 흐름은 UserTransferMapper.selectSchUsableOnDate 로
     * 별도 처리).
     */
    @Override
    public boolean isValidDefaultSch(String cmpnyCd, String siteCd, String schCd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd) || !StringUtils.hasText(schCd)) {
            return false;
        }
        String tomorrowYmd = LocalDate.now().plusDays(1).format(YMD);
        return defaultSchGenMapper.countActiveSchOnSite(cmpnyCd, siteCd, schCd, tomorrowYmd) > 0;
    }
}
