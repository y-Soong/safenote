package com.prafta.app.dailycontract.dailycontract01.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractSignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.MySignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.SignGateResponse;
import com.prafta.app.dailycontract.dailycontract01.service.AppDailyContract01Service;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.SignGateResult;
import com.prafta.common.cmm.dailycontract.service.DailyContractService;
import com.prafta.common.cmm.file.application.model.ImageBytesResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 일용직 계약서 서비스 구현 — core(DailyContractService) 위임.
 *
 * <p>서명 검증/합성/저장/멱등 가드/파일 방어는 core 가 수행한다(웹과 단일 출처).
 * 본인 스코프는 (cmpnyCd, userCd) 파라미터로만 강제한다(IDOR 차단).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppDailyContract01ServiceImpl implements AppDailyContract01Service {

    private final DailyContractService dailyContractService;

    @Override
    public SignGateResponse judgeSignGate(String cmpnyCd, String userCd) {
        SignGateResult result = dailyContractService.judgeSignGate(cmpnyCd, userCd);

        log.info("계약서 서명 게이트 판정 — cmpnyCd={}, userCd={}, required={}",
                cmpnyCd, userCd, result.signRequiredYn());

        return SignGateResponse.builder()
                .signRequiredYn(result.signRequiredYn())
                .contractVer(result.contractVer())
                .contractNm(result.contractNm())
                .build();
    }

    @Override
    public ImageBytesResult loadContractImage(String cmpnyCd, String userCd) {
        return dailyContractService.loadActiveContractImageForUser(cmpnyCd, userCd);
    }

    @Override
    public ContractSignResponse sign(String cmpnyCd, String userCd, MultipartFile signFile) {
        log.info("계약서 서명 저장 진입 — cmpnyCd={}, userCd={}", cmpnyCd, userCd);

        ContractSignMetaResult sign = dailyContractService.signContract(cmpnyCd, userCd, signFile);

        return ContractSignResponse.builder()
                .signId(sign.signId())
                .contractVer(sign.contractVer())
                .firstWorkDate(sign.firstWorkDate())
                .signDtime(sign.signDtime())
                .build();
    }

    @Override
    public MySignResponse findMySign(String cmpnyCd, String userCd) {
        ContractSignMetaResult sign = dailyContractService.findMySign(cmpnyCd, userCd);
        if (sign == null) {
            return MySignResponse.builder()
                    .signYn("N")
                    .build();
        }
        return MySignResponse.builder()
                .signYn("Y")
                .signId(sign.signId())
                .contractVer(sign.contractVer())
                .firstWorkDate(sign.firstWorkDate())
                .signDtime(sign.signDtime())
                .build();
    }

    @Override
    public ImageBytesResult loadMySignImage(String cmpnyCd, String userCd) {
        return dailyContractService.loadMySignImage(cmpnyCd, userCd);
    }
}
