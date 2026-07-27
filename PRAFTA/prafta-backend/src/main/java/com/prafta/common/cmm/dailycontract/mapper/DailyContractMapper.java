package com.prafta.common.cmm.dailycontract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.dailycontract.application.command.ContractInsertCommand;
import com.prafta.common.cmm.dailycontract.application.command.ContractSignInsertCommand;
import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;
import com.prafta.common.cmm.dailycontract.result.ContractLockRow;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.result.DailyUserMetaResult;
import com.prafta.common.cmm.dailycontract.result.EntryCycleResult;

/**
 * 일용직 근로계약서(TB_DAILY_CONTRACT / TB_DAILY_CONTRACT_SIGN) Mapper — 웹/앱 공용 core.
 *
 * <p>출처: 일용직 계약서+승인제 plan §T3 / §3 DDL(prafta-daily-contract-1-ddl.sql 최종본 기준).
 * 게이트 판정에 필요한 최근 소진('05') 요청 조회는 TB_DAILY_ENTRY_REQUEST 를 직접 조회한다
 * (dailyentry mapper 가 TB_USER/TB_NOTI_OUTBOX 를 직접 조회하는 기존 스타일 미러).
 */
@Mapper
public interface DailyContractMapper {

    /** 서명 ID 채번 ('CS' + YYYYMMDD + 시퀀스, dailyentry selectEntryRequestId 패턴 미러). */
    String selectSignId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 사업장 활성(USE_YN='Y') 계약서 단건 — 기능성 유니크로 최대 1건. 없으면 null.
     *
     * <p>호출 허용 범위(승인시점 버전확정 T2): 관리자 화면 기준 조회({@code findActiveContract}),
     * 승인 시 pin 해석({@code DailyContractPinner}), 리졸버의 <b>K8 레거시 폴백</b> 뿐이다.
     * 서명·열람 경로에서 직접 호출하면 pin 기준이 깨져 M-2 경합이 재현된다.
     */
    ActiveContractResult selectActiveContract(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * 특정 버전 계약서 단건 — <b>{@code USE_YN} 조건 없음</b>(승인시점 버전확정 T2 / K5).
     *
     * <p>pin 된 버전이 그 사이 사용중지되어도 열람·서명을 완주해야 하므로 활성 조건을 걸지 않는다.
     * 없으면 null(버전 행 소실 → 호출부가 활성 폴백으로 강등).
     */
    ActiveContractResult selectContractByVer(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("contractVer") int contractVer);

    /** 다음 계약서 버전(MAX+1, 최초 1). */
    int selectNextContractVer(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /** 활성 계약서 비활성(USE_YN='Y' 전건 'N' — 교체/사용중지 공용). 영향행 수 반환(0 = 활성 없음). */
    int updateContractDeactivate(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("updateNo") String updateNo);

    /** 계약서 신규 버전 INSERT (USE_YN='Y'). 활성 중복은 UX_DAILY_CONTRACT_ACTIVE 가 백스톱. */
    void insertContract(ContractInsertCommand command);

    /** 사업장 계약서 버전 이력 목록 (최신 버전 우선, LIMIT 상한). */
    List<ContractVersionRow> selectContractVersionList(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("limitCnt") int limitCnt);

    /** 특정 버전 계약서의 이미지 파일코드 (관리자 미리보기용). 없으면 null. */
    String selectContractFileMgmtCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("contractVer") int contractVer);

    /** 일용직 사용자 메타(사업장/이름 — TB_DAILY_USER). 일용직 아님/미존재면 null. */
    DailyUserMetaResult selectDailyUserMeta(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /**
     * 계정의 <b>현재 승인 사이클 행</b> — pin 값과 사이클 REQ_ID 의 단일 출처(승인시점 버전확정 T2/§4).
     *
     * <p>선택 규칙: 소진('05') 최신 1건 우선 → 없으면 <b>당일</b> 승인('02') 최신 1건. 없으면 null.
     * 구 {@code selectLatestConsumedReqId}('05' 전용)를 대체한다 — 게이트 판정과 서명 기록이 서로 다른
     * 조회를 하다 사이에 상태가 바뀌면 판정과 기록이 어긋나므로 출처를 하나로 묶었다.
     */
    EntryCycleResult selectCurrentCycleRequest(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 계정의 특정 계약서 버전 최신 서명 단건 (게이트 판정/멱등 가드). 없으면 null. */
    ContractSignMetaResult selectLatestSignForVer(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd, @Param("contractVer") int contractVer);

    /** 계정의 최신 서명 단건 (버전 무관 — 앱 내 계약서 열람). 없으면 null. */
    ContractSignMetaResult selectMySignLatest(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 서명본 INSERT (append-only — SIGN_DTIME=서버 NOW). */
    void insertContractSign(ContractSignInsertCommand command);

    /** 서명본 단건 조회 (관리자 열람 인가 판정용 — signId 스코프). 없으면 null. */
    ContractSignMetaResult selectSignMetaById(@Param("cmpnyCd") String cmpnyCd, @Param("signId") String signId);

    /** 서명 이력 목록 (사업장 스코프 강제 + 기간/이름 필터, LIMIT 상한 — 만료/탈퇴 계정 포함). */
    List<ContractSignRow> selectSignListForAdmin(ContractSignListQuery query);

    // ------------------------------------------------------------------------
    // 미서명 계약서 in-place 정정 (승인시점 버전확정 T3 / K6·K7·J8~J11)
    // ------------------------------------------------------------------------

    /**
     * 특정 (사업장, 버전) 계약서의 서명 행 수 — 정정 가능 조건(0건)의 서버측 근거.
     *
     * <p>서명자가 1명이라도 있으면 그 버전의 내용을 바꿀 수 없다(증적 훼손) → 새 버전으로 등록해야 한다.
     */
    int selectSignCntByVer(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("contractVer") int contractVer);

    /** 정정 대상 계약서 단건 + 행 잠금(FOR UPDATE — 동시 정정/정정+교체 직렬화). 없으면 null. */
    ContractLockRow selectContractForUpdate(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("contractVer") int contractVer);

    /**
     * 계약서 파일 in-place 교체 — <b>{@code USE_YN='Y'} 조건부</b>(잠금 이후 상태 반전 백스톱).
     *
     * <p>{@code CONTRACT_VER} 는 증가시키지 않고 {@code USE_YN} 도 바꾸지 않는다(J9) → 재서명 트리거 미발생.
     * 계약서명({@code CONTRACT_NM})은 정정 대상이 아니다(등록 팝업에서 고정값이므로 일관성 유지 — Q5).
     * 영향행 수 반환(0 = 그 사이 교체·중지됨 → 409_002).
     */
    int updateContractFile(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("contractVer") int contractVer, @Param("fileMgmtCd") String fileMgmtCd,
            @Param("updateNo") String updateNo);

    /** 해당 버전을 pin 한 승인('02') 요청 수 — 정정 경고(J10) 데이터. */
    int selectPinnedApprovedReqCnt(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("contractVer") int contractVer);

    /** 같은 사업장 대기('01') 요청 수 — 승인 시 확정될 예정 건(pin 미기록이라 버전 무관, J10). */
    int selectPendingReqCnt(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);
}
