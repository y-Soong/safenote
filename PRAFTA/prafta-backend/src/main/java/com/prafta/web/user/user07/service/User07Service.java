package com.prafta.web.user.user07.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user07.dto.response.ContractAmendPrecheckResponse;
import com.prafta.web.user.user07.dto.response.ContractAmendResponse;
import com.prafta.web.user.user07.dto.response.ContractListResponse;
import com.prafta.web.user.user07.dto.response.ContractMetaResponse;
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

    /**
     * 미서명 계약서 in-place 정정 (승인시점 버전확정 T3 — 버전 미증가, 재서명 미발생).
     *
     * <p>계약서명은 정정 대상이 아니므로 파라미터로 받지 않는다(등록 시 고정값과의 일관성).
     * 서명 0건 조건·사업장 인가·업로드 검증은 모두 core 가 수행한다.
     */
    ContractAmendResponse amendContract(String siteCd, int contractVer, MultipartFile file, TokenInfo token);

    /**
     * 정정 사전 점검 (정정 팝업 표시용 — J10 경고 데이터). 해당 버전 없으면 null(호출부 400_003).
     */
    ContractAmendPrecheckResponse loadAmendPrecheck(String siteCd, int contractVer, TokenInfo token);

    /**
     * 특정 버전 계약서 원본 로드 (미리보기 — 없으면 null, 호출부 400_003).
     *
     * <p>PDF/이미지 원본 바이트를 그대로 반환한다(Content-Type 동적 스트림이라 새 탭 내장 뷰어로 열림).
     */
    FileBytesResult loadContractImage(String siteCd, int contractVer, TokenInfo token);

    /**
     * 특정 버전 계약서의 형식/페이지 수 (활성 계약서 카드 표시용 — T4).
     *
     * <p>목록(contract-lists)은 FILE_EXT 조인으로 형식만 내려 주고(PDF 파싱 0회 — D-P11),
     * 페이지 수는 활성 1건에 대해서만 본 EP 로 조회한다. 해당 버전이 없으면 null(호출부 400_003).
     */
    ContractMetaResponse loadContractMeta(String siteCd, int contractVer, TokenInfo token);
}
