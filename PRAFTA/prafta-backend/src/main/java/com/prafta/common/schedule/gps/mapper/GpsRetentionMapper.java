package com.prafta.common.schedule.gps.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 위치정보(GPS 좌표) 보존기간 파기 매퍼 — 위치기반서비스 이용약관 제7조 ①.
 *
 * <h3>★대상 3종</h3>
 * <ul>
 *   <li>{@code TB_USER_ATTD_GPS} — 출퇴근(출역) 좌표. 사업장 지오펜스 반경을 벗어난 건에 한하여 적재된다.</li>
 *   <li>{@code TB_TBM_ATTENDANCE} — TBM 입실 시 확인된 참석자 좌표({@code ENTRY_GPS_*}).</li>
 *   <li>{@code TB_TBM_SESSION} — TBM 세션을 개설한 관리자 좌표({@code MANAGER_GPS_*}).</li>
 * </ul>
 * 종전에는 출퇴근 좌표만 약관에 적혀 있었으나, TBM 두 테이블도 동일하게 개인위치정보를 저장하고
 * 있어 약관 제7조를 확장하고 파기 대상에 편입했다(2026-09-01).
 *
 * <h3>★★행을 지우지 않고 좌표 컬럼만 NULL 로 만든다</h3>
 * 세 테이블 모두 위치정보 외에 <b>파기 대상이 아닌 업무 기록</b>을 함께 담고 있다.
 * <ul>
 *   <li>{@code TB_USER_ATTD_GPS} — 외근 사유({@code OFFSITE_REASON}), 근태 연결키({@code ATTD_ID})</li>
 *   <li>{@code TB_TBM_ATTENDANCE} — 이수 여부·서명 등 산업안전보건법상 3년 보존 대상인 교육 참석 기록</li>
 *   <li>{@code TB_TBM_SESSION} — 세션 자체(교육 실시 기록)</li>
 * </ul>
 * 행을 삭제하면 "외근이었다 / 교육을 이수했다"까지 함께 사라져 고객사의 법정 보존 의무를 깨뜨린다.
 * 좌표만 지우면 <b>"어디였는지"</b>만 사라지고 나머지 기록은 남는다.
 *
 * <p>★평문 컬럼({@code LAT}/{@code LON} 등)도 함께 NULL 로 만든다. 암호문만 지우면
 * 읽기 fallback 규칙({@code GpsCoordCrypto} — {@code *_ENC} 가 NULL 이면 구 평문 컬럼)에 걸려
 * <b>파기했다고 생각한 좌표가 되살아난다</b>. 현재 평문 컬럼은 전량 소거된 상태지만
 * (GPS좌표-암호화-전환), 규칙 자체가 살아 있는 한 쌍으로 지우는 것이 안전하다.
 *
 * <p>★{@code UPDATE_NO}/{@code UPDATE_DATE} 는 건드리지 않는다(TBM 두 테이블). 업무 내용은
 * 바뀌지 않았는데 최종 수정자를 배치로 덮으면 실제 마지막 업무 수정자를 잃는다.
 * 파기 실행 기록은 배치 로그로 남긴다.
 *
 * <p>★파기 기준 시각은 모두 <b>DB {@code NOW()}</b> 다. 애플리케이션 시계와 DB 시계를 섞지 않는다.
 */
@Mapper
public interface GpsRetentionMapper {

    /**
     * 파기 대상 출퇴근 좌표 건수(dry-run 및 실행 후 검증용).
     *
     * @param retentionMonths 보존 개월수(기본 36 = 3년)
     * @return 아직 좌표가 남아 있는 보존기간 경과 행 수
     */
    int countExpiredAttdGps(@Param("retentionMonths") int retentionMonths);

    /**
     * 보존기간이 지난 출퇴근 좌표 파기(1회 호출 = 최대 {@code batchSize} 행).
     *
     * <p>★기준 컬럼은 {@code API_CALL_DATE}(측정일자, {@code varchar(8)} YYYYMMDD) 다.
     *    약관이 말하는 "수집일"과 정확히 같은 값이고 NOT NULL 이다.
     *
     * @return 실제 파기된 행 수(0 이면 더 이상 대상 없음)
     */
    int purgeAttdGps(@Param("retentionMonths") int retentionMonths
            , @Param("batchSize") int batchSize);

    /** 파기 대상 TBM 입실 좌표 건수. */
    int countExpiredTbmAttendanceGps(@Param("retentionMonths") int retentionMonths);

    /**
     * 보존기간이 지난 TBM 입실 좌표 파기(1회 호출 = 최대 {@code batchSize} 행).
     *
     * <p>★기준 컬럼은 {@code INSERT_DATE} 다. 입실 시각({@code ENTRY_AT})은 nullable 이라
     *    기준으로 쓰면 NULL 인 행이 <b>영원히 파기되지 않는다</b>. {@code INSERT_DATE} 는
     *    NOT NULL 이고 입실 행 생성 시각이라 수집 시점과 사실상 동일하다.
     */
    int purgeTbmAttendanceGps(@Param("retentionMonths") int retentionMonths
            , @Param("batchSize") int batchSize);

    /** 파기 대상 TBM 관리자(개설자) 좌표 건수. */
    int countExpiredTbmSessionGps(@Param("retentionMonths") int retentionMonths);

    /**
     * 보존기간이 지난 TBM 관리자 좌표 파기(1회 호출 = 최대 {@code batchSize} 행).
     *
     * <p>★기준 컬럼은 {@code INSERT_DATE} 다({@code OPENED_AT} 은 nullable — 위 사유 동일).
     *    {@code INSERT_DATE} 는 {@code OPENED_AT} 보다 이르거나 같으므로 파기가 늦어지지 않는다.
     */
    int purgeTbmSessionGps(@Param("retentionMonths") int retentionMonths
            , @Param("batchSize") int batchSize);
}
