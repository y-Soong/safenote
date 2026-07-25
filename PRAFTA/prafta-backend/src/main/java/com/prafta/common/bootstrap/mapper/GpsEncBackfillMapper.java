package com.prafta.common.bootstrap.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.bootstrap.result.GpsEncBackfillRow;

/**
 * GPS좌표-암호화-전환-05: GPS 좌표 암호문 백필 전용 매퍼(일회성 러너 전용 — 서비스 경로에서 사용 금지).
 *
 * <p>대상 조회는 {@code *_ENC IS NULL AND 평문 IS NOT NULL} 조건(멱등 — 재실행 시 0건).
 * UPDATE 는 평문 컬럼을 건드리지 않는다(소거는 prafta-gps-enc-2-verify-and-purge.sql 수동 실행).
 */
@Mapper
public interface GpsEncBackfillMapper {

    /** TB_USER_ATTD_GPS 백필 대상(암호문 미채움 + 평문 존재) 조회. */
    List<GpsEncBackfillRow> selectAttdGpsTargets();

    /** TB_USER_ATTD_GPS 암호문 채움(평문 무변경). 키=(CMPNY_CD, GPS_ID). */
    int updateAttdGpsEnc(@Param("cmpnyCd") String cmpnyCd, @Param("rowKey") String rowKey,
            @Param("latEnc") String latEnc, @Param("lonEnc") String lonEnc);

    /** TB_TBM_ATTENDANCE 백필 대상 조회. */
    List<GpsEncBackfillRow> selectTbmAttendanceTargets();

    /** TB_TBM_ATTENDANCE 암호문 채움. 키=(CMPNY_CD, ATTENDANCE_CD). */
    int updateTbmAttendanceEnc(@Param("cmpnyCd") String cmpnyCd, @Param("rowKey") String rowKey,
            @Param("latEnc") String latEnc, @Param("lonEnc") String lonEnc);

    /** TB_TBM_SESSION 백필 대상 조회. */
    List<GpsEncBackfillRow> selectTbmSessionTargets();

    /** TB_TBM_SESSION 암호문 채움. 키=(CMPNY_CD, SESSION_CD). */
    int updateTbmSessionEnc(@Param("cmpnyCd") String cmpnyCd, @Param("rowKey") String rowKey,
            @Param("latEnc") String latEnc, @Param("lonEnc") String lonEnc);
}
