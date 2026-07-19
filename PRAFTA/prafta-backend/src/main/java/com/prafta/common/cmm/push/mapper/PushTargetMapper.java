package com.prafta.common.cmm.push.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.push.result.TbmPushTargetRow;

/**
 * PUSH 생산자(트리거/스케줄러)의 수신 대상 산출 전용 공용 Mapper (PRAFTA-APP-021).
 *
 * <p>특정 도메인 모듈에 종속되지 않는 "PUSH 대상" 조회만 모은다.
 * outbox 채번/INSERT 는 {@code LeaveDashboardMapper} 를 재사용하므로 본 매퍼는 미보유.
 */
@Mapper
public interface PushTargetMapper {

    /**
     * TBM 세션의 <b>실제 입실(enter)한 참석자</b> 목록 (W3, PRAFTA-APP-021-3b + SUBCON-T5 F6).
     *
     * <p>대상 = TB_TBM_ATTENDANCE 의 해당 세션 행 중 ENTRY_AT IS NOT NULL(입실 완료), DEL_YN='N'.
     * REGULAR/DAILY 무관 전부 포함한다("참석 예정 대상자" 개념 없음 — 입실자만, §8-R 2).
     *
     * <p><b>T5 F6</b>: 참석자 회사 조건을 제거해 <b>타사(지정 체인) 참석자에게도</b> 교육 시작/종료
     * 푸시가 가도록 한다. 대신 세션이 {@code hostCmpnyCd}(개설사) 소유임을 SQL 안에서 검증한다
     * (소유 검증 없이 스코프만 넓히면 타사 세션 참석자 명단이 새어나간다).
     * 반환 행은 (참석자 회사코드, USER_CD) 쌍이다 — USER_CD 는 회사별 채번이라 회사코드 동반 필수.
     *
     * @param hostCmpnyCd 세션 개설사 회사코드(소유 검증)
     * @param sessionCd   TBM 세션 코드
     */
    List<TbmPushTargetRow> selectTbmEnteredTargets(@Param("hostCmpnyCd") String hostCmpnyCd,
                                                   @Param("sessionCd") String sessionCd);
}
