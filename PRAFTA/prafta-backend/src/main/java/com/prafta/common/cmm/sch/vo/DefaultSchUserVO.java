package com.prafta.common.cmm.sch.vo;

/**
 * 기본근무 자동 스케줄 생성 대상 사용자 메타 (PRAFTA-COM-008-E-3).
 *
 * <p>매핑 대상: DefaultSchGenMapper.selectDefaultSchUsers / selectDefaultSchUser.
 * <p>⚠️ MyBatis 위치매핑 — record 필드 순서 = SELECT 컬럼 순서.
 */
public record DefaultSchUserVO(
        String cmpnyCd
        , String siteCd
        , String userCd
        , String defaultSchCd
) {
}
