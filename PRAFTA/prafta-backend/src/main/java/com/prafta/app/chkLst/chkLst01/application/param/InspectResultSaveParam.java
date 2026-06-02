package com.prafta.app.chkLst.chkLst01.application.param;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.chkLst.chkLst01.dto.request.SaveInspectResultRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B1: 점검결과 저장 Param.
 * <p>multipart 요청을 service 단일 인자로 정리한다(request + files + tokenInfo).
 * <p>files Map 은 controller 가 받은 {@code Map<String, MultipartFile>} 원본
 *   (key 정규식 {@code ^files\[(.+)]$}).
 * <p>files null 허용(첨부 없는 항목 케이스).
 * <p>prafta-app-011: siteCd 불일치 시 service 레이어에서 403 차단을 위해
 *   reqSiteCd (클라이언트 원본값) 와 siteCdMismatch 플래그를 보존한다.
 * <p>prafta-app-011: userCd 클라이언트 입력 필드 제거 -- DB 기록은 tokenInfo.gv_userCd() 사용.
 * <p>prafta-036-C(H-3): cmpnyCd 도 tokenInfo.gv_cmpnyCd() 로 강제 캐노니컬라이즈
 *   (파일 디렉토리 경로 첫 세그먼트 공격자 통제 차단). request.cmpnyCd 는 수신은 하되 무시.
 */
public record InspectResultSaveParam(
    String cmpnyCd
    , String siteCd
    , String reqSiteCd
    , boolean siteCdMismatch
    , String chkptCd
    , String workDate
    , MultipartFile items
    , Map<String, MultipartFile> files
    , TokenInfo tokenInfo
) {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(InspectResultSaveParam.class);

    public static InspectResultSaveParam from(
            SaveInspectResultRequest request
            , Map<String, MultipartFile> files
            , TokenInfo tokenInfo
    ) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // siteCd 토큰 캐노니컬라이즈 — 불일치 여부는 service 에서 판단
        String tokenSiteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(tokenSiteCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String reqSiteCd = request.getSiteCd();
        boolean mismatch = StringUtils.hasText(reqSiteCd) && !tokenSiteCd.equals(reqSiteCd);
        if (mismatch) {
            log.warn("[chkLst01] siteCd 불일치 감지: 요청={}, 토큰={} (userCd={})",
                    reqSiteCd, tokenSiteCd, tokenInfo.gv_userCd());
        }

        // workDate: YYYYMMDD 형식 + 유효 날짜 검증
        String workDate = request.getWorkDate();
        if (!StringUtils.hasText(workDate))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!workDate.matches("\\d{8}"))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        try {
            LocalDate.parse(workDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // prafta-036-C(H-3): cmpnyCd 토큰 캐노니컬라이즈
        return new InspectResultSaveParam(
            tokenInfo.gv_cmpnyCd()
            , tokenSiteCd
            , reqSiteCd
            , mismatch
            , request.getChkptCd()
            , workDate
            , request.getItems()
            , files
            , tokenInfo
        );
    }
}
