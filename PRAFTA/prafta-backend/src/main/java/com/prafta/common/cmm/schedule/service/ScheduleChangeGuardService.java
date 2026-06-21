package com.prafta.common.cmm.schedule.service;

import java.util.List;

import com.prafta.common.cmm.schedule.vo.ScheduleLockVO;

/**
 * 공통 스케줄 변경 가드 서비스 (prafta-com-016 shared-schedule-guard).
 *
 * <p><b>원칙</b>: 확정 연차(종일/반차/시간차 — USE_UNIT_TYPE 무관) 또는 초과근무(등록/신청)가 있는
 * (사용자, 날짜)는 그 날의 근무 스케줄을 변경할 수 없다. 본 서비스는 (cmpnyCd, siteCd, userCd, 날짜목록)을
 * 받아 잠긴 날짜 + 사유({@link ScheduleLockVO.Reason#LEAVE} / {@link ScheduleLockVO.Reason#OT})를 반환한다.
 * 판정만 수행하며(read-only), 차단/skip 의 정책 결정은 각 호출 경로가 한다.
 *
 * <h3>호출 가이드 (경로별)</h3>
 * <ul>
 *   <li><b>① 교대 덮어쓰기(016-D)</b>: {@link #findLockedDays}로 잠긴 날을 받아 그 날만 덮어쓰기 skip.
 *       종일 포함 모든 연차 + OT 대상(USE_UNIT_TYPE 무관). 016-D-4 팝업에 잠긴 날 목록 표시.</li>
 *   <li><b>② Attd_05 직접 셀 변경/삭제(016-C)</b>: {@link #findLockedDays}로 잠긴 셀을 받아 변경 거부(skip)
 *       + BatchResultPop 안내. 종일 포함 모든 연차 + OT 대상.</li>
 *   <li><b>③ 근무타입(SCH_CD) 시간/휴게 변경(016-A)</b>: 그 근무타입을 쓰는 미래 적용분(APPLY_DATE) 날짜
 *       목록을 모아 {@link #findLockedDays}로 판정 후, <b>시간차·반차 연차 + OT 만</b> 하드 차단 대상으로
 *       필터한다(종일 연차는 시간 무관이라 제외). 종일 제외 필터는 016-A 호출부가
 *       {@link ScheduleLockVO#getLeaveUseUnitType()} 가 '00' 인 LEAVE 잠금을 걸러서 수행한다.</li>
 * </ul>
 *
 * <p>※ 일(日) 단위 판정만 제공한다. 월 단위가 필요한 경로는 호출부가 해당 월 일자목록을 만들어 넘긴다
 *   (예: 016-C-3 월 부분삭제 — 삭제 대상 일자목록을 넘겨 OT 잠긴 날만 제외).
 */
public interface ScheduleChangeGuardService {

    /**
     * 입력 날짜목록 중 잠긴(연차 또는 OT) 날짜를 사유와 함께 반환한다.
     * 같은 날짜가 연차·OT 양쪽이면 둘 다(각 1건씩) 포함된다. 잠긴 날이 없으면 빈 리스트.
     *
     * @param cmpnyCd  회사 코드
     * @param siteCd   사업장 코드(OT 판정에 사용)
     * @param userCd   대상 사용자 코드
     * @param workYmds 판정 대상 날짜목록(YYYYMMDD). null/빈 목록이면 빈 리스트 반환.
     * @return 잠긴 날짜 + 사유 목록
     */
    List<ScheduleLockVO> findLockedDays(String cmpnyCd, String siteCd, String userCd, List<String> workYmds);
}
