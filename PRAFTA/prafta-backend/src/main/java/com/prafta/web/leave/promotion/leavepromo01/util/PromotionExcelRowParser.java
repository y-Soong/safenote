package com.prafta.web.leave.promotion.leavepromo01.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * prafta-com-008-A-6: 2차 촉진 일괄지정 엑셀(.xlsx) 행 파서.
 *
 * <p>{@link PromotionExcelTemplateBuilder} 양식과 1:1. 1행(안내)/2행(헤더)/3행(예시) skip, 4행(인덱스3)
 * 부터 데이터. 빈 행이 나오면 시트 종료로 간주. 등록에 사용하는 값은 USER_CD(0번)·연차사용날짜(4번)뿐
 * (이름/부서/미사용연차는 표시값으로 무시).
 */
public final class PromotionExcelRowParser {

    private PromotionExcelRowParser() {
    }

    /** 데이터 행 시작 인덱스(0-based). */
    public static final int DATA_START_ROW_INDEX = 3;

    /** 헤더(컬럼 순서 = 파서 인덱스). */
    public static final String[] HEADERS = new String[] {
            "USER_CD(필수)"
            , "이름(표시)"
            , "부서(표시)"
            , "미사용연차(표시)"
            , "연차사용날짜(YYYYMMDD)"
    };

    /** 등록 사용 컬럼: USER_CD. */
    private static final int COL_USER_CD = 0;
    /** 등록 사용 컬럼: 연차사용날짜. */
    private static final int COL_WORK_YMD = 4;

    /** 파싱된 행 1건(USER_CD + 연차사용날짜 + 원본 표시행). */
    public record ParsedRow(
            int index,
            String userCd,
            String workYmd,
            List<String> sourceRow
    ) {
    }

    /**
     * 시트의 데이터 영역을 행 목록으로 파싱한다. 빈 행에서 종료.
     *
     * @param sheet 워크북 시트 0
     * @return 파싱된 행 목록(헤더/예시/빈 행 제외)
     */
    public static List<ParsedRow> parse(Sheet sheet) {
        List<ParsedRow> result = new ArrayList<>();
        int lastRowIdx = sheet.getLastRowNum();
        int outIndex = 0;
        for (int rowIdx = DATA_START_ROW_INDEX; rowIdx <= lastRowIdx; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null || isEmptyRow(row)) {
                break;
            }
            String userCd = strAt(row, COL_USER_CD);
            String workYmd = strAt(row, COL_WORK_YMD);
            result.add(new ParsedRow(outIndex++, userCd, workYmd, toSourceRow(row)));
        }
        return result;
    }

    /** 실패 행 재업로드용 원본 행(양식 컬럼 순서). HEADERS 와 1:1. */
    public static List<String> toSourceRow(Row row) {
        return Arrays.asList(
                nz(strAt(row, 0))   // USER_CD
              , nz(strAt(row, 1))   // 이름
              , nz(strAt(row, 2))   // 부서
              , nz(strAt(row, 3))   // 미사용연차
              , nz(strAt(row, 4))   // 연차사용날짜
        );
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static boolean isEmptyRow(Row row) {
        short last = row.getLastCellNum();
        if (last <= 0) {
            return true;
        }
        for (int c = 0; c < last; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                continue;
            }
            String v = readCellAsString(cell);
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String strAt(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        String v = readCellAsString(cell);
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static String readCellAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date d = cell.getDateCellValue();
                    if (d == null) {
                        return null;
                    }
                    return new java.text.SimpleDateFormat("yyyyMMdd").format(d);
                }
                double n = cell.getNumericCellValue();
                if (n == Math.floor(n) && !Double.isInfinite(n)) {
                    return Long.toString((long) n);
                }
                return Double.toString(n);
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    try {
                        double v = cell.getNumericCellValue();
                        if (v == Math.floor(v)) {
                            return Long.toString((long) v);
                        }
                        return Double.toString(v);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return null;
        }
    }
}
