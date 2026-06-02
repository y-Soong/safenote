package com.prafta.app.req.req06.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.app.req.req06.application.param.MyReqListParam;
import com.prafta.app.req.req06.application.query.MyReqListQuery;
import com.prafta.app.req.req06.dto.response.MyReqItemResponse;
import com.prafta.app.req.req06.dto.response.MyReqListResponse;
import com.prafta.app.req.req06.mapper.AppReq06Mapper;
import com.prafta.app.req.req06.result.MyReqItemResult;
import com.prafta.app.req.req06.service.AppReq06Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-006: 본인 요청 목록 조회 서비스 구현.
 *
 * <p>가공 흐름:
 * <ol>
 *   <li>전체/필터 카운트 + 페이지 행(limit+1) 조회</li>
 *   <li>SYS032/SYS033 라벨 일괄 조회 (in-memory map — 호출당 1회)</li>
 *   <li>각 행을 응답 DTO 로 매핑 (REQ_TYPE/REQ_STATUS 라벨, 요일/날짜 디스플레이, summary.lines)</li>
 *   <li>limit+1 의 마지막 행이 있으면 hasMore=true 로 잘라낸다</li>
 * </ol>
 *
 * <p>LEAVE_TYPE/OT_TYPE 은 SYS 코드가 아닌 자유 텍스트 컬럼이므로 본 서비스 내부의 하드코딩 맵으로 매핑한다.
 * 미매핑 값은 원본 코드 그대로 노출(fallback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppReq06ServiceImpl implements AppReq06Service {

    private final AppReq06Mapper mapper;

    private static final DateTimeFormatter F_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter F_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ─────────────── 자유 텍스트 LEAVE_TYPE / OT_TYPE 하드코딩 매핑 ───────────────
    // (SYS006/SYS010 가정은 plan 의 가정 오류 — 실제는 자유 텍스트 컬럼. 정밀 매핑은 §7 follow-up.)
    private static final Map<String, String> LEAVE_TYPE_LABEL = Map.of(
            "ANNUAL", "연차",
            "HALF_AM", "오전반차",
            "HALF_PM", "오후반차",
            "SICK", "병가",
            "FAMILY", "경조사"
    );
    private static final Map<String, String> OT_TYPE_LABEL = Map.of(
            "EXTEND", "연장",
            "NIGHT", "야간",
            "HOLIDAY", "휴일"
    );

    @Override
    public MyReqListResponse selectMyReqList(MyReqListParam param) {

        int totalCount = mapper.selectMyTotalCount(param.cmpnyCd(), param.siteCd(), param.userCd());

        MyReqListQuery query = MyReqListQuery.from(param);
        int filteredCount = mapper.selectMyFilteredCount(query);

        List<MyReqItemResult> rows = mapper.selectMyReqPage(query);

        // hasMore 판정 — limit+1 로 조회했으므로 마지막 행이 있으면 다음 페이지 존재.
        boolean hasMore = rows.size() > param.limit();
        List<MyReqItemResult> pageRows = hasMore ? rows.subList(0, param.limit()) : rows;

        Map<String, String> reqTypeMap = toLabelMap(mapper.selectSystValDLabels("SYS032"));
        Map<String, String> reqStatusMap = toLabelMap(mapper.selectSystValDLabels("SYS033"));

        List<MyReqItemResponse> items = new ArrayList<>(pageRows.size());
        for (MyReqItemResult r : pageRows) {
            items.add(toResponse(r, reqTypeMap, reqStatusMap));
        }

        return new MyReqListResponse(totalCount, filteredCount, items, hasMore);
    }

    // ─────────────── 행 → 응답 매핑 ───────────────
    private MyReqItemResponse toResponse(MyReqItemResult r,
                                         Map<String, String> reqTypeMap,
                                         Map<String, String> reqStatusMap) {

        String reqTypeDisplay = reqTypeMap.getOrDefault(safe(r.reqType()), safe(r.reqType()));
        String reqStatusDisplay = reqStatusMap.getOrDefault(safe(r.reqStatus()), safe(r.reqStatus()));

        String targetYmdDisplay = formatYmdWithWeekday(r.workYmd());

        List<String> summaryLines = buildSummaryLines(r);

        String reqDatetimeIso = r.insertDate() == null ? null : r.insertDate().format(F_ISO);
        String reqDateDisplay = formatRequestDate(r.insertDate());

        String processedAtIso = r.processDate() == null ? null : r.processDate().format(F_ISO);
        String processedDateDisplay = formatProcessedDate(r.processDate(), r.reqStatus());

        String rejectReason = "03".equals(r.reqStatus()) ? r.processComment() : null;

        return new MyReqItemResponse(
                r.reqId(), r.reqType(), reqTypeDisplay,
                r.reqStatus(), reqStatusDisplay,
                r.workYmd(), targetYmdDisplay,
                summaryLines,
                reqDatetimeIso, reqDateDisplay,
                processedAtIso, processedDateDisplay,
                rejectReason
        );
    }

    // ─────────────── summary.lines 가공 (단순 1차) ───────────────
    private List<String> buildSummaryLines(MyReqItemResult r) {
        String reqType = safe(r.reqType());
        List<String> lines = new ArrayList<>(2);

        if ("01".equals(reqType) || "02".equals(reqType)) {
            // 근태 생성/수정: "{출근 HH:mm} ~ {퇴근 HH:mm}"
            String s = formatHHmm(r.startTime(), "출근 미지정");
            String e = formatHHmm(r.endTime(), "퇴근 미지정");
            lines.add(s + " ~ " + e);
        } else if ("03".equals(reqType) || "04".equals(reqType)) {
            // 초과근무 생성/수정
            String s = formatHHmm(r.startTime(), "시작 미지정");
            String e = formatHHmm(r.endTime(), "종료 미지정");
            int minutes = computeMinutes(r.startTime(), r.endTime());
            StringBuilder sb = new StringBuilder();
            sb.append(s).append(" ~ ").append(e);
            if (minutes > 0) sb.append(" (").append(minutes).append("분)");
            String otLabel = OT_TYPE_LABEL.get(safe(r.otType()));
            if (otLabel != null) {
                sb.append(" · ").append(otLabel);
            } else if (r.otType() != null && !r.otType().isBlank()) {
                sb.append(" · ").append(r.otType());
            }
            lines.add(sb.toString());
        } else if ("05".equals(reqType) || "06".equals(reqType)) {
            // 연차 사용/수정
            String leaveLabel = LEAVE_TYPE_LABEL.get(safe(r.leaveType()));
            if (leaveLabel == null) leaveLabel = (r.leaveType() == null || r.leaveType().isBlank()) ? "연차" : r.leaveType();
            String daysStr = formatLeaveDays(r.leaveDays());
            lines.add(leaveLabel + " · " + daysStr + "일");
            // 다일 연차이면 START_DATE ~ END_DATE 1줄 추가
            if (r.startDate() != null && r.endDate() != null && !r.startDate().equals(r.endDate())) {
                lines.add(formatYmdSlash(r.startDate()) + " ~ " + formatYmdSlash(r.endDate()));
            }
        }
        return lines;
    }

    private String formatHHmm(String hhmm, String fallback) {
        if (hhmm == null || hhmm.isBlank()) return fallback;
        String t = hhmm.trim();
        if (t.length() == 4) return t.substring(0, 2) + ":" + t.substring(2);
        if (t.length() >= 5 && t.charAt(2) == ':') return t.substring(0, 5);
        return t;
    }

    private int computeMinutes(String start, String end) {
        try {
            int s = parseToMinutes(start);
            int e = parseToMinutes(end);
            if (s < 0 || e < 0) return 0;
            int diff = e - s;
            if (diff < 0) diff += 24 * 60; // 자정 넘김 보정
            return diff;
        } catch (Exception ex) {
            return 0;
        }
    }

    private int parseToMinutes(String hhmm) {
        if (hhmm == null) return -1;
        String t = hhmm.trim();
        if (t.length() == 4) {
            return Integer.parseInt(t.substring(0, 2)) * 60 + Integer.parseInt(t.substring(2));
        }
        if (t.length() >= 5 && t.charAt(2) == ':') {
            return Integer.parseInt(t.substring(0, 2)) * 60 + Integer.parseInt(t.substring(3, 5));
        }
        return -1;
    }

    private String formatLeaveDays(BigDecimal days) {
        if (days == null) return "0";
        // 정수면 정수로, 아니면 stripTrailingZeros.
        BigDecimal stripped = days.stripTrailingZeros();
        if (stripped.scale() <= 0) return stripped.toPlainString();
        return stripped.toPlainString();
    }

    // ─────────────── 날짜/시각 디스플레이 ───────────────
    private String formatYmdWithWeekday(String ymd) {
        if (ymd == null || ymd.isBlank()) return null;
        try {
            LocalDate d = LocalDate.parse(ymd, F_YMD);
            String weekday = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
            return d.toString() + " (" + weekday + ")";
        } catch (Exception e) {
            return ymd;
        }
    }

    private String formatYmdSlash(String ymd) {
        if (ymd == null || ymd.isBlank()) return "";
        try {
            return LocalDate.parse(ymd, F_YMD).toString();
        } catch (Exception e) {
            return ymd;
        }
    }

    private String formatRequestDate(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.getYear() + "년 " + dt.getMonthValue() + "월 " + dt.getDayOfMonth() + "일 요청";
    }

    private String formatProcessedDate(LocalDateTime dt, String reqStatus) {
        if (dt == null) return null;
        String action;
        switch (safe(reqStatus)) {
            case "02": action = "승인"; break;
            case "03": action = "반려"; break;
            case "04": action = "취소"; break;
            default: return null; // 01 신청 상태는 처리 일자 없음
        }
        return dt.getYear() + "년 " + dt.getMonthValue() + "월 " + dt.getDayOfMonth() + "일 " + action;
    }

    private Map<String, String> toLabelMap(List<Map<String, String>> rows) {
        Map<String, String> result = new HashMap<>();
        if (rows == null) return result;
        for (Map<String, String> row : rows) {
            String k = row.get("k");
            String v = row.get("v");
            if (k != null) result.put(k, v);
        }
        return result;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
