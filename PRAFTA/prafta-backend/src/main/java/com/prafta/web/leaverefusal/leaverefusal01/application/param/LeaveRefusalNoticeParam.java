package com.prafta.web.leaverefusal.leaverefusal01.application.param;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.leaverefusal.leaverefusal01.application.model.LeaveRefusalNoticeModel;
import com.prafta.web.leaverefusal.leaverefusal01.dto.request.LeaveRefusalNoticeRequest;

/**
 * 노무수령거부 통지 발송 파라미터 (PRAFTA-COM-001 기능1).
 *
 * <p>List 요청을 JWT 식별자와 결합하여 캐노니컬 모델 목록으로 변환하며, null/필수값/형식을
 * 검증한다(위반 시 {@link CommonErrorCode#COMMON_400_001}). 식별자(cmpnyCd/userCd)는
 * 요청이 아니라 JWT(TokenInfo)에서만 도출한다(IDOR 가드).
 */
public record LeaveRefusalNoticeParam(
        String gvCmpnyCd,
        String gvUserCd,
        String gvAuthCd,
        List<LeaveRefusalNoticeModel> targets
) {
    private static final Pattern YMD8 = Pattern.compile("^\\d{8}$");
    // 코드 형식 화이트리스트: 영숫자/언더스코어/하이픈. userCd<=20(TB_USER.USER_CD), siteCd<=50(TB_USER.SITE_CD).
    private static final Pattern USER_CD = Pattern.compile("^[A-Za-z0-9_-]{1,20}$");
    private static final Pattern SITE_CD = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");

    public static LeaveRefusalNoticeParam from(List<LeaveRefusalNoticeRequest> requests, TokenInfo tokenInfo) {

        if (tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (requests == null || requests.isEmpty()) {
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "통지 대상이 비어있습니다");
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        if (isBlank(cmpnyCd) || isBlank(userCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        List<LeaveRefusalNoticeModel> models = new ArrayList<>(requests.size());
        for (LeaveRefusalNoticeRequest r : requests) {
            if (r == null) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "통지 대상 항목이 null 입니다");
            }
            if (isBlank(r.getSiteCd())) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "siteCd 누락");
            }
            if (!SITE_CD.matcher(r.getSiteCd().trim()).matches()) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "siteCd 형식 오류");
            }
            if (isBlank(r.getUserCd())) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "userCd 누락");
            }
            if (!USER_CD.matcher(r.getUserCd().trim()).matches()) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "userCd 형식 오류");
            }
            if (isBlank(r.getTargetYmd()) || !YMD8.matcher(r.getTargetYmd()).matches()) {
                throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "targetYmd 형식 오류(YYYYMMDD)");
            }
            models.add(new LeaveRefusalNoticeModel(
                    cmpnyCd,
                    r.getSiteCd().trim(),
                    r.getUserCd().trim(),
                    r.getTargetYmd().trim(),
                    userCd
            ));
        }

        return new LeaveRefusalNoticeParam(cmpnyCd, userCd, tokenInfo.gv_authCd(), models);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
