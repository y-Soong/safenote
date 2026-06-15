package com.prafta.common.cmm.leave.promotion.autobatch;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.leave.promotion.mapper.LeavePromotionMapper;
import com.prafta.common.cmm.shift.service.ShiftMembershipService;

import lombok.RequiredArgsConstructor;

/**
 * prafta-com-008-A-5: 사용자별 가용일 집합 A_i 산출(autobatch §2, D-AB1/2).
 *
 * <p>{@code d ∈ A_i} ⟺ 모두 만족:
 * <ol>
 *   <li>windowFrom ≤ d ≤ windowTo</li>
 *   <li>availFrom ≤ d ≤ availTo (만료 후 배정 금지)</li>
 *   <li>주말(토/일) 아님 (D-AB1 제외)</li>
 *   <li>휴일 아님 (tb_holiday 일자 ∪ tb_holiday_rule 매년 MMDD)</li>
 *   <li>이미 그 사람 CONFIRMED 연차일 아님(중복 금지)</li>
 *   <li>교대팀 소속일이면 work_plan 근무일에만 허용(D-AB2, 비교대자는 휴일캘린더+주말만으로 판정)</li>
 * </ol>
 *
 * <p>결정성: 결과는 오름차순 정렬. 회사 단위 휴일 집합/규칙은 호출부가 1회 조회해 주입한다(반복 조회 방지).
 * 교대 판정은 사용자당 1회(대표일), 교대자에 한해 work_plan 근무일 카운트로 일자별 한정한다.
 */
@Component
@RequiredArgsConstructor
public class AssignableDateResolver {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ShiftMembershipService shiftMembershipService;
    private final LeavePromotionMapper leavePromotionMapper;

    /**
     * 사용자 i 의 가용일 집합(오름차순). 회사 휴일/규칙은 사전 조회분을 주입받는다.
     *
     * @param cmpnyCd        회사 코드
     * @param siteCd         대상자 사업장(work_plan/교대 판정 스코프)
     * @param userCd         대상 사용자
     * @param availFrom      본연차 사용가능 시작일 (YYYYMMDD, null=하한 무제한)
     * @param availTo        본연차 사용가능 종료일 (YYYYMMDD, null=상한 무제한)
     * @param windowFrom     배치 윈도 시작 (YYYYMMDD)
     * @param windowTo       배치 윈도 종료 (YYYYMMDD)
     * @param holidayYmds    회사 일자휴일 집합(YYYYMMDD)
     * @param holidayMmdds   회사 매년 반복 휴일 집합(MMDD)
     * @param existingLeave  그 사용자 윈도 내 기존 CONFIRMED 연차일(YYYYMMDD)
     * @return 가용일(YYYYMMDD) 오름차순 리스트
     */
    public List<String> resolve(String cmpnyCd, String siteCd, String userCd,
                                String availFrom, String availTo,
                                String windowFrom, String windowTo,
                                Set<String> holidayYmds, Set<String> holidayMmdds,
                                Set<String> existingLeave) {

        LocalDate from = parse(windowFrom);
        LocalDate to = parse(windowTo);
        // 만료/사용가능 구간으로 윈도를 좁힌다(만료 후 배정 금지).
        if (availFrom != null && !availFrom.isBlank()) {
            LocalDate af = parse(availFrom);
            if (af.isAfter(from)) {
                from = af;
            }
        }
        if (availTo != null && !availTo.isBlank()) {
            LocalDate at = parse(availTo);
            if (at.isBefore(to)) {
                to = at;
            }
        }
        if (from.isAfter(to)) {
            return new ArrayList<>();
        }

        Set<String> existing = (existingLeave == null) ? new HashSet<>() : existingLeave;
        // 교대팀 소속 여부는 사용자당 1회만 판정(대표일=윈도 시작). 교대자만 work_plan 근무일로 한정.
        boolean shiftMember = shiftMembershipService.isInShiftTeamOn(cmpnyCd, siteCd, userCd, from.format(YMD));

        List<String> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            // 주말 제외(D-AB1).
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            String ymd = d.format(YMD);
            // 휴일(일자/매년 반복) 제외.
            if (holidayYmds != null && holidayYmds.contains(ymd)) {
                continue;
            }
            if (holidayMmdds != null && holidayMmdds.contains(ymd.substring(4))) {
                continue;
            }
            // 이미 그 사람 연차일이면 제외(중복 금지).
            if (existing.contains(ymd)) {
                continue;
            }
            // 교대자: 그 일자에 근무 스케줄(work_plan)이 있어야 등록 가능(교대패턴 우선, 즉석 생성 금지).
            if (shiftMember
                    && leavePromotionMapper.countWorkPlanDay(cmpnyCd, siteCd, userCd, ymd) <= 0) {
                continue;
            }
            result.add(ymd);
        }
        return result;
    }

    private static LocalDate parse(String ymd) {
        return LocalDate.parse(ymd, YMD);
    }
}
