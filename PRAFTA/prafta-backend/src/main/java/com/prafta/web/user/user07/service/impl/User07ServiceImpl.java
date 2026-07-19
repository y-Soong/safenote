package com.prafta.web.user.user07.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.dailycontract.result.ContractVersionRow;
import com.prafta.common.cmm.dailycontract.service.DailyContractService;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user07.dto.response.ContractListResponse;
import com.prafta.web.user.user07.dto.response.ContractRegResponse;
import com.prafta.web.user.user07.dto.response.ContractStopResponse;
import com.prafta.web.user.user07.dto.response.ContractVersionItem;
import com.prafta.web.user.user07.service.User07Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹 User_07(일용직 계약서 관리) 서비스 구현 — core(DailyContractService) 위임.
 *
 * <p>사업장 인가 가드/업로드 검증/버전 전이 트랜잭션은 core 가 수행한다(앱과 단일 출처).
 * 회사/처리자 식별은 JWT 클레임으로만 강제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class User07ServiceImpl implements User07Service {

    private final DailyContractService dailyContractService;

    @Override
    public ContractListResponse selectContractList(String siteCd, TokenInfo token) {
        log.info("웹 계약서 관리 목록 조회 진입 — cmpnyCd={}, siteCd={}", token.gv_cmpnyCd(), siteCd);

        List<ContractVersionRow> rows = dailyContractService.findContractVersions(
                token.gv_cmpnyCd(), siteCd, token.gv_userCd(), token.gv_authCd());

        List<ContractVersionItem> items = rows.stream()
                .map(r -> new ContractVersionItem(
                        r.contractVer()
                        , r.contractNm()
                        , r.useYn()
                        , r.insertNo()
                        , r.insertNm()
                        , r.insertDate()))
                .toList();

        // 활성 요약 카드 — 버전 이력에서 USE_YN='Y' 단건(기능성 유니크로 최대 1건).
        ContractVersionItem active = items.stream()
                .filter(i -> "Y".equals(i.useYn()))
                .findFirst()
                .orElse(null);

        return ContractListResponse.builder()
                .activeContract(active)
                .versionList(items)
                .totalCount(items.size())
                .build();
    }

    @Override
    public ContractRegResponse registerContract(String siteCd, String contractNm, MultipartFile file,
            TokenInfo token) {
        log.info("웹 계약서 등록 진입 — cmpnyCd={}, siteCd={}, 등록자={}",
                token.gv_cmpnyCd(), siteCd, token.gv_userCd());

        int newVer = dailyContractService.registerContract(
                token.gv_cmpnyCd(), siteCd, contractNm, file, token.gv_userCd(), token.gv_authCd());

        return ContractRegResponse.builder()
                .contractVer(newVer)
                .build();
    }

    @Override
    public ContractStopResponse stopContract(String siteCd, TokenInfo token) {
        log.info("웹 계약서 사용중지 진입 — cmpnyCd={}, siteCd={}, 처리자={}",
                token.gv_cmpnyCd(), siteCd, token.gv_userCd());

        dailyContractService.stopContract(
                token.gv_cmpnyCd(), siteCd, token.gv_userCd(), token.gv_authCd());

        return ContractStopResponse.builder()
                .processedCount(1)
                .build();
    }

    @Override
    public ImageBytesResult loadContractImage(String siteCd, int contractVer, TokenInfo token) {
        return dailyContractService.loadContractImageForAdmin(
                token.gv_cmpnyCd(), siteCd, contractVer, token.gv_userCd(), token.gv_authCd());
    }
}
