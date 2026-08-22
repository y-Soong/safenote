package com.prafta.common.cmm.sch.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.sch.vo.DefaultSchUserVO;
import com.prafta.common.cmm.sch.vo.SchOptionVO;

/**
 * 기본근무 자동 스케줄 생성 공용 Mapper (PRAFTA-COM-008-E-3).
 *
 * <p>기본 근무타입(tb_user.DEFAULT_SCH_CD)이 설정된 교대 비소속자에게
 * 오늘~당해 12/31 평일(월~금) work_plan 을 멱등 생성한다(빈 날만, 덮어쓰기 금지).
 * SQL 규칙: leading comma, #{...} 바인딩, SELECT * 금지.
 */
@Mapper
public interface DefaultSchGenMapper {

    /** 기본 근무타입이 설정된 사용자가 1명 이상 존재하는 회사 CMPNY_CD 목록(배치 트리거1 대상). */
    List<String> selectDefaultSchCompanyCds();

    /**
     * 해당 회사에서 기본 근무타입이 설정된(DEFAULT_SCH_CD NOT NULL) 활성 사용자 목록.
     * 사용중(USE_YN='Y', ACCOUNT_STATUS='01')만. 교대 소속 여부는 호출부(서비스)에서 일자별로 판정한다.
     */
    List<DefaultSchUserVO> selectDefaultSchUsers(@Param("cmpnyCd") String cmpnyCd);

    /** 단건 사용자의 기본 근무타입 메타(DEFAULT_SCH_CD/SITE_CD) 조회. 미설정/없으면 null. */
    DefaultSchUserVO selectDefaultSchUser(@Param("cmpnyCd") String cmpnyCd,
                                          @Param("userCd") String userCd);

    /**
     * 빈 날(work_plan 미존재)에만 기본근무 work_plan 1일 INSERT. PK 충돌 시 무동작(멱등, 덮어쓰기 금지).
     * GEN_SOURCE='DEFAULT_SCH'. 호출부가 평일/교대비소속/미마감월을 사전 필터링한다.
     *
     * @return 신규 생성 1, 이미 존재(스킵) 0
     */
    int insertDefaultSchDayIfAbsent(@Param("cmpnyCd") String cmpnyCd,
                                    @Param("siteCd") String siteCd,
                                    @Param("userCd") String userCd,
                                    @Param("workYmd") String workYmd,
                                    @Param("schCd") String schCd,
                                    @Param("operatorNo") String operatorNo);

    /**
     * 기본근무 변경 시 미래 자동생성분만 새 SCH_CD 로 갱신.
     * 대상 = WORK_YMD &gt; fromYmd(=오늘) AND GEN_SOURCE='DEFAULT_SCH'. 수동/연차/교대/촉진 보존.
     * 마감월 제외는 호출부에서 처리(여기선 단순 미래 자동생성분 갱신).
     * E3(당일분모 전환, W6): 연차 잠금일(확정 비종일 연차 + 미결 시간차 신청) 제외 — 당일 분모 보존.
     * 제외는 조용히 skip(배치성 — 하드 실패 금지), 건수 로그는 {@link #countFutureDefaultSchLockedDays}.
     *
     * @return 갱신 행 수
     */
    int updateFutureDefaultSch(@Param("cmpnyCd") String cmpnyCd,
                               @Param("siteCd") String siteCd,
                               @Param("userCd") String userCd,
                               @Param("fromYmd") String fromYmd,
                               @Param("newSchCd") String newSchCd,
                               @Param("operatorNo") String operatorNo);

    /**
     * E3(W6) 보조: {@link #updateFutureDefaultSch} 가 연차 잠금(확정 비종일 연차 OR 미결 시간차)으로
     * 제외(보존)하는 미래 자동생성분 건수 — skip 로그 전용(read-only, 술어 동일).
     */
    int countFutureDefaultSchLockedDays(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("siteCd") String siteCd,
                                        @Param("userCd") String userCd,
                                        @Param("fromYmd") String fromYmd);

    /**
     * prafta-com-008-E-5/E-8: 사업장 활성(USE_YN='Y') 근무타입 옵션 목록.
     * User_01 기본 근무타입 select / 로그인 게이트 팝업의 선택지로 사용한다.
     */
    List<SchOptionVO> selectActiveSchOptions(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("siteCd") String siteCd);

    /**
     * prafta-com-008-E-5/E-8: 기본 근무타입 화이트리스트 검증용.
     * 대상 사업장(siteCd)에 USE_YN='Y' 로 존재하고, 현재본 또는 이력본 중 APPLY_DATE 가
     * asOfDate 이하인 버전이 하나라도 있으면 1, 아니면 0. (클라 신뢰 금지)
     *
     * <p>2026-08-22: 적용일자 검증 추가 — 호출부(create/edit/self-change/login gate, 전 경로)가
     * 전부 {@code applyDefaultSchChange} 의 "명일(오늘+1)부터 반영" 규칙을 따르므로 asOfDate 는
     * 호출부에서 항상 명일을 넘긴다(소속이동의 임의 미래일 이동일과는 별개 — 그쪽은
     * UserTransferMapper.selectSchUsableOnDate 로 분리).
     */
    int countActiveSchOnSite(@Param("cmpnyCd") String cmpnyCd,
                             @Param("siteCd") String siteCd,
                             @Param("schCd") String schCd,
                             @Param("asOfDate") String asOfDate);

    /** prafta-com-008-E-8: 단건 사용자의 SITE_CD 조회(게이트 검증 스코프 도출). 없으면 null. */
    String selectUserSiteCd(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd);

    /**
     * prafta-com-008-E-8: tb_user.DEFAULT_SCH_CD + DEFAULT_SCH_SET_DATE=NOW() 갱신(로그인 게이트 저장).
     * @return 갱신 행 수(1 이면 성공)
     */
    int updateUserDefaultSch(@Param("cmpnyCd") String cmpnyCd,
                             @Param("userCd") String userCd,
                             @Param("defaultSchCd") String defaultSchCd,
                             @Param("operatorNo") String operatorNo);

    /**
     * F3(G4): 자동생성 범위 내 회사 휴일(특정일) YMD 목록.
     * tb_holiday USE_YN='Y' 중 [fromYmd, toYmd] 범위의 HOLIDAY_YMD 를 YYYYMMDD 문자열로 반환.
     * 회사(CMPNY_CD) 스코프(tb_holiday 에 SITE_CD 없음). 루프 진입 전 1회 배치 로딩(일별 조회 폭증 방지).
     */
    List<String> selectHolidayYmds(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("fromYmd") String fromYmd,
                                   @Param("toYmd") String toYmd);

    /**
     * F3(G4): 회사 반복휴일(매년 고정 MM/DD) 목록. tb_holiday_rule USE_YN='Y' → 'MMDD' 문자열.
     * F2 노무수령거부 게이트(LeaveRefusalMapper)와 동일 소스 정합. 루프 진입 전 1회 배치 로딩.
     */
    List<String> selectHolidayRuleMmdds(@Param("cmpnyCd") String cmpnyCd);
}
