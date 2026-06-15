package com.prafta.common.cmm.leave.promotion.autobatch.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.leave.promotion.autobatch.AutoBatchTargetVO;

/**
 * prafta-com-008-A-5: 자동배치 계산용 조회 전용 Mapper.
 *
 * <p>순수 계산({@code LeaveAutoBatchService})에 필요한 스냅샷(대상자/기존연차/초기부하/휴일)을 한 번에
 * 읽어와, 계산 자체는 메모리에서 결정적으로 수행한다(난수·NOW() 분기 금지, 도래 비교는 서비스 today).
 *
 * <p>모든 조회는 CMPNY_CD + SITE_CD(세션 고정 검증 통과값) + 노드 cascade(Attd12 RECURSIVE 패턴) 스코프로
 * 격리한다. SQL 규칙: leading comma, {@code #{...}} 바인딩, SELECT * 금지.
 *
 * <p>패키지는 {@code @MapperScan(com.prafta.**.**.mapper)} 에 잡히도록 {@code ...autobatch.mapper}
 * 에 둔다(XML 도 {@code .../autobatch/mapper/} 폴더 — 전 프로젝트 매퍼 공통 컨벤션).
 */
@Mapper
public interface LeaveAutoBatchMapper {

    /**
     * 자동배치 대상자(스코프 내 SECOND 도래자 + 미사용 잔여&gt;0)와 본연차 사용가능 구간(availFrom~availTo).
     *
     * <p>WebLeavePromo01Mapper.selectDesignateTargets 와 동일한 대상 정의를 쓰되, 계산에 필요한
     * availFrom/availTo(가장 임박 본연차 grant)와 잔여(remainingDays)만 싣는다. 정렬 userCd asc(결정성).
     */
    List<AutoBatchTargetVO> selectAutoBatchTargets(@Param("gvCmpnyCd") String gvCmpnyCd,
                                                   @Param("siteCd") String siteCd,
                                                   @Param("nodeCd") String nodeCd,
                                                   @Param("incSubNodeYn") String incSubNodeYn,
                                                   @Param("userNm") String userNm,
                                                   @Param("tenureFilter") String tenureFilter,
                                                   @Param("oneYearAgoYmd") String oneYearAgoYmd);

    /**
     * 사용자의 윈도 내 기존 CONFIRMED 연차일(START_DATE) 목록 — 가용일에서 제외(중복 금지).
     *
     * <p>촉진/일반 구분 없이 그 사용자가 이미 쉬는 날(SYS_ANNUAL CONFIRMED 종일/시간차 불문 START_DATE)을
     * 모두 본다. DIRECT_USE_KEY(USER|DATE|SYS_ANNUAL) 충돌 사전 회피.
     */
    List<String> selectUserConfirmedLeaveYmds(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("userCd") String userCd,
                                              @Param("windowFrom") String windowFrom,
                                              @Param("windowTo") String windowTo);

    /**
     * 스코프 내 윈도 기간의 일자별 기존 CONFIRMED 연차 인원(초기부하). MIN_OVERLAP 초기 load 시드.
     *
     * <p>대상 = 스코프(사업장+노드 cascade) 활성 사용자의 SYS_ANNUAL CONFIRMED START_DATE 별 사용자 수.
     * 1차 지정 등 기존 등록분을 빈 캘린더가 아니라 실제 부하로 반영(autobatch §4-2).
     */
    List<DateCountRow> selectScopeDailyLeaveLoad(@Param("gvCmpnyCd") String gvCmpnyCd,
                                                 @Param("siteCd") String siteCd,
                                                 @Param("nodeCd") String nodeCd,
                                                 @Param("incSubNodeYn") String incSubNodeYn,
                                                 @Param("windowFrom") String windowFrom,
                                                 @Param("windowTo") String windowTo);

    /** 회사 일자휴일(tb_holiday) YYYYMMDD 목록(USE_YN='Y'). 윈도 기간만. */
    List<String> selectHolidayYmds(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("windowFrom") String windowFrom,
                                   @Param("windowTo") String windowTo);

    /** 회사 매년 반복 휴일(tb_holiday_rule) 'MMDD' 목록(USE_YN='Y'). */
    List<String> selectHolidayRuleMmdds(@Param("cmpnyCd") String cmpnyCd);

    /** 일자별 인원 운반(초기부하 시드). */
    class DateCountRow {
        private String ymd;
        private int cnt;

        public String getYmd() {
            return ymd;
        }

        public void setYmd(String ymd) {
            this.ymd = ymd;
        }

        public int getCnt() {
            return cnt;
        }

        public void setCnt(int cnt) {
            this.cnt = cnt;
        }
    }
}
