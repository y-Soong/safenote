package com.prafta.app.attd.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.attd.admin.application.query.AdminAttdScopeQuery;
import com.prafta.app.attd.admin.result.DailyAttdRow;
import com.prafta.app.attd.admin.result.HalfLeaveWindowRow;
import com.prafta.app.attd.admin.result.MonthlyAttdRow;

/**
 * J1-5: 앱 관리자 근태 상세 Mapper(web attd08/attd11 조회 SQL 포팅 + 토큰·노드 스코프).
 *
 * <p>식별자(cmpny/site)는 서비스에서 토큰값만 전달한다. 노드 스코프는 Phase 1
 * {@code AdminScopeMapper.selectScopedNodeCds}(재귀 CTE) 결과 List 를 IN 절에 임베드한다.
 * <p>web comApi/webApi 컨트롤러·서비스를 직접 호출하지 않고 SQL 만 본 매퍼로 포팅했다(앱/웹 분리).
 * <p>지각/조퇴/근무시간 행별 판정·집계는 서비스가 raw 일시 stamp 로 수행한다(자정 넘김 보정 — attd11 규칙).
 */
@Mapper
public interface AppAdminAttdMapper {

    /** 일자(workYmd) 근태 원시 행(차수 단위) — 스코프 + 키워드. ORDER BY USER_CD, WORK_SEQ. */
    List<DailyAttdRow> selectDailyAttdRows(AdminAttdScopeQuery query);

    /** 월(workYm LIKE) 근태 원시 행(차수 단위) — 스코프 + 키워드. ORDER BY USER_CD, WORK_YMD, WORK_SEQ. */
    List<MonthlyAttdRow> selectMonthlyAttdRows(AdminAttdScopeQuery query);

    /**
     * NF-1(2026-08-07): 조회 대상 기간의 확정 부분연차(반차 {@code '01'}) 면제 시각 구간.
     *
     * <p>웹 {@code Attd08Mapper/Attd11Mapper.selectHalfLeaveWindows} 와 동일 술어(회사·사업장·기간 스코프 필수).
     * 일자 조회면 {@code workYmd} 단일일, 월별 조회면 {@code workYm} LIKE 로 기간을 잡는다
     * (같은 Query 를 쓰는 근태 원시행 조회와 기간 축을 맞춘다).
     * 노드 스코프는 걸지 않는다 — 서비스가 (USER_CD, WORK_YMD) 키로 조인하므로 스코프 밖 사용자의
     * 행은 조회 결과에 도달하지 않는다(웹 두 화면과 동일한 취급).
     */
    List<HalfLeaveWindowRow> selectHalfLeaveWindows(AdminAttdScopeQuery query);
}
