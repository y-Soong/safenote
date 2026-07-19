package com.prafta.web.user.user07.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user07.dto.response.ContractListResponse;
import com.prafta.web.user.user07.dto.response.ContractRegResponse;
import com.prafta.web.user.user07.dto.response.ContractStopResponse;

/**
 * 웹 User_07(일용직 계약서 관리) 서비스 (얇은 위임 계층 — core: DailyContractService).
 *
 * <p>출처: 일용직 계약서+승인제 plan §T3 endpoint / UI-DC-05.
 * 사업장 인가 가드(master/hr + SITE_AUTH)는 core 가 수행한다.
 */
public interface User07Service {

    /** 활성 계약서 요약 + 버전 이력 조회. */
    ContractListResponse selectContractList(String siteCd, TokenInfo token);

    /** 계약서 등록/교체 (업로드 = 새 버전, D8-② 재서명 트리거). */
    ContractRegResponse registerContract(String siteCd, String contractNm, MultipartFile file, TokenInfo token);

    /** 계약서 사용중지. */
    ContractStopResponse stopContract(String siteCd, TokenInfo token);

    /** 특정 버전 계약서 이미지 로드 (미리보기 — 없으면 null, 호출부 400_003). */
    ImageBytesResult loadContractImage(String siteCd, int contractVer, TokenInfo token);
}
