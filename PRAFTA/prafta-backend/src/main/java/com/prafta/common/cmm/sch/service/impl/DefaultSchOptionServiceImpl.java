package com.prafta.common.cmm.sch.service.impl;

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

    private final DefaultSchGenMapper defaultSchGenMapper;

    @Override
    public List<SchOptionVO> getActiveSchOptions(String cmpnyCd, String siteCd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd)) {
            return List.of();
        }
        return defaultSchGenMapper.selectActiveSchOptions(cmpnyCd, siteCd);
    }

    @Override
    public boolean isValidDefaultSch(String cmpnyCd, String siteCd, String schCd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd) || !StringUtils.hasText(schCd)) {
            return false;
        }
        return defaultSchGenMapper.countActiveSchOnSite(cmpnyCd, siteCd, schCd) > 0;
    }
}
