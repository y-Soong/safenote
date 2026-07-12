package com.prafta.common.cmm.sch.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        if (from.isAfter(to)) {
            return 0;
        }

        int created = 0;
        String prevClosedYm = null;
        boolean prevClosed = false;

        for (LocalDate cur = from; !cur.isAfter(to); cur = cur.plusDays(1)) {
            // 평일(월~금)만 생성. 주말(토/일) 제외(쟁점①).
            DayOfWeek dow = cur.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            String ymd = cur.format(YMD);

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
