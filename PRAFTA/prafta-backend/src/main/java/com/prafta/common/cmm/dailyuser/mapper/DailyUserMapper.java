package com.prafta.common.cmm.dailyuser.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 일용직 사용자(TB_DAILY_USER) 공용 매퍼.
 *
 * <p>PRAFTA-app-027-1 — 자정 만료 배치에서 만료 계정을 일괄 비활성으로 전이한다.
 */
@Mapper
public interface DailyUserMapper {

    /**
     * PRAFTA-app-027-1 — 만료(WORK_EXPIRE_DATE &lt; 오늘) AND 활성(USE_YN='Y') 일용직을
     * USE_YN='N' + ACCOUNT_STATUS='05'(비활성화)로 일괄 전이한다.
     *
     * <p>이미 'N'인 행은 조건에서 제외되어 영향 0(멱등).
     *
     * @param todayYmd 서버 기준 오늘(YYYYMMDD)
     * @param updateNo 수정자(시스템 배치 = "SYSTEM")
     * @return 전이된 행 수
     */
    int updateExpireDailyUsers(@Param("todayYmd") String todayYmd, @Param("updateNo") String updateNo);

    /**
     * 만료 일용직이 점유 중이던 슬롯 반납(SLOT_STATUS='01' + CURR_USER_CD=NULL).
     *
     * <p>고정슬롯(FIXED_YN='Y')은 반납하지 않고 점유를 유지한다.
     * 이미 비점유('02' 아님)인 슬롯은 영향 0(멱등).
     *
     * @param todayYmd 서버 기준 오늘(YYYYMMDD)
     * @param updateNo 수정자(시스템 배치 = "SYSTEM")
     * @return 반납된 슬롯 수
     */
    int releaseExpiredDailyUserSlots(@Param("todayYmd") String todayYmd, @Param("updateNo") String updateNo);

    /**
     * 통합형 — 만료된 일용직의 TB_USER 행을 비활성(USE_YN='N' + ACCOUNT_STATUS='05')으로 전이한다.
     *
     * <p>EMPLOYMENT_TYPE='DAILY' 가드로 정규 사용자의 오전이를 절대 차단한다(보안 핵심).
     * 이미 'N'인 행은 영향 0(멱등).
     *
     * @param todayYmd 서버 기준 오늘(YYYYMMDD)
     * @param updateNo 수정자(시스템 배치 = "SYSTEM")
     * @return 전이된 행 수
     */
    int deactivateExpiredTbUser(@Param("todayYmd") String todayYmd, @Param("updateNo") String updateNo);

    /**
     * 통합형 — 만료된 일용직의 TB_USER_SITE_AUTH 권한 행을 비활성(USE_YN='N')으로 전이한다.
     *
     * <p>EMPLOYMENT_TYPE='DAILY' 가드로 정규 사용자 권한 오전이를 차단한다.
     * 이미 'N'인 행은 영향 0(멱등).
     *
     * @param todayYmd 서버 기준 오늘(YYYYMMDD)
     * @param updateNo 수정자(시스템 배치 = "SYSTEM")
     * @return 전이된 행 수
     */
    int deactivateExpiredTbUserSiteAuth(@Param("todayYmd") String todayYmd, @Param("updateNo") String updateNo);
}
