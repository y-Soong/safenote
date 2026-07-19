package com.prafta.app.dailycontract.dailycontract01.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractSignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.MySignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.SignGateResponse;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;

/**
 * 앱 일용직 계약서 서비스 (얇은 위임 계층 — core: DailyContractService).
 *
 * <p>출처: 일용직 계약서+승인제 plan §T3 endpoint / UI-DC-01·04.
 * 식별자(cmpnyCd/userCd)는 컨트롤러가 JWT 클레임에서만 도출해 전달한다.
 */
public interface AppDailyContract01Service {

    /** 서명 게이트 판정 (R2 + D8). */
    SignGateResponse judgeSignGate(String cmpnyCd, String userCd);

    /** 활성 계약서 원본 이미지 로드 (본인 사업장 자동 스코프). 없으면 null(호출부 400_003). */
    ImageBytesResult loadContractImage(String cmpnyCd, String userCd);

    /** 서명 저장 (multipart 서명 PNG — 서버 합성 + SHA-256, R5). */
    ContractSignResponse sign(String cmpnyCd, String userCd, MultipartFile signFile);

    /** 본인 최신 서명 메타 (없으면 signYn='N'). */
    MySignResponse findMySign(String cmpnyCd, String userCd);

    /** 본인 최신 서명 합성본 이미지 (없으면 null — 호출부 404_001). */
    ImageBytesResult loadMySignImage(String cmpnyCd, String userCd);
}
