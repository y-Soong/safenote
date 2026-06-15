package com.prafta.app.admin.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.admin.dashboard.application.query.AttdCountQuery;
import com.prafta.app.admin.dashboard.application.query.SafetyCountQuery;
import com.prafta.app.admin.dashboard.result.AttdCountResult;
import com.prafta.app.admin.dashboard.result.PatrolCountResult;

/**
 * J1-10 (B-5): 관리자 대시보드 요약 카운트 Mapper(조회 전용).
 *
 * <p>식별자(cmpnyCd/siteCd)는 서비스가 토큰/멤버십 재검증 후 전달(IDOR 차단). leading 콤마, #{} 바인딩,
 * SELECT * 금지. 노드 스코프는 AdminScopeMapper.scopedNodeCds 조각을 include 로 재사용(신규 CTE 작성 금지).
 */
@Mapper
public interface AppAdminDashboardMapper {

    /** DB 기준 오늘(YYYYMMDD). 클라 시각 신뢰 금지(home01.selectTodayYmd 동형). */
    String selectTodayYmd();

    /**
     * 사업장 접근 권한 확인(TB_USER_SITE_AUTH, USE_YN='Y'). 1 이상이면 접근 가능(안전 위젯 IDOR 재검증).
     * <p>AppAdminSafetyMapper.countUserSiteAuth 동형(2-3 사업장 멤버십 검증).
     */
    int countUserSiteAuth(
            @org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
            @org.apache.ibatis.annotations.Param("userCd") String userCd,
            @org.apache.ibatis.annotations.Param("siteCd") String siteCd);

    /** 근태 3종 카운트(출근 실제/예정/연차) — 노드 스코프. 한 번의 호출로 3개 반환. */
    AttdCountResult selectAttdCounts(AttdCountQuery query);

    /** 순회 점검 카운트(분모=사업장 활성 체크포인트, 분자=금일 답변 존재 개소) — 사업장 스코프. */
    PatrolCountResult selectPatrolCount(SafetyCountQuery query);

    /** 위험성 평가 미처리 검토요청(SYS011='001') 건수 — 사업장 스코프. */
    int selectRiskPendingCount(SafetyCountQuery query);

    /** 아차사고 신규 접수(REPORT_STATUS_CD='100') 건수 — 사업장 스코프. */
    int selectNearMissNewCount(SafetyCountQuery query);
}
