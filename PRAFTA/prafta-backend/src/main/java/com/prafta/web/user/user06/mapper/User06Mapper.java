package com.prafta.web.user.user06.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user06.application.command.BlacklistInsertCommand;
import com.prafta.web.user.user06.application.command.BlacklistReleaseCommand;
import com.prafta.web.user.user06.application.query.BlacklistListQuery;
import com.prafta.web.user.user06.result.BlacklistRaw;

@Mapper
public interface User06Mapper {

    /**
     * 메뉴 버튼 권한 보유 카운트(서버측 역할 게이트 — User_06 메뉴).
     * TB_SYST_AUTH_MENU 에 CMPNY_CD + AUTH_CD + MENU_D_ID + USE_YN='Y' 이고 지정 버튼플래그가 'Y'인 행이 있으면 1 이상.
     * btnType 은 서비스 상수('SRCH'/'NEW'/'DELT')만 전달하며, XML 에서 고정 컬럼으로 분기(동적 ${} 미사용).
     */
    int selectMenuButtonAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("authCd") String authCd,
            @Param("menuDId") String menuDId, @Param("btnType") String btnType);

    /** 블랙리스트 목록 조회(회사 스코프, 전화/사용여부 필터, 등록자명 조인, 페이징 상한). */
    List<BlacklistRaw> selectBlacklistList(BlacklistListQuery query);

    /** 활성 블랙리스트 등록 여부 카운트(CMPNY_CD + MBL_NO_HMAC + USE_YN='Y'). 1 이상이면 중복. */
    int selectActiveBlacklistCnt(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

    /** 블랙리스트ID 채번('B' + YYYYMMDD + 시퀀스). */
    String selectBlacklistId(@Param("cmpnyCd") String cmpnyCd);

    /** 블랙리스트 INSERT(USE_YN='Y' 고정). */
    void insertBlacklist(BlacklistInsertCommand command);

    /** 블랙리스트 해제(USE_YN 'Y'→'N', 회사 스코프 조건부 UPDATE). 영향행 수 반환. */
    int releaseBlacklist(BlacklistReleaseCommand command);
}
