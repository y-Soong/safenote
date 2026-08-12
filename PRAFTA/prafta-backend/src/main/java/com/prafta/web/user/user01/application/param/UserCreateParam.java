package com.prafta.web.user.user01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.dto.request.UserCreateRequest;

/**
 * 관리자 단건 사용자 생성 파라미터 (PRAFTA-036).
 *
 * <p>회사 스코프/입력자/요청자 권한은 토큰에서만 가져온다(IDOR 방지).
 * 본 record 는 단건 생성과 엑셀 업로드(행 단위 재사용) 모두에서 입력 매개체로 쓰인다.
 */
public record UserCreateParam(
    String userId
    , String userNm
    , String authCd
    , String siteNo
    , String nodeCd
    , String mblNo
    , String email
    , String gender
    , String birthDt
    , String rankCd
    , String hireDate
    , String employmentType
    , String contractEndDate
    , Integer creditMonths
    , String creditReasonType
    , String creditReasonDetail
    // PRAFTA-037-F7 — 추가 권한 사이트 코드 목록 (기본 siteCd 외). 엑셀 업로드는 null/빈 리스트.
    , List<String> additionalSiteCdList
    // PRAFTA-COM-008-E-5 — 기본 근무타입(SCH_CD, 선택). 엑셀 업로드는 null.
    , String defaultSchCd
    // 소정-03 — 계정 생성 시 소정근로시간 필수 입력 (FULL:풀타임 / DIRECT:직접 입력).
    , String stdWorkType
    // 소정-03 — 주 소정근로 분 (stdWorkType=DIRECT 필수).
    , Integer stdWorkWeekMinutes
    // 소정-03 — 사유코드[SYS083] (미입력 시 회사 통상 기준값 비교로 자동 판정).
    , String stdWorkReasonCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvAuthLevel
) {

    public static UserCreateParam from(UserCreateRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 권한/회사 스코프 강제용 클레임 필수.
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new UserCreateParam(
            request.getUserId()
            , request.getUserNm()
            , request.getAuthCd()
            , request.getSiteNo()
            , request.getNodeCd()
            , request.getMblNo()
            , request.getEmail()
            , request.getGender()
            , request.getBirthDt()
            , request.getRankCd()
            , request.getHireDate()
            , request.getEmploymentType()
            , request.getContractEndDate()
            , request.getCreditMonths()
            , request.getCreditReasonType()
            , request.getCreditReasonDetail()
            , request.getAdditionalSiteCdList()
            , request.getDefaultSchCd()
            , request.getStdWorkType()
            , request.getStdWorkWeekMinutes()
            , request.getStdWorkReasonCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_authLevel()
        );
    }
}
