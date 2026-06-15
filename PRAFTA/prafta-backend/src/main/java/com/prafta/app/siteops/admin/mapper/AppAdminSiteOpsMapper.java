package com.prafta.app.siteops.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.siteops.admin.application.command.SiteOpsCheckInCommand;
import com.prafta.app.siteops.admin.application.command.SiteOpsCheckOutCommand;
import com.prafta.app.siteops.admin.result.SiteOpsOpenAttdResult;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 Mapper.
 *
 * <p>식별자(cmpnyCd/siteCd/userCd)는 token/서버 확정 출처(IDOR 차단). leading 콤마, #{} 바인딩, SELECT * 금지.
 */
@Mapper
public interface AppAdminSiteOpsMapper {

    /** 현장전환 사업장이 토큰 사용자의 접근가능 사업장(USE_YN='Y')인지 검증(1=멤버십, 0=아님). */
    int existsAccessibleSite(@Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd, @Param("siteCd") String siteCd);

    /**
     * 대상 일용직 유효성(TBM countEntryTarget DAILY 분기 동일식). 1=유효, 0=무효.
     * USE_YN='Y' AND ACCOUNT_STATUS='01' AND WITHDRAWAL_DATE IS NULL AND WORK_EXPIRE_DATE>=오늘
     * AND 동일 회사/사업장.
     */
    int countValidDailyUser(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd);

    /**
     * 슬롯 점유 보강 검증(옵션B). CURR_USER_CD=userCd AND SLOT_STATUS='02'(점유) AND USE_YN='Y'
     * AND 동일 회사/사업장 슬롯이 1건 이상이면 1.
     */
    int countOccupiedSlot(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd);

    /** 토스트용 일용직 이름(평문). 마스킹은 서비스에서 수행. 없으면 null. */
    String selectDailyUserNm(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd);

    /** ATTD_ID 선채번(회사별 시퀀스). AppAttd01Mapper.selectAttdId 동형. */
    String selectAttdId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 동시성 직렬화용 일용직 행 비관적 잠금(TB_DAILY_USER FOR UPDATE). 자연키 UNIQUE 부재 보완 —
     * count→insert 경쟁을 트랜잭션 단위로 직렬화. 잠금은 커밋/롤백 시 자동 해제. 행이 있으면 USER_CD 반환.
     */
    String lockDailyUserForCheckIn(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd);

    /** 당일 출근행(DEL_YN='N', CHECK_IN_TIME IS NOT NULL) 수. WORK_SEQ 산정/멱등 판정. */
    int countAttdToday(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd,
            @Param("todayYmd") String todayYmd);

    /** 당일 열린(미퇴근) 출근행 수. S1 멱등(2중 열림 방지) 판정. */
    int countOpenAttdToday(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd,
            @Param("todayYmd") String todayYmd);

    /** 당일 열린(미퇴근) 출근행 1건(ATTD_ID, 최신). 퇴근 대상. 없으면 null. */
    SiteOpsOpenAttdResult selectOpenAttdToday(@Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd, @Param("userCd") String userCd,
            @Param("todayYmd") String todayYmd);

    /** 일용직 출근 INSERT(NODE_CD=NULL, deviceUuid=NULL). AppAttd01Mapper.insertCheckIn 포팅. */
    void insertDailyCheckIn(@Param("c") SiteOpsCheckInCommand command);

    /** 일용직 퇴근 UPDATE(WHERE CHECK_OUT_TIME IS NULL 가드). 영향 행수 반환(0=이미 퇴근=멱등). */
    int updateDailyCheckOut(@Param("c") SiteOpsCheckOutCommand command);
}
