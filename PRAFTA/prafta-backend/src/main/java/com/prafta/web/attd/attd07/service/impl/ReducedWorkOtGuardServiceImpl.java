package com.prafta.web.attd.attd07.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.stdwork.StdWorkReasonCd;
import com.prafta.common.cmm.stdwork.service.StdWorkHoursService;
import com.prafta.common.cmm.stdwork.vo.StdWorkHoursVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.mapper.Attd07Mapper;
import com.prafta.web.attd.attd07.service.ReducedWorkOtGuardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-07: 단축근무자 초과근무 게이트 구현.
 *
 * <p>판정 순서 (fail-closed)
 * <ol>
 *   <li>근무일 기준 소정근로 유효 이력 행 조회 — 행이 없거나 사유가 단축이 아니면 <b>즉시 통과</b>
 *       (대다수 근로자의 동작 완전 불변. 여기서 끝나면 추가 쿼리 0회).</li>
 *   <li>PREGNANCY(임신기) → 무조건 거부(ATTD_400_200).</li>
 *   <li>CHILDCARE / FAMILY_CARE → 근로자 명시 청구 확인 없으면 거부(ATTD_400_201).
 *       단 <b>근로자 신청(REQ) 경유</b>({@code claimVerifiedAtRequest=true})면 신청 시점에 이미
 *       검증된 사실이므로 이 단계를 건너뛴다.</li>
 *   <li>해당 주(월~일) 초과근무 합계 + 이번 요청분 &gt; 720분이면 거부(ATTD_400_202).
 *       <b>REQ 경유 승인에서도 이 검사는 생략하지 않는다</b> — 신청과 승인 사이에 다른 초과근무가
 *       쌓여 한도를 넘을 수 있기 때문이다.</li>
 * </ol>
 *
 * <p><b>주 경계 산정</b> — 근무일이 속한 주의 <b>월요일 ~ 일요일</b>. 주52시간 관리(Attd_15)의
 * 주 정의({@code Weekly52hListsParam}: weekStartYmd 는 반드시 월요일, weekEnd = +6일, 사업장 무관
 * 전사 동일 기준)와 동일하게 맞춘 것이며 자체 정의가 아니다. 주 귀속은 Attd_15 집계와 같이
 * {@code WORK_YMD}(근무일) 기준이므로 오버나이트 초과근무도 근무일이 속한 주로 계산된다.
 *
 * <p><b>집계 범위(M-1)</b> — 주 합계는 <b>회사 + 근로자 + 주 범위</b>로만 집계하고 사업장으로 좁히지
 * 않는다. 연장근로 한도는 근로자 기준이므로, 사업장별로 집계하면 추가 사업장 권한
 * ({@code TB_USER_SITE_AUTH})을 가진 근로자가 같은 주에 사업장마다 720분씩 등록해 법정 한도를
 * 넘길 수 있고, 주중 사업장 이동 시 앞 사업장 실적이 합계에서 누락된다. {@code siteCd} 인자는
 * 로그 컨텍스트 용도로만 쓴다.
 *
 * <p><b>로깅 최소화(M-3)</b> — 사유코드(PREGNANCY/CHILDCARE/FAMILY_CARE)와 단축 적용기간은 로그에
 * 남기지 않는다. {@code userCd} 와 결합하면 임신 사실(건강정보)·가족관계 정보가 되기 때문이다
 * (정책 §11.1 목적제한·최소수집). 거부 사유 추적은 로그에 함께 찍는 에러코드로 갈음한다.
 *
 * <p><b>사유코드 하드코딩 경계</b> — 사유 집합 판정은 {@link StdWorkReasonCd}(단축 여부)와
 * 임신기 상수 1건만 사용한다. 2단계의 차감·부여 규칙 분기는 여기서 다루지 않는다(SYS083 데이터 분기).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReducedWorkOtGuardServiceImpl implements ReducedWorkOtGuardService {

    /** 소정-02 공용 서비스(읽기 전용 사용) — 근무일 유효 소정근로 이력 행의 단일 출처. */
    private final StdWorkHoursService stdWorkHoursService;
    private final Attd07Mapper attd07Mapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void assertOvertimeAllowed(String cmpnyCd,
                                      String siteCd,
                                      String userCd,
                                      String workYmd,
                                      int requestMinutes,
                                      boolean workerClaimConfirmed,
                                      boolean claimVerifiedAtRequest,
                                      List<String> excludeOtIds,
                                      String excludeReqId) {

        // ★L-1(security 지적): 식별값 blank 는 fail-closed 로 거부한다.
        //   보안 게이트가 입력 결손에 통과(fail-open)로 반응하면 하류 형식오류 분기(fail-closed)와
        //   방향이 어긋나고, 게이트 자체를 무력화하는 우회 표면이 된다. 정상 호출은 상위 검증
        //   (Param.from 필수값 / @Pattern 8자리)을 이미 통과하므로 여기 도달 자체가 비정상이다.
        if (isBlank(cmpnyCd) || isBlank(userCd) || isBlank(workYmd)) {
            log.warn("[소정-07] 초과근무 거부 - 게이트 식별값 결손(fail-closed). cmpnyCdBlank={}, userCdBlank={}, workYmdBlank={}",
                    isBlank(cmpnyCd), isBlank(userCd), isBlank(workYmd));
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // ★siteCd 는 한도 집계에 쓰지 않는다(M-1 — 주 한도는 근로자 기준, 사업장 무관).
        //   로그 컨텍스트 용도로만 남기므로 blank 여도 판정에 영향이 없다.

        // 1) 근무일 기준 유효 소정근로 이력 행. 미입력 계정(대다수)은 null → 게이트 미적용.
        StdWorkHoursVO effective = stdWorkHoursService.findEffectiveRow(cmpnyCd, userCd, workYmd);
        if (effective == null || !StdWorkReasonCd.isReduced(effective.getReasonCd())) {
            return; // 단축 기간이 아님 → 종전 동작과 완전히 동일(추가 쿼리 없음).
        }

        // ★M-3(security 지적): 사유코드·적용기간은 로그에 남기지 않는다.
        //   (userCd + PREGNANCY) 조합은 사실상 임신 사실(건강정보), CHILDCARE/FAMILY_CARE 는 가족관계
        //   정보다. 운영은 상시 파일 로깅(prafta.log) 상태라 로그 열람 권한자 전원에게 노출된다
        //   (정책 §11.1 목적제한·최소수집). 로그에는 "단축 대상" 사실만 남기고, 사유 구분은
        //   에러코드(200/201/202)로 갈음한다 — 코드가 사유와 1:1 이라 추적성은 유지된다.
        String reasonCd = effective.getReasonCd();

        // 2) 임신기 단축: 연장근로 전면 금지(예외 없음).
        if (StdWorkReasonCd.PREGNANCY.equals(reasonCd)) {
            log.info("[소정-07] 초과근무 거부(ATTD_400_200) - 단축 기간 연장근로 금지 사유. cmpnyCd={}, siteCd={}, userCd={}, workYmd={}",
                    cmpnyCd, siteCd, userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_200);
        }

        // 3) 육아기·가족돌봄 단축: 근로자 명시 청구 확인이 없으면 거부.
        //    사업주가 연장근로를 요구할 수 없는 영역이라, 확인 값 미전송(구버전 클라이언트 포함)은
        //    허용이 아니라 거부로 처리한다(fail-safe).
        //    ★ 단, 근로자 신청(REQ) 경유 승인은 신청 시점에 이 검사를 이미 통과한 건이므로 건너뛴다
        //      (명시 청구 = 신청 시점의 사실이지 승인자가 재확인할 값이 아님 — 2026-08-12 확정).
        //      아래 주 720분 한도는 REQ 경유에서도 그대로 검사한다.
        if (claimVerifiedAtRequest) {
            log.debug("[소정-07] 단축 기간 청구 확인 검사 생략(신청 시점 검증분). cmpnyCd={}, siteCd={}, userCd={}, workYmd={}",
                    cmpnyCd, siteCd, userCd, workYmd);
        } else if (!workerClaimConfirmed) {
            log.info("[소정-07] 초과근무 거부(ATTD_400_201) - 단축 기간 근로자 명시 청구 미확인(관리자 직접 등록). cmpnyCd={}, siteCd={}, userCd={}, workYmd={}",
                    cmpnyCd, siteCd, userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_201);
        }

        // 4) 주 12시간(720분) 한도. 주 경계 = 근무일이 속한 주의 월~일(Attd_15 주52 기준과 동일).
        LocalDate work;
        try {
            work = LocalDate.parse(workYmd, YMD);
        } catch (DateTimeParseException e) {
            // 단축 대상임이 확인된 상태에서 근무일이 해석 불가하면 한도 판정을 할 수 없다 → fail-closed.
            log.warn("[소정-07] 초과근무 거부 - 근무일 형식 오류로 주 한도 판정 불가. cmpnyCd={}, userCd={}, workYmd={}",
                    cmpnyCd, userCd, workYmd);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        LocalDate weekStart = work.minusDays(work.getDayOfWeek().getValue() - (long) DayOfWeek.MONDAY.getValue());
        LocalDate weekEnd = weekStart.plusDays(6);
        String weekStartYmd = weekStart.format(YMD);
        String weekEndYmd = weekEnd.format(YMD);

        // ★M-1: 사업장 무관 집계(회사+근로자+주 범위). 다중 사업장 근로자의 한도 우회를 막는다.
        Integer weekly = attd07Mapper.selectWeeklyOvertimeMinutes(
                cmpnyCd, userCd, weekStartYmd, weekEndYmd, excludeOtIds, excludeReqId);
        int alreadyMinutes = (weekly == null) ? 0 : weekly;
        int addMinutes = Math.max(requestMinutes, 0);
        int totalMinutes = alreadyMinutes + addMinutes;

        if (totalMinutes > WEEKLY_OT_LIMIT_MINUTES) {
            log.info("[소정-07] 초과근무 거부(ATTD_400_202) - 단축 기간 주 한도 초과. cmpnyCd={}, siteCd={}, userCd={}, workYmd={}, 주={}~{}, 기존={}분, 요청={}분, 한도={}분",
                    cmpnyCd, siteCd, userCd, workYmd, weekStartYmd, weekEndYmd,
                    alreadyMinutes, addMinutes, WEEKLY_OT_LIMIT_MINUTES);
            throw new ApiException(AttdErrorCode.ATTD_400_202);
        }

        log.info("[소정-07] 단축 기간 초과근무 게이트 통과. cmpnyCd={}, siteCd={}, userCd={}, workYmd={}, 주={}~{}, 합계={}분/{}분",
                cmpnyCd, siteCd, userCd, workYmd, weekStartYmd, weekEndYmd,
                totalMinutes, WEEKLY_OT_LIMIT_MINUTES);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
