package com.prafta.common.cmm.dailyjoin.service;

import com.prafta.common.cmm.dailyjoin.application.param.InsertDailyUserParam;
import com.prafta.common.cmm.dailyjoin.application.param.SiteInfoParam;
import com.prafta.common.cmm.dailyjoin.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.dailyjoin.dto.response.InsertDailyUserResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.UserIdDupleCheckResponse;

/**
 * 일일사용자 회원가입(비로그인 외부 화면) 서비스.
 */
public interface DailyJoinService {

    /** joinCd 기반 회사/사업장 정보 조회. */
    SiteInfoResponse selectSiteInfo(SiteInfoParam param);

    /** 사용자ID 중복체크. */
    UserIdDupleCheckResponse checkUserIdDuple(UserIdDupleCheckParam param);

    /** 일일사용자 회원가입. */
    InsertDailyUserResponse insertDailyUser(InsertDailyUserParam param);
}
