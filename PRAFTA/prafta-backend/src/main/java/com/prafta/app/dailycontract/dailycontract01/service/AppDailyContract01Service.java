package com.prafta.app.dailycontract.dailycontract01.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractMetaResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractSignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.MySignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.SignGateResponse;
import com.prafta.common.cmm.file.application.model.FileBytesResult;

/**
 * 앱 일용직 계약서 서비스 (얇은 위임 계층 — core: DailyContractService).
 *
 * <p>출처: 일용직 계약서+승인제 plan §T3 endpoint / UI-DC-01·04.
 * 식별자(cmpnyCd/userCd)는 컨트롤러가 JWT 클레임에서만 도출해 전달한다.
 */
public interface AppDailyContract01Service {

    /** 서명 게이트 판정 (R2 + D8). */
    SignGateResponse judgeSignGate(String cmpnyCd, String userCd);

    /**
     * 활성 계약서 원본 로드 — <b>구버전 앱 폴백 경로</b>(본인 사업장 자동 스코프).
     * 없으면 null(호출부 400_003).
     *
     * <p>PDF 양식은 core 가 세로 병합 PNG 로 변환해 내려 준다(T5/P7). 신규 앱은
     * {@link #findContractMeta}/{@link #loadContractPage} 를 사용한다.
     */
    FileBytesResult loadContractImage(String cmpnyCd, String userCd);

    /** 활성 계약서 메타(형식/페이지 수) — pager 초기화. 없으면 null(호출부 400_003). */
    ContractMetaResponse findContractMeta(String cmpnyCd, String userCd);

    /** 활성 계약서 단일 페이지(1-base) — 없으면 null(호출부 400_003), 범위 밖은 core 가 400_007. */
    FileBytesResult loadContractPage(String cmpnyCd, String userCd, int page);

    /** 서명 저장 (multipart 서명 PNG — 서버 합성 + SHA-256, R5). */
    ContractSignResponse sign(String cmpnyCd, String userCd, MultipartFile signFile);

    /** 본인 최신 서명 메타 (없으면 signYn='N'). 서명본 형식/페이지 수 포함(T4). */
    MySignResponse findMySign(String cmpnyCd, String userCd);

    /**
     * 본인 최신 서명본 — <b>구버전 앱 표시용</b>(PDF 면 세로 병합 PNG). 없으면 null(호출부 404_001).
     */
    FileBytesResult loadMySignImage(String cmpnyCd, String userCd);

    /** 본인 최신 서명본 단일 페이지(1-base) — 없으면 null(호출부 404_001). */
    FileBytesResult loadMySignPage(String cmpnyCd, String userCd, int page);

    /** 본인 최신 서명본 원본 바이트(신규 앱 저장용 — PDF/레거시 PNG). 없으면 null(호출부 404_001). */
    FileBytesResult loadMySignFile(String cmpnyCd, String userCd);
}
