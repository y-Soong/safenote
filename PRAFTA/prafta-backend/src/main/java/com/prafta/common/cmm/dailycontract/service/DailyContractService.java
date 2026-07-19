package com.prafta.common.cmm.dailycontract.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.dailycontract.application.query.ContractSignListQuery;
import com.prafta.common.cmm.dailycontract.result.ActiveContractResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignRow;
import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.result.SignGateResult;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;

/**
 * 일용직 근로계약서 core 서비스 (웹 User_07/User_08 / 앱 dailycontract01 공용).
 *
 * <p>출처: 일용직 계약서+승인제 요청서 §2 D1·D2·D3·D8, §3 R1·R2·R5, §4-3, §6 / plan §T3.
 * 정책서: {@code common/11-security-privacy.md} §11.1·§11.3.
 *
 * <p>열람 인가 원칙(IDOR): 본인(세션 userCd 일치) 또는 해당 사업장 관리자(master/hr + SITE_AUTH —
 * {@code DailyEntryService.hasSiteAuthority} 재사용)만. 파일 경로는 응답에 노출하지 않고 스트림만 반환한다.
 */
public interface DailyContractService {

    /** 사업장 활성 계약서 단건 조회. 없으면 null. */
    ActiveContractResult findActiveContract(String cmpnyCd, String siteCd);

    /**
     * 계약서 서명 게이트 판정 (R2 + D8, plan §6 기본안 3).
     *
     * <p>활성 계약서 없음 → 스킵('N'). 있음 && (현재 버전 서명 없음 OR 서명의 REQ_ID 가
     * 최근 소진('05') 요청 ID 와 불일치) → 서명 필요('Y'). REQ_ID NULL 레거시 서명은 버전만 비교.
     * 일용직 아님(TB_DAILY_USER 미존재)도 스킵('N').
     */
    SignGateResult judgeSignGate(String cmpnyCd, String userCd);

    /**
     * 계약서 등록/교체 (웹 User_07 — D8-② 재서명 트리거).
     * 기존 활성 USE_YN='N' + CONTRACT_VER MAX+1 INSERT 를 한 트랜잭션으로 처리한다.
     * 업로드 검증: 이미지(png/jpg) + 10MB 이하(DAILYCONTRACT_400_004).
     *
     * @return 신규 계약서 버전
     */
    int registerContract(String cmpnyCd, String siteCd, String contractNm, MultipartFile file,
            String procUserCd, String procAuthCd);

    /**
     * 계약서 사용중지 (웹 User_07). 활성 계약서 없으면 DAILYCONTRACT_400_003.
     */
    void stopContract(String cmpnyCd, String siteCd, String procUserCd, String procAuthCd);

    /** 사업장 계약서 버전 이력 (웹 User_07 — 사업장 인가 가드 포함). */
    List<ContractVersionRow> findContractVersions(String cmpnyCd, String siteCd,
            String procUserCd, String procAuthCd);

    /** 특정 버전 계약서 이미지 로드 (웹 User_07 미리보기 — 사업장 인가 가드 포함). 없으면 null. */
    ImageBytesResult loadContractImageForAdmin(String cmpnyCd, String siteCd, int contractVer,
            String procUserCd, String procAuthCd);

    /** 활성 계약서 원본 이미지 로드 (앱 일용직 서명 화면 — 본인 사업장 자동 스코프). 없으면 null. */
    ImageBytesResult loadActiveContractImageForUser(String cmpnyCd, String userCd);

    /**
     * 서명 저장 (R5 — 서버 합성 + SHA-256 + append-only INSERT).
     *
     * <p>서명 PNG 서버 검증(크기/형식/디코딩) → 계약서 원본 + §4-3 자동 계약정보 블록 + 서명 합성 →
     * 합성본·원본 저장(tb_file_info) → 해시 기록 → INSERT(USER_NM_SNAPSHOT, 최근 소진 REQ_ID 연결).
     * 현재 사이클 서명 존재 시 DAILYCONTRACT_400_002(멱등 가드).
     *
     * @return 생성된 서명 메타(signId 등)
     */
    ContractSignMetaResult signContract(String cmpnyCd, String userCd, MultipartFile signFile);

    /** 본인 최신 서명 메타 조회 (앱 내 계약서 — 교부 의무 §6-1). 없으면 null. */
    ContractSignMetaResult findMySign(String cmpnyCd, String userCd);

    /** 본인 최신 서명 합성본 이미지 로드 (본인 스코프 강제 — userCd 로만 조회). 없으면 null. */
    ImageBytesResult loadMySignImage(String cmpnyCd, String userCd);

    /** 서명 이력 목록 (웹 User_08 탭2 — 사업장 인가 가드 포함, 만료/탈퇴 계정 포함). */
    List<ContractSignRow> findSignListForAdmin(ContractSignListQuery query,
            String procUserCd, String procAuthCd);

    /**
     * 서명본 합성 이미지 로드 (웹 User_08 — 관리자 열람/다운로드).
     * signId 의 SITE_CD 기준 사업장 인가 가드(IDOR). 미존재 시 DAILYCONTRACT_404_001.
     */
    ImageBytesResult loadSignImageForAdmin(String cmpnyCd, String signId,
            String procUserCd, String procAuthCd);
}
