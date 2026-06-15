package com.prafta.common.cmm.push.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * PUSH 생산자(트리거/스케줄러)의 수신 대상 산출 전용 공용 Mapper (PRAFTA-APP-021).
 *
 * <p>특정 도메인 모듈에 종속되지 않는 "PUSH 대상 USER_CD 목록" 조회만 모은다.
 * outbox 채번/INSERT 는 {@code LeaveDashboardMapper} 를 재사용하므로 본 매퍼는 미보유.
 * 모든 조회는 CMPNY_CD 스코프로 격리한다.
 */
@Mapper
public interface PushTargetMapper {

    /**
     * TBM 세션의 <b>실제 입실(enter)한 참석자</b> USER_CD 목록 (W3, PRAFTA-APP-021-3b).
     *
     * <p>대상 = TB_TBM_ATTENDANCE 의 해당 세션 행 중 ENTRY_AT IS NOT NULL(입실 완료), DEL_YN='N'.
     * REGULAR/DAILY 무관 전부 포함한다("참석 예정 대상자" 개념 없음 — 입실자만, §8-R 2).
     * USER_CD 기준 DISTINCT. 입실자가 없으면 빈 리스트.
     *
     * @param cmpnyCd   회사 코드 (CMPNY_CD 스코프)
     * @param sessionCd TBM 세션 코드
     */
    List<String> selectTbmEnteredUserCds(@Param("cmpnyCd") String cmpnyCd,
                                         @Param("sessionCd") String sessionCd);
}
