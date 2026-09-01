package com.prafta.common.cmm.location.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.location.application.command.LocationPurgeHistCommand;
import com.prafta.common.cmm.location.mapper.result.LocationPurgeScopeResult;

/**
 * 위치정보 계정 단위 파기 매퍼 — 위치정보 동의철회·중지 S3.
 *
 * <h3>★대상 3종</h3>
 * <ul>
 *   <li>{@code TB_USER_ATTD_GPS} — 출퇴근(출역) 좌표</li>
 *   <li>{@code TB_TBM_ATTENDANCE} — TBM 입실 시 본인 좌표</li>
 *   <li>{@code TB_TBM_SESSION} — <b>본인이 개설한</b> 세션의 관리자 좌표</li>
 * </ul>
 *
 * <h3>★행을 지우지 않고 좌표 컬럼만 NULL 로 만든다</h3>
 * 세 테이블 모두 외근 사유·교육 이수 등 파기 대상이 아닌 업무 기록을 함께 담고 있다.
 * 행을 지우면 고객사의 법정 보존 의무(근로기준법·산업안전보건법 3년)를 깨뜨린다.
 * 좌표만 지우면 <b>"어디였는지"</b> 만 사라지고 나머지는 남는다.
 *
 * <p>★평문/암호문을 <b>쌍으로</b> NULL 처리한다. 읽기 fallback 규칙이
 * "{@code *_ENC} 가 NULL 이면 구 평문 컬럼"({@code GpsCoordCrypto}) 이라 암호문만 지우면
 * <b>파기했다고 생각한 좌표가 되살아난다.</b>
 *
 * <p>★{@code UPDATE_NO}/{@code UPDATE_DATE} 는 건드리지 않는다(TBM 두 테이블). 업무 내용은
 * 바뀌지 않았는데 최종 수정자를 파기로 덮으면 실제 마지막 업무 수정자를 잃는다.
 * 파기 사실은 {@code TB_LOCATION_PURGE_HIST} 로 남긴다.
 *
 * <p>배치({@code GpsRetentionMapper})와 달리 <b>LIMIT 을 두지 않는다</b> — 대상이 한 계정
 * 분량이라 트랜잭션이 길어지지 않고, 철회는 "지체 없이" 전량 파기해야 한다(법 제24조).
 */
@Mapper
public interface LocationPurgeMapper {

    /**
     * 파기 대상 집계 — 실행 <b>전</b>에 호출해 이력에 남길 건수/기간을 확보한다.
     *
     * <p>파기 후에는 대상이 사라져 집계할 수 없으므로 순서가 중요하다.
     */
    LocationPurgeScopeResult selectPurgeScope(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("userTypeCd") String userTypeCd);

    /** 출퇴근 좌표 파기. 실제 파기된 행 수 반환. */
    int purgeAttdGpsByUser(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd);

    /** TBM 입실 좌표 파기(계정 계통 포함 — USER_CD 는 계통별로 중복될 수 있다). */
    int purgeTbmAttendanceGpsByUser(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd
            , @Param("userTypeCd") String userTypeCd);

    /**
     * TBM 개설자(관리자) 좌표 파기.
     *
     * <p>★세션 자체는 다른 참석자들의 교육 기록이므로 절대 지우지 않는다.
     * 개설자 본인의 좌표만 NULL 로 만든다.
     */
    int purgeTbmSessionGpsByUser(
            @Param("cmpnyCd") String cmpnyCd
            , @Param("userCd") String userCd);

    /**
     * 파기 이력 INSERT(append-only).
     *
     * <p>★★좌표 값은 어떤 형태로도(원본·해시·마스킹·부분값) 남기지 않는다.
     * 남기는 것은 "누가·언제·무엇을·몇 건·왜" 뿐이다.
     */
    int insertPurgeHist(LocationPurgeHistCommand command);
}
