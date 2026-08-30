package com.prafta.common.cmm.sch.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.sch.mapper.DefaultSchGenMapper;
import com.prafta.common.cmm.sch.service.DefaultSchGenService;
import com.prafta.common.cmm.sch.vo.DefaultSchUserVO;
import com.prafta.common.cmm.shift.service.ShiftMembershipService;
import com.prafta.web.attd.attd07.service.AttdCloseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기본근무 자동 스케줄 생성 구현 (PRAFTA-COM-008-E-3).
 *
 * <p>평일(월~금)만, 교대 비소속(E-7) 구간만, 미마감월(prafta-028)만, 빈 날만 멱등 생성한다.
 * 결정성을 위해 난수/현재시각 분기를 쓰지 않는다(범위 from~to 는 호출부가 결정).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSchGenServiceImpl implements DefaultSchGenService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private final DefaultSchGenMapper defaultSchGenMapper;
    private final ShiftMembershipService shiftMembershipService;
    private final AttdCloseService attdCloseService;

    @Override
    @Transactional
    public int generateForUser(String cmpnyCd, String siteCd, String userCd,
                               String schCd, String fromYmd, String toYmd) {
        if (!StringUtils.hasText(schCd) || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(fromYmd) || !StringUtils.hasText(toYmd)) {
            return 0;
        }
        LocalDate from = LocalDate.parse(fromYmd, YMD);
        LocalDate to = LocalDate.parse(toYmd, YMD);

        // 입사일 하한 클램프(prafta-061 R2): from = max(from, HIRE_DATE).
        // 입사 예정자를 미리 등록하면 입사 전 기간에 스케줄이 깔려 결근처럼 집계되는 문제 방지.
        // 서비스 내부 단일 지점 — 전 호출 경로(연 배치 1/1~ / 계정생성·기본타입 변경 명일~ /
        // 소속이동 발효일~)에 공통 적용된다.
        // ★fail-open: HIRE_DATE 미설정(NULL — 운영 실존, 예: manager 계정)/비정형/파싱 실패는
        //   클램프 없이 종전 동작 유지(예외로 생성을 막지 않는다. 비정형만 warn 로그 1줄).
        String hireDateRaw = defaultSchGenMapper.selectUserHireDate(cmpnyCd, userCd);
        if (hireDateRaw != null) {
            if (hireDateRaw.matches("^\\d{8}$")) {
                try {
                    LocalDate hireDate = LocalDate.parse(hireDateRaw, YMD);
                    if (hireDate.isAfter(from)) {
                        from = hireDate;
                        fromYmd = hireDateRaw; // 휴일 배치 로딩 범위 정합(클램프된 시작일로 조회)
                    }
                } catch (Exception e) {
                    log.warn("기본근무 자동생성 — 입사일 파싱 실패로 클램프 생략(fail-open). cmpnyCd={}, userCd={}, hireDate={}",
                            cmpnyCd, userCd, hireDateRaw);
                }
            } else {
                log.warn("기본근무 자동생성 — 입사일 비정형으로 클램프 생략(fail-open). cmpnyCd={}, userCd={}, hireDate={}",
                        cmpnyCd, userCd, hireDateRaw);
            }
        }

        // 클램프 후 재검사 — 입사일이 범위 끝(to)보다 뒤면 0건(전 기간이 입사 전).
        if (from.isAfter(to)) {
            return 0;
        }

        int created = 0;
        String prevClosedYm = null;
        boolean prevClosed = false;

        // 휴일 배치 로딩(F3/G4): 범위 내 회사 휴일(특정일 tb_holiday + 매년 반복 tb_holiday_rule)을
        // 루프 진입 전 1회씩만 조회해 Set 으로 사용한다(일별 쿼리 폭증 방지). 회사(CMPNY_CD) 스코프.
        // 생성 시점 기준으로만 스킵 — 이미 생성된 계획은 소급 변경/삭제하지 않는다.
        Set<String> holidayYmds = new HashSet<>(
                defaultSchGenMapper.selectHolidayYmds(cmpnyCd, fromYmd, toYmd));
        Set<String> holidayRuleMmdds = new HashSet<>(
                defaultSchGenMapper.selectHolidayRuleMmdds(cmpnyCd));

        for (LocalDate cur = from; !cur.isAfter(to); cur = cur.plusDays(1)) {
            // 평일(월~금)만 생성. 주말(토/일) 제외(쟁점①).
            DayOfWeek dow = cur.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            String ymd = cur.format(YMD);

            // 휴일 제외(F3/G4): 회사 휴일(특정일 또는 반복규칙 MM/DD)이면 그날 생성 스킵.
            // 주말 제외와 동일 취급. F2 노무수령거부 게이트와 동일한 휴일 소스를 사용한다.
            if (holidayYmds.contains(ymd) || holidayRuleMmdds.contains(ymd.substring(4))) {
                continue;
            }

            // 교대팀 소속 구간 제외(E-7). 교대패턴이 스케줄을 보장하므로 자동생성 비대상.
            if (shiftMembershipService.isInShiftTeamOn(cmpnyCd, siteCd, userCd, ymd)) {
                continue;
            }

            // 마감월 가드(prafta-028). 동일 월 반복 조회를 피해 직전 판정 캐시.
            String ym = ymd.substring(0, 6);
            if (!ym.equals(prevClosedYm)) {
                prevClosed = attdCloseService.isClosedForUser(cmpnyCd, siteCd, userCd, ym);
                prevClosedYm = ym;
            }
            if (prevClosed) {
                continue;
            }

            // 빈 날만 INSERT(멱등, 덮어쓰기 금지). GEN_SOURCE='DEFAULT_SCH'.
            created += defaultSchGenMapper.insertDefaultSchDayIfAbsent(
                    cmpnyCd, siteCd, userCd, ymd, schCd, SYSTEM_OPERATOR);
        }

        if (created > 0) {
            log.info("기본근무 자동생성 — cmpnyCd={}, userCd={}, 범위={}~{}, 생성 {}일",
                    cmpnyCd, userCd, fromYmd, toYmd, created);
        }
        return created;
    }

    @Override
    @Transactional
    public int ensureWorkPlanDay(String cmpnyCd, String userCd, String workYmd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(workYmd)) {
            return 0;
        }
        DefaultSchUserVO u = defaultSchGenMapper.selectDefaultSchUser(cmpnyCd, userCd);
        if (u == null || !StringUtils.hasText(u.defaultSchCd()) || !StringUtils.hasText(u.siteCd())) {
            return 0; // 기본근무 미설정 → 폴백 대상 아님.
        }
        // 교대 비소속·미마감월일 때만 단일일 생성. (폴백은 평일 제한 없음 — 연차/촉진 대상일은 주말일 수도 있음)
        if (shiftMembershipService.isInShiftTeamOn(cmpnyCd, u.siteCd(), userCd, workYmd)) {
            return 0;
        }
        if (attdCloseService.isClosedForUser(cmpnyCd, u.siteCd(), userCd, workYmd.substring(0, 6))) {
            return 0;
        }
        int created = defaultSchGenMapper.insertDefaultSchDayIfAbsent(
                cmpnyCd, u.siteCd(), userCd, workYmd, u.defaultSchCd(), SYSTEM_OPERATOR);
        if (created > 0) {
            log.info("기본근무 폴백 단일일 생성 — cmpnyCd={}, userCd={}, workYmd={}, schCd={}",
                    cmpnyCd, userCd, workYmd, u.defaultSchCd());
        }
        return created;
    }

    @Override
    // 별도 트랜잭션(REQUIRES_NEW): 본 메서드는 호출부(로그인 게이트 setDefaultSch / 사용자 저장 User01)가
    // try-catch 로 예외를 격리한다. 같은 트랜잭션(REQUIRED)이면 내부 실패가 공유 트랜잭션을 rollback-only 로
    // 마킹 → 호출부가 예외를 삼켜도 커밋 시점에 UnexpectedRollbackException 으로 전체가 깨진다.
    // REQUIRES_NEW 로 분리하면 자동생성 실패가 자기 트랜잭션만 롤백하고 호출부 본 흐름은 정상 커밋된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyDefaultSchChange(String cmpnyCd, String siteCd, String userCd, String newSchCd) {
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(siteCd)
                || !StringUtils.hasText(userCd) || !StringUtils.hasText(newSchCd)) {
            return 0;
        }
        // D3: 당일은 건드리지 않는다. 명일(today+1)부터 당해 12/31 까지만 반영.
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        String todayYmd = today.format(YMD);        // updateFutureDefaultSch 의 fromYmd (배타 경계 = 오늘) → WORK_YMD > 오늘 = 명일부터
        String tomorrowYmd = tomorrow.format(YMD);  // generateForUser 의 생성 시작(포함) = 명일

        // 1) 명일 이후 자동생성분만 새 SCH_CD 로 갱신(수동/연차/교대/촉진 보존).
        //    WHERE WORK_YMD > fromYmd(=오늘) 이므로 명일부터 갱신되어 generateForUser 시작과 정합한다.
        //    E3(당일분모 전환, W6): 연차 잠금일(확정 비종일 연차 + 미결 시간차 신청)은 갱신 대상에서
        //    제외(SQL NOT EXISTS)한다 — 시간차 분모(E1)가 당일 배정 스케줄이라 그날 행 덮어쓰기가 차감
        //    분모를 훼손하기 때문. 배치성이므로 하드 실패 없이 조용히 보존하고, 제외 건수만 로그로 남긴다.
        int lockedSkipped = defaultSchGenMapper.countFutureDefaultSchLockedDays(
                cmpnyCd, siteCd, userCd, todayYmd);
        if (lockedSkipped > 0) {
            log.info("기본근무 변경 반영 — 연차 잠금일(확정 비종일/미결 시간차) {}건 보존(E3 skip). cmpnyCd={}, userCd={}",
                    lockedSkipped, cmpnyCd, userCd);
        }
        int updated = defaultSchGenMapper.updateFutureDefaultSch(
                cmpnyCd, siteCd, userCd, todayYmd, newSchCd, SYSTEM_OPERATOR);

        // 2) 명일 빈 평일에 신규 생성(설정/변경 즉시 트리거 — 명일~당해 12/31).
        String toYmd = today.withMonth(12).withDayOfMonth(31).format(YMD);
        int created = generateForUser(cmpnyCd, siteCd, userCd, newSchCd, tomorrowYmd, toYmd);

        log.info("기본근무 변경 반영 — cmpnyCd={}, userCd={}, newSchCd={}, 범위=명일({})~{}, 미래갱신 {}일, 신규 {}일",
                cmpnyCd, userCd, newSchCd, tomorrowYmd, toYmd, updated, created);
        return updated + created;
    }
}
