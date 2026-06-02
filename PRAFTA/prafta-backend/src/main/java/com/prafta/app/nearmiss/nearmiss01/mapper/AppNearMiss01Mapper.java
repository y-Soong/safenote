package com.prafta.app.nearmiss.nearmiss01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.nearmiss.nearmiss01.application.command.ChangeStatusCommand;
import com.prafta.app.nearmiss.nearmiss01.application.command.InsertReportCommand;
import com.prafta.app.nearmiss.nearmiss01.application.command.NotiOutboxCommand;
import com.prafta.app.nearmiss.nearmiss01.application.query.IncidentDetailQuery;
import com.prafta.app.nearmiss.nearmiss01.application.query.MyReportListQuery;
import com.prafta.app.nearmiss.nearmiss01.application.query.NearMissIdSeqQuery;
import com.prafta.app.nearmiss.nearmiss01.application.query.SiteIncidentListQuery;
import com.prafta.app.nearmiss.nearmiss01.result.IncidentResult;
import com.prafta.app.nearmiss.nearmiss01.result.StatusCountResult;

/**
 * 아차사고/사건 보고 (앱) Mapper.
 *
 * <p>웹 NearMiss01Mapper 의 재사용 쿼리(채번/사업장권한/목록/상세/상태/카운트)를 앱에 미러링한 사본이다.
 *    web mapper 호출/의존 없이 동일 테이블만 공유한다(app-010 완전분리 원칙).
 *    신규: insertReport(보고자 입력), updateFirstReview(100->200 + 임시조치), updateReject(900 + 사유),
 *          푸시 대상 selectSiteSafetyManagers / selectNextNotiId / insertNotiOutbox.
 */
@Mapper
public interface AppNearMiss01Mapper {

    // 사업장 접근 권한 확인 (tb_user_site_auth 매핑, USE_YN='Y'). 1 이상이면 접근 가능
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // A2 내 보고 목록 (REPORTER_ID=gvUserCd 스코프)
    List<IncidentResult> selectMyReportList(MyReportListQuery query);

    // A3 사업장 사건 목록 (사업장 스코프 + 필터)
    List<IncidentResult> selectSiteIncidentList(SiteIncidentListQuery query);

    // A4 상태별 카운트 (필터 동반)
    StatusCountResult selectStatusCounts(SiteIncidentListQuery query);

    // A5 단건 상세 (사업장 스코프 강제)
    IncidentResult selectIncidentInfo(IncidentDetailQuery query);

    // 전이/접근 검증용: 현재 처리상태 + 보고자 단건 조회 (없으면 null)
    IncidentResult selectReportMeta(IncidentDetailQuery query);

    // A1 채번: 사업장+당일 기준 다음값 (NM + YYYYMMDD + 3자리)
    String selectNextNearMissId(NearMissIdSeqQuery query);

    // A1 보고 INSERT (REPORT_STATUS_CD='100', SRC_*=NULL)
    int insertReport(InsertReportCommand command);

    // A6 1차 확인 100->200 (검토중 + 임시조치 + 검토자)
    int updateFirstReview(ChangeStatusCommand command);

    // A6 반려 900 (사유 + 검토자)
    int updateReject(ChangeStatusCommand command);

    // 푸시 대상: 사업장 안전관리자 USER_CD 목록 (tb_user_site_auth ∩ 안전직군 AUTH_CD)
    List<String> selectSiteSafetyManagers(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("authCdList") List<String> authCdList
    );

    // 푸시 outbox 1행 INSERT (PENDING). NOTI_ID 는 INSERT 내부 서브쿼리로 회사+당일 채번.
    //   UNIQUE(CMPNY_CD,DEDUP_KEY) 중복 시 예외 → 서비스에서 흡수(중복 발송 방지).
    int insertNotiOutbox(NotiOutboxCommand command);
}
