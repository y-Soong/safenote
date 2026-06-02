package com.prafta.app.attd.attd01.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.attd.attd01.application.param.CheckInParam;
import com.prafta.app.attd.attd01.application.param.CheckOutParam;
import com.prafta.app.attd.attd01.application.param.DayDetailParam;
import com.prafta.app.attd.attd01.application.param.MonthParam;
import com.prafta.app.attd.attd01.application.param.TodayParam;
import com.prafta.app.attd.attd01.application.param.WeekParam;
import com.prafta.app.attd.attd01.dto.request.CheckInRequest;
import com.prafta.app.attd.attd01.dto.request.CheckOutRequest;
import com.prafta.app.attd.attd01.dto.request.DayDetailRequest;
import com.prafta.app.attd.attd01.dto.request.MonthRequest;
import com.prafta.app.attd.attd01.dto.request.WeekRequest;
import com.prafta.app.attd.attd01.dto.response.MyAttendanceDayResponse;
import com.prafta.app.attd.attd01.dto.response.MyMonthResponse;
import com.prafta.app.attd.attd01.dto.response.MyWeekResponse;
import com.prafta.app.attd.attd01.service.AppAttd01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-002: 모바일 앱 "내 근태 조회" 컨트롤러 (attd01).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 *   <ul>
 *     <li>GET /prafta/appApi/attd/my/today          (파라미터 없음)</li>
 *     <li>GET /prafta/appApi/attd/my/week?weekStartYmd=YYYYMMDD</li>
 *     <li>GET /prafta/appApi/attd/my/month?yearMonth=YYYYMM</li>
 *     <li>GET /prafta/appApi/attd/my/day-detail?workYmd=YYYYMMDD</li>
 *   </ul>
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. CMPNY_CD/USER_CD/SITE_CD 는 JWT 에서만 얻으며,
 *   week/month/day-detail 은 날짜 파라미터만 받고 USER_CD 는 절대 받지 않는다(본인 데이터만 조회).
 */
@Slf4j
@RestController
@RequestMapping("/attd")
@RequiredArgsConstructor
public class AppAttd01Controller {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AppAttd01Service appAttd01Service;
    private final JwtUtil jwtUtil;

    /**
     * 오늘 근태 조회 (오늘 탭). 파라미터 없음 — workYmd 는 서버 today.
     */
    @GetMapping("/my/today")
    public ResponseEntity<?> getToday(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        String today = LocalDate.now().format(YMD);

        log.info("[attd01] 오늘 근태 조회 진입 (userCd={}, workYmd={})", tokenInfo.gv_userCd(), today);

        MyAttendanceDayResponse response = appAttd01Service.selectToday(
                TodayParam.from(tokenInfo, today)
        );

        log.info("[attd01] 오늘 근태 조회 완료 (userCd={})", tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 이번주 7일 요약 조회.
     */
    @GetMapping("/my/week")
    public ResponseEntity<?> getWeek(
            @ModelAttribute WeekRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        log.info("[attd01] 이번주 근태 조회 진입 (userCd={}, weekStartYmd={})",
                tokenInfo.gv_userCd(), request.getWeekStartYmd());

        MyWeekResponse response = appAttd01Service.selectWeek(
                WeekParam.from(request, tokenInfo)
        );

        log.info("[attd01] 이번주 근태 조회 완료 (userCd={})", tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 이번달 일별 dayType + 합계 조회.
     */
    @GetMapping("/my/month")
    public ResponseEntity<?> getMonth(
            @ModelAttribute MonthRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        log.info("[attd01] 이번달 근태 조회 진입 (userCd={}, yearMonth={})",
                tokenInfo.gv_userCd(), request.getYearMonth());

        MyMonthResponse response = appAttd01Service.selectMonth(
                MonthParam.from(request, tokenInfo)
        );

        log.info("[attd01] 이번달 근태 조회 완료 (userCd={})", tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 임의 일자 상세 조회 (이번달 셀 선택 등). 응답 구조는 오늘 탭과 동일.
     */
    @GetMapping("/my/day-detail")
    public ResponseEntity<?> getDayDetail(
            @ModelAttribute DayDetailRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        log.info("[attd01] 일자 상세 조회 진입 (userCd={}, workYmd={})",
                tokenInfo.gv_userCd(), request.getWorkYmd());

        MyAttendanceDayResponse response = appAttd01Service.selectDayDetail(
                DayDetailParam.from(request, tokenInfo)
        );

        log.info("[attd01] 일자 상세 조회 완료 (userCd={})", tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 셀프 퇴근(check-out). 최종 URL: POST /prafta/appApi/attd/check-out.
     *
     * <p>요청 본문(JSON): {@code { lat, lon, accuracy?, isMocked, workYmd? }} — USER_CD/CMPNY_CD/SITE_CD 는
     *   본문으로 받지 않고 JWT 에서만 얻는다(IDOR). workSeq 도 받지 않고 서버가 열린 구간을 판정한다.
     * <p>GPS(prafta-app-003): Mock('Y')은 CheckOutParam.from 에서 거부. 좌표 결측은 폴백(온사이트).
     *   서버가 사업장 지오펜스로 외근(밖) 여부를 판정해 응답 isOffsite 로 반환한다. IP 는 서버가 추출.
     * <p>응답: 갱신된 해당 근무일 카드(오늘 탭과 동일 구조, MyAttendanceDayResponse) + isOffsite.
     */
    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(
            @RequestBody CheckOutRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
            , HttpServletRequest httpRequest
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        String ipAddr = ClientIpExtractor.extract(httpRequest);

        log.info("[attd01] 셀프 퇴근 요청 (userCd={})", tokenInfo.gv_userCd());

        MyAttendanceDayResponse response = appAttd01Service.checkOut(
                CheckOutParam.from(request, ipAddr, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 셀프 출근(check-in). 최종 URL: POST /prafta/appApi/attd/check-in.
     *
     * <p>요청 본문(JSON): {@code { lat, lon, accuracy?, isMocked, workYmd? }} — USER_CD/CMPNY_CD/SITE_CD 는
     *   본문으로 받지 않고 JWT 에서만 얻는다(IDOR). WORK_SEQ 도 받지 않고 서버가 그 일자 기존 근태 개수로 산정한다.
     * <p>출근 규칙(prafta-app-003 A1, attd §5/§7): 구간기반 출근횟수·재출근·초과출근 차단·연차일 차단·
     *   다음날 게이트(전날 미완료 차단)·월마감·지오펜스. Mock('Y')은 CheckInParam.from 에서 거부.
     * <p>응답: 생성 반영된 해당 근무일 카드(오늘 탭과 동일 구조, MyAttendanceDayResponse) + isOffsite.
     */
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(
            @RequestBody CheckInRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization
            , HttpServletRequest httpRequest
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        String ipAddr = ClientIpExtractor.extract(httpRequest);

        log.info("[attd01] 셀프 출근 요청 (userCd={})", tokenInfo.gv_userCd());

        MyAttendanceDayResponse response = appAttd01Service.checkIn(
                CheckInParam.from(request, ipAddr, tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
