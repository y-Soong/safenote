package com.prafta.common.cmm.baseinfo.application.query;

import com.prafta.common.cmm.baseinfo.application.param.SiteInfoParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

public record SiteInfoQuery(
		String cmpnyCd
		, String userCd
		, String siteNo
		, String siteNm
		// 사용여부 필터('Y'/'N', 빈 값=전체). 일반 조회 전용(회원가입은 joinMode 로 'Y' 강제).
		, String useYn
		// 회원가입 전용 활성기간 필터 플래그("Y" 면 개시일 이전·종료일 이후 사업장 제외 + USE_YN='Y' 강제). 일반 조회는 null.
		, String joinMode
) {
	public static SiteInfoQuery from(SiteInfoParam param) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SiteInfoQuery(
    		param.cmpnyCd()
    		, param.userCd()
    		, param.siteNo()
    		, param.siteNm()
    		, param.useYn()  // 일반 조회 — 화면에서 선택한 사용여부 필터(없으면 전체)
    		, null  // 일반(로그인 후) 조회 — 활성기간/USE_YN 강제 필터 미적용(기존 동작 보존)
        );
    }
}
