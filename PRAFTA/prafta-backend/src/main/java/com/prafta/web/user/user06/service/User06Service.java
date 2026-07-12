package com.prafta.web.user.user06.service;

import com.prafta.web.user.user06.application.param.BlacklistListParam;
import com.prafta.web.user.user06.application.param.BlacklistRegParam;
import com.prafta.web.user.user06.application.param.BlacklistReleaseParam;
import com.prafta.web.user.user06.dto.response.BlacklistListResponse;
import com.prafta.web.user.user06.dto.response.BlacklistRegResponse;

public interface User06Service {

    /** 일일계정 블랙리스트 목록 조회(회사 스코프, 휴대폰 마스킹 응답). */
    BlacklistListResponse selectBlacklistList(BlacklistListParam param);

    /** 블랙리스트 등록(평문 휴대폰 → HMAC/ENC/LAST4 파생, 채번 후 INSERT). */
    BlacklistRegResponse insertBlacklist(BlacklistRegParam param);

    /** 블랙리스트 해제(USE_YN 'Y'→'N'). 대상 없으면 404. */
    void releaseBlacklist(BlacklistReleaseParam param);
}
