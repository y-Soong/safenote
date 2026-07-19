package com.prafta.common.cmm.dailycontract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.dailycontract.application.command.ContractInsertCommand;
import com.prafta.common.cmm.dailycontract.application.command.ContractSignInsertCommand;
import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.result.DailyUserMetaResult;

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

    /** 사업장 활성(USE_YN='Y') 계약서 단건 — 기능성 유니크로 최대 1건. 없으면 null. */
    ActiveContractResult selectActiveContract(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

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

    /** 계정의 최근 소진('05') 승인요청 ID (CONSUME_DTIME 최신 1건). 없으면 null — 재서명 사이클 판정(D8-①). */
    String selectLatestConsumedReqId(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

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
}
