package com.prafta.common.cmm.dailyjoin.application.param;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.cmm.dailyjoin.dto.request.AgrTermsRequest;
import com.prafta.common.cmm.dailyjoin.dto.request.InsertDailyUserRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 일일사용자 회원가입 파라미터.
 * 비로그인 외부 화면 요청이므로 회사/사업장 정보를 본문에서 받는다.
 * 약관 버전은 클라이언트 신뢰값이 아닌 서버 조회값을 저장하므로 termsId 만 수신한다.
 */
public record InsertDailyUserParam(
    String cmpnyCd
    , String siteCd
    , String userId
    , String userNm
    , String mblNo
    , String certNo
    , List<AgrTerms> agrTermsList
) {

    /** 약관 동의 항목(파라미터 내부 표현). */
    public record AgrTerms(
        String termsId
    ) {
    }

    public static InsertDailyUserParam from(InsertDailyUserRequest request) {

        if (request == null
                || request.getCmpnyCd() == null || request.getCmpnyCd().isBlank()
                || request.getSiteCd() == null || request.getSiteCd().isBlank()
                || request.getUserId() == null || request.getUserId().isBlank()
                || request.getUserNm() == null || request.getUserNm().isBlank()
                || request.getMblNo() == null || request.getMblNo().isBlank()
                || request.getCertNo() == null || request.getCertNo().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 형식/길이 서버측 검증
        DailyJoinValidators.validateCmpnyCd(request.getCmpnyCd());
        DailyJoinValidators.validateSiteCd(request.getSiteCd());
        DailyJoinValidators.validateUserId(request.getUserId());
        DailyJoinValidators.validateUserNm(request.getUserNm());
        DailyJoinValidators.validateCertNo(request.getCertNo());
        DailyJoinValidators.validateMblNo(request.getMblNo());

        List<AgrTerms> agrTermsList = new ArrayList<>();
        if (request.getAgrTermsList() != null) {
            for (AgrTermsRequest agrTerms : request.getAgrTermsList()) {
                if (agrTerms == null
                        || agrTerms.getTermsId() == null || agrTerms.getTermsId().isBlank()) {
                    throw new ApiException(CommonErrorCode.COMMON_400_001);
                }
                agrTermsList.add(new AgrTerms(agrTerms.getTermsId()));
            }
        }

        return new InsertDailyUserParam(
            request.getCmpnyCd()
            , request.getSiteCd()
            , request.getUserId()
            , request.getUserNm()
            , request.getMblNo()
            , request.getCertNo()
            , agrTermsList
        );
    }
}
