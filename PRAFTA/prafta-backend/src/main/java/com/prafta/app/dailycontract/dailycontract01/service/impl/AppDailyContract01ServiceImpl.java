package com.prafta.app.dailycontract.dailycontract01.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractMetaResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.ContractSignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.MySignResponse;
import com.prafta.app.dailycontract.dailycontract01.dto.response.SignGateResponse;
import com.prafta.app.dailycontract.dailycontract01.service.AppDailyContract01Service;
import com.prafta.common.cmm.dailycontract.result.ContractDocMeta;
import com.prafta.common.cmm.dailycontract.result.ContractMetaResult;
import com.prafta.common.cmm.dailycontract.result.ContractSignMetaResult;
import com.prafta.common.cmm.dailycontract.result.SignGateResult;
import com.prafta.common.cmm.dailycontract.service.DailyContractService;
import com.prafta.common.cmm.file.application.model.FileBytesResult;

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
    public FileBytesResult loadContractImage(String cmpnyCd, String userCd) {
        return dailyContractService.loadActiveContractImageForUser(cmpnyCd, userCd);
    }

    @Override
    public ContractMetaResponse findContractMeta(String cmpnyCd, String userCd) {
        ContractMetaResult meta = dailyContractService.findActiveContractMeta(cmpnyCd, userCd);
        if (meta == null) {
            return null;
        }
        log.info("계약서 메타 조회 — cmpnyCd={}, userCd={}, ver={}, format={}, pages={}",
                cmpnyCd, userCd, meta.contractVer(), meta.formatType(), meta.pageCount());

        return ContractMetaResponse.builder()
                .contractVer(meta.contractVer())
                .contractNm(meta.contractNm())
                .formatType(meta.formatType())
                .pageCount(meta.pageCount())
                .build();
    }

    @Override
    public FileBytesResult loadContractPage(String cmpnyCd, String userCd, int page) {
        return dailyContractService.loadActiveContractPage(cmpnyCd, userCd, page);
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
        // 서명본 형식/페이지 수(T4) — 파일 유실 시 null(메타 카드는 표시, 페이지 렌더는 불가).
        ContractDocMeta doc = describeSignDocQuietly(cmpnyCd, userCd);

        return MySignResponse.builder()
                .signYn("Y")
                .signId(sign.signId())
                .contractVer(sign.contractVer())
                .firstWorkDate(sign.firstWorkDate())
                .signDtime(sign.signDtime())
                .formatType(doc == null ? null : doc.formatType())
                .pageCount(doc == null ? null : doc.pageCount())
                .build();
    }

    @Override
    public FileBytesResult loadMySignImage(String cmpnyCd, String userCd) {
        return dailyContractService.loadMySignImage(cmpnyCd, userCd);
    }

    @Override
    public FileBytesResult loadMySignPage(String cmpnyCd, String userCd, int page) {
        return dailyContractService.loadMySignPage(cmpnyCd, userCd, page);
    }

    @Override
    public FileBytesResult loadMySignFile(String cmpnyCd, String userCd) {
        return dailyContractService.loadMySignFile(cmpnyCd, userCd);
    }

    /**
     * 서명본 형식/페이지 수 조회 — <b>실패를 메타 응답으로 흘리지 않는다</b>(T4 필드 추가로 인한 회귀 방지).
     *
     * <p>{@code my-sign} 은 T4 이전까지 파일을 읽지 않았다. 신규 필드 도출을 위해 파일을 읽게 되면서
     * 파일 손상/형식 이상이 기존에 정상이던 메타 카드(교부 화면 진입점)를 막을 수 있으므로,
     * 도출 실패 시 두 필드만 비운다. 실제 원인은 {@code my-sign-page} 호출 시 에러코드로 드러난다.
     */
    private ContractDocMeta describeSignDocQuietly(String cmpnyCd, String userCd) {
        try {
            return dailyContractService.describeMySignDoc(cmpnyCd, userCd);
        } catch (Exception e) {
            log.warn("서명본 형식/페이지 수 도출 실패(메타 응답은 유지) — cmpnyCd={}, userCd={}, 원인={}",
                    cmpnyCd, userCd, e.getMessage());
            return null;
        }
    }
}
