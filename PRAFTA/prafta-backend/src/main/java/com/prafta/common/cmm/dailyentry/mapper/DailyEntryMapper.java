package com.prafta.common.cmm.dailyentry.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.dailyentry.application.command.EntryNotiOutboxCommand;
import com.prafta.common.cmm.dailyentry.application.command.EntryRequestInsertCommand;
import com.prafta.common.cmm.dailyentry.application.query.EntryRequestListQuery;
import com.prafta.common.cmm.dailyentry.result.EntryRequestMetaResult;
import com.prafta.common.cmm.dailyentry.result.EntryRequestRow;

/**
 * 일용직 입장 승인요청(TB_DAILY_ENTRY_REQUEST) Mapper — 웹/앱 공용 core.
 *
 * <p>출처: 일용직 계약서+승인제 plan §T2 / §3 DDL. 상태 모델은 plan §1(SYS082) 단일 기준.
 */
@Mapper
public interface DailyEntryMapper {

    /** 승인요청 ID 채번 ('ER' + YYYYMMDD + 시퀀스, selectDailySlotHisId 패턴 미러). */
    String selectEntryRequestId(@Param("cmpnyCd") String cmpnyCd);

    /** 계정의 open('01' 대기/'02' 승인) 요청 단건 조회. 기능성 유니크로 최대 1건. 없으면 null. */
    EntryRequestMetaResult selectOpenEntryRequest(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 당일 거부('03', PROC_DTIME=오늘) 이력 존재 카운트. 1 이상이면 당일 재요청 금지(007). */
    int selectTodayRejectedCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 승인요청 신규 INSERT (REQ_STATUS='01' 고정). open 중복은 UX_DAILY_ENTRY_REQ_OPEN 이 백스톱. */
    void insertEntryRequest(EntryRequestInsertCommand command);

    /** 처리 대상 요청 단건 조회 + 행 잠금(FOR UPDATE — 동시 승인/거부 직렬화). 없으면 null. */
    EntryRequestMetaResult selectEntryRequestMetaForUpdate(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);

    /**
     * 승인 처리 ('01' 대기 → '02' 승인 조건부 UPDATE). 영향행 수 반환(0 = 이미 처리됨).
     *
     * <p>승인 시점 확정 계약서 버전(pin — 승인시점 버전확정 T1)을 <b>같은 문장</b>에서 기록한다.
     * 별도 UPDATE 문을 두면 pin 만 반영되고 상태 전이가 실패하는 창이 생긴다.
     *
     * @param contractVer pin 값 — 활성 계약서 버전(>0) 또는 미등록 센티넬 0(K4)
     */
    int updateEntryRequestApprove(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId,
            @Param("procUserCd") String procUserCd, @Param("contractVer") int contractVer);

    /** 거부 처리 ('01' 대기 → '03' 거부 조건부 UPDATE, 사유 기록). 영향행 수 반환. */
    int updateEntryRequestReject(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId,
            @Param("reason") String reason, @Param("procUserCd") String procUserCd);

    /** 승인 소진 ('02' 승인 → '05' 소진 조건부 UPDATE, 로그인 성공 트랜잭션 내 호출). 영향행 수 반환(0 = 경합 → 롤백). */
    int updateEntryRequestConsume(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId,
            @Param("userCd") String userCd);

    /** 자정 만료 — REQ_DTIME 이 오늘 이전이고 대기('01')/승인('02') 상태인 요청 일괄 '04' 전이(D7). 영향행 수 반환. */
    int updateExpireOverdueEntryRequests(@Param("updateNo") String updateNo);

    /** 승인요청 목록 조회 (사업장 스코프 + 상태/유형/요청일 필터, LIMIT 상한). */
    List<EntryRequestRow> selectEntryRequestList(EntryRequestListQuery query);

    /** 처리자의 사업장 권한(TB_USER_SITE_AUTH USE_YN='Y') 보유 카운트 — 승인/거부/목록 인가 가드. */
    int selectSiteAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd,
            @Param("siteCd") String siteCd);

    /** 푸시 대상 — 해당 사업장 권한을 보유한 활성 관리자(AUTH_CD IN authCdList) USER_CD 목록. */
    List<String> selectSiteEntryAdmins(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("authCdList") List<String> authCdList);

    /** 승인요청 발생 푸시 outbox INSERT (tb_noti_outbox, 대상자별 1행 — nearmiss01 패턴 미러). */
    void insertEntryNotiOutbox(EntryNotiOutboxCommand command);
}
