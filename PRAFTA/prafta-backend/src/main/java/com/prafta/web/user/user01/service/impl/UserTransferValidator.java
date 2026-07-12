package com.prafta.web.user.user01.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.shift.service.ShiftMembershipService;
import com.prafta.common.error.user.UserErrorCode;
import com.prafta.web.user.user01.mapper.UserTransferMapper;
import com.prafta.web.user.user01.result.PartialLeaveTimeResult;
import com.prafta.web.user.user01.result.SchSegmentTimeResult;
import com.prafta.web.user.user01.result.TransferBlockReason;
import com.prafta.web.user.user01.result.UserTransferBasicResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소속이동 5종 불가케이스 사전 검증기 (정규직 한정) — PRAFTA-WEB_001-1.
 *
 * <p>판정 결과를 {@link TransferBlockReason} 목록으로 반환한다(다중 사유 누적).
 * eligibility 조회(다중 사유 표시)와 등록 검증(첫 사유로 fail-closed)에서 공통 사용한다.
 *
 * <p>정규직 정의 = EMPLOYMENT_TYPE != 'DAILY'(NULL 포함이 정규직). 일용직은 본 검증을 호출하지 않는다.
 *
 * <p>판정 기준(확정 결정사항):
 * <ul>
 *   <li>① 사업장 관리자 = 대상자가 현재 사업장 SITE_ADMIN_CD.</li>
 *   <li>② 노드 마지막 담당자 = 대상자가 MAIN/SUB 담당자이고 제거 시 담당자 0명.</li>
 *   <li>③ 순회점검 담당자 = 대상자가 TB_CHKPT_TYPE_MGMT.MGMT_USER_CD(USE_YN='Y').</li>
 *   <li>④ 교대조 소속 = 이동일 기준 {@link ShiftMembershipService#isInShiftTeamOn}.</li>
 *   <li>⑤ 시간차 연차 미커버 = 현재/미래 부분연차([START_TIME,END_TIME])를 기본근무타입 근무구간 합집합이 완전히 감싸지 못함.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTransferValidator {

    private final UserTransferMapper userTransferMapper;
    private final ShiftMembershipService shiftMembershipService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 5종 불가케이스를 평가해 불가 사유 목록을 반환한다(빈 목록이면 가능).
     *
     * @param cmpnyCd         회사 코드(토큰 도출값, 스코프)
     * @param target          대상 사용자 기본 정보(현재 사업장/부서/고용형태)
     * @param toDefaultSchCd  관리자가 지정한 기본 근무타입(⑤ 판정용, null 이면 ⑤ 생략)
     * @param toSiteCd        이동 사업장(⑤ 근무타입 effective 조회 스코프). null 이면 대상자 현재 사업장으로 폴백.
     * @param moveDate        소속이동일 YYYYMMDD(④/⑤ 판정용, null/blank 이면 ④는 오늘 기준, ⑤는 생략)
     * @return 불가 사유 목록(없으면 빈 목록)
     */
    public List<TransferBlockReason> evaluate(String cmpnyCd, UserTransferBasicResult target, String toDefaultSchCd,
            String toSiteCd, String moveDate) {

        List<TransferBlockReason> reasons = new ArrayList<>();
        String userCd = target.userCd();
        String fromSiteCd = target.siteCd();

        // ① 사업장 관리자
        if (fromSiteCd != null && !fromSiteCd.isBlank()
                && userTransferMapper.selectIsSiteAdmin(cmpnyCd, fromSiteCd, userCd) > 0) {
            reasons.add(reason(UserErrorCode.USER_400_065));
        }

        // ② 노드 마지막 담당자(정/부 불문)
        if (userTransferMapper.selectLastAdminNodeCnt(cmpnyCd, userCd) > 0) {
            reasons.add(reason(UserErrorCode.USER_400_066));
        }

        // ③ 순회점검 담당자
        if (userTransferMapper.selectIsChkptManager(cmpnyCd, userCd) > 0) {
            reasons.add(reason(UserErrorCode.USER_400_067));
        }

        // ④ 교대조 소속(이동일 기준, 이동일 없으면 오늘 기준)
        String shiftJudgeDate = (moveDate != null && !moveDate.isBlank()) ? moveDate : LocalDate.now().format(YMD);
        if (shiftMembershipService.isInShiftTeamOn(cmpnyCd, fromSiteCd, userCd, shiftJudgeDate)) {
            reasons.add(reason(UserErrorCode.USER_400_068));
        }

        // ⑤ 시간차 연차 미커버(기본근무타입·이동일 모두 있어야 판정 가능)
        if (toDefaultSchCd != null && !toDefaultSchCd.isBlank()
                && moveDate != null && !moveDate.isBlank()) {
            if (hasUncoveredPartialLeave(cmpnyCd, userCd, toDefaultSchCd, toSiteCd, fromSiteCd, moveDate)) {
                reasons.add(reason(UserErrorCode.USER_400_069));
            }
        }

        return reasons;
    }

    /**
     * 불가⑤ 판정: 현재/미래 부분(시간차) 연차 중 기본근무타입 근무구간 합집합이 완전히 감싸지 못하는 행이 하나라도 있으면 true.
     *
     * <p>근무구간 = 기본근무타입(schCd)의 이동일 기준 effective 1구간(FST)·2구간(SEC) 합집합.
     * 자정을 넘기는 구간(end &lt;= start)은 +1440분으로 보정 후 병합한다.
     */
    private boolean hasUncoveredPartialLeave(String cmpnyCd, String userCd, String schCd,
            String toSiteCd, String fromSiteCd, String moveDate) {

        List<PartialLeaveTimeResult> leaves = userTransferMapper.selectFuturePartialLeaves(cmpnyCd, userCd, moveDate);
        if (leaves == null || leaves.isEmpty()) {
            return false; // 시간차 연차 없음 → 커버리지 판정 불필요(불가 아님)
        }

        // 근무타입 시간은 이동 사업장 스코프로 조회(미지정 시 대상자 현재 사업장 폴백).
        String schSiteCd = (toSiteCd != null && !toSiteCd.isBlank()) ? toSiteCd : fromSiteCd;
        SchSegmentTimeResult seg = userTransferMapper.selectEffectiveSchSegment(cmpnyCd, schSiteCd, schCd, moveDate);

        // 근무타입/시간을 찾지 못하면 어떤 연차도 감쌀 수 없음 → 불가(fail-closed).
        if (seg == null) {
            log.warn("소속이동 불가⑤ 판정 - 기본근무타입 시간 조회 실패(불가 처리) cmpnyCd={}, schCd={}, siteCd={}",
                    cmpnyCd, schCd, schSiteCd);
            return true;
        }

        List<int[]> workWindows = mergeIntervals(toIntervals(seg));
        if (workWindows.isEmpty()) {
            log.warn("소속이동 불가⑤ 판정 - 기본근무타입 근무구간 없음(불가 처리) cmpnyCd={}, schCd={}", cmpnyCd, schCd);
            return true;
        }

        for (PartialLeaveTimeResult leave : leaves) {
            Integer ls = toMinutes(leave.startTime());
            Integer le = toMinutes(leave.endTime());
            if (ls == null || le == null) {
                // 시간 파싱 불가(데이터 이상) → 보수적으로 불가 처리.
                log.warn("소속이동 불가⑤ 판정 - 연차 시간 파싱 실패(불가 처리) leaveId={}, start={}, end={}",
                        leave.leaveId(), leave.startTime(), leave.endTime());
                return true;
            }
            if (le <= ls) {
                le += 1440; // 자정을 넘기는 연차 구간 보정
            }
            if (!isCovered(ls, le, workWindows)) {
                return true; // 감싸지 못하는 연차 존재 → 불가
            }
        }
        return false;
    }

    /** 근무구간(1/2구간)을 [start,end] 분 단위 구간 목록으로 변환(자정 넘김 보정 포함). */
    private List<int[]> toIntervals(SchSegmentTimeResult seg) {
        List<int[]> intervals = new ArrayList<>();
        addInterval(intervals, seg.fstSchStrTime(), seg.fstSchEndTime());
        addInterval(intervals, seg.secSchStrTime(), seg.secSchEndTime());
        return intervals;
    }

    private void addInterval(List<int[]> intervals, String strTime, String endTime) {
        Integer s = toMinutes(strTime);
        Integer e = toMinutes(endTime);
        if (s == null || e == null) {
            return;
        }
        if (e <= s) {
            e += 1440; // 오버나이트 근무 보정
        }
        intervals.add(new int[]{s, e});
    }

    /** 구간 목록을 시작순 정렬 후 겹치거나 맞닿는 구간을 병합한다. */
    private List<int[]> mergeIntervals(List<int[]> intervals) {
        List<int[]> merged = new ArrayList<>();
        intervals.sort((a, b) -> Integer.compare(a[0], b[0]));
        for (int[] cur : intervals) {
            if (merged.isEmpty()) {
                merged.add(new int[]{cur[0], cur[1]});
                continue;
            }
            int[] last = merged.get(merged.size() - 1);
            if (cur[0] <= last[1]) { // 겹침 또는 맞닿음 → 병합
                last[1] = Math.max(last[1], cur[1]);
            } else {
                merged.add(new int[]{cur[0], cur[1]});
            }
        }
        return merged;
    }

    /** [ls,le] 가 병합된 근무구간 중 하나에 완전히 포함되는지. */
    private boolean isCovered(int ls, int le, List<int[]> windows) {
        for (int[] w : windows) {
            if (w[0] <= ls && le <= w[1]) {
                return true;
            }
        }
        return false;
    }

    /** "HHmm" 4자리 문자열을 0~1439 분으로 변환. 형식 불량이면 null. */
    private Integer toMinutes(String hhmm) {
        if (hhmm == null) {
            return null;
        }
        String t = hhmm.trim();
        if (t.length() != 4 || !t.chars().allMatch(Character::isDigit)) {
            return null;
        }
        int hh = Integer.parseInt(t.substring(0, 2));
        int mm = Integer.parseInt(t.substring(2, 4));
        if (hh > 23 || mm > 59) {
            return null;
        }
        return hh * 60 + mm;
    }

    private TransferBlockReason reason(UserErrorCode code) {
        return new TransferBlockReason(code.code(), code.message());
    }
}
