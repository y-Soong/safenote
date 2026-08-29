package com.prafta.app.admin.employeestatus.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.admin.employeestatus.application.query.EmployeeStatusScopeQuery;
import com.prafta.app.admin.employeestatus.result.EmployeeGpsTrailRow;
import com.prafta.app.admin.employeestatus.result.EmployeeOwnerScopeResult;
import com.prafta.app.admin.employeestatus.result.EmployeeStatusRosterRow;

/**
 * PRAFTA-002/003: 앱 관리자 "직원관리" Mapper.
 *
 * <p>PRAFTA-002(로스터 기반 실시간 근태) — 시작점은 {@code TB_USER}(스코프 내 활성 사용자 전체)이며
 * {@code TB_USER_WORK_PLAN}/{@code TB_USER_ATTD_MGMT}를 LEFT JOIN 한다(§0-3-3 근거 — 미출근/휴무 인원도
 * 목록에 나와야 한다). 상태(근무중/미출근/휴무/퇴근) 산출은 SQL CASE 가 아니라 서비스(Java)가 한다.
 *
 * <p>PRAFTA-003(GPS 궤적) — 웹 {@code Attd08Mapper.selectAttdOwnerScope}/{@code selectAttdGpsTrail}의 SQL을
 * 그대로 이식했다(재설계 금지). 복호화는 서비스 계층에서만 수행한다(매퍼는 암호문+평문 원본만 반환).
 */
@Mapper
public interface AppAdminEmployeeStatusMapper {

    /** 일자(workYmd) 직원 현황 로스터 — 스코프 내 활성 사용자 전체 + 키워드. ORDER BY USER_NM, USER_CD. */
    List<EmployeeStatusRosterRow> selectDailyRoster(EmployeeStatusScopeQuery query);

    /** PRAFTA-003: GPS 궤적 인가 판정용 근태 행 스코프(사업장/부서/소유자). 없으면 null. */
    EmployeeOwnerScopeResult selectAttdOwnerScope(@Param("cmpnyCd") String cmpnyCd,
                                                   @Param("attdId") String attdId);

    /** PRAFTA-003: 암호문+평문 병렬 행 조회(fallback 복호화는 서비스 계층). */
    List<EmployeeGpsTrailRow> selectAttdGpsTrail(@Param("gvCmpnyCd") String gvCmpnyCd,
                                                  @Param("attdId") String attdId);
}
