package com.prafta.web.leave.promotion.leavepromo01.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.leave.promotion.leavepromo01.result.PromotionTargetRowResult;

/**
 * prafta-com-008-A-6: 2차 촉진 일괄지정 엑셀 양식(.xlsx) 빌더.
 *
 * <p>구조(prafta-052/User_01 양식 관례):
 * <ol>
 *   <li>1행: 빨간 안내문 "4행부터 데이터가 저장됩니다. 한 행 = 한 사용자의 한 연차사용날짜."</li>
 *   <li>2행: 헤더 (USER_CD / 이름 / 부서 / 미사용연차 / 연차사용날짜(YYYYMMDD))</li>
 *   <li>3행: 예시</li>
 *   <li>4행 ~: 대상자 1인 1행(연차사용날짜는 공란 — 관리자가 채움, 여러 날은 같은 USER_CD 로 행 추가)</li>
 * </ol>
 *
 * <p>식별키 = USER_CD 단독(확정-4, 사번 컬럼 없음). 이름/부서/미사용연차는 확인용 표시값(파서는 USER_CD/
 * 연차사용날짜만 사용). USER_CD 컬럼은 사용자 노출 부담이 낮아 숨기지 않고 읽기 안내만 한다.
 */
public final class PromotionExcelTemplateBuilder {

    private PromotionExcelTemplateBuilder() {
    }

    /** 데이터 행 시작 인덱스(0-based): 1행=0(안내), 2행=1(헤더), 3행=2(예시), 4행=3(데이터). */
    public static final int DATA_START_ROW_INDEX = PromotionExcelRowParser.DATA_START_ROW_INDEX;

    /** 헤더 — 파서 컬럼 순서와 1:1. */
    public static final String[] HEADERS = PromotionExcelRowParser.HEADERS;

    private static final String[] EXAMPLE_ROW = new String[] {
            "USER0001"      // USER_CD(필수)
            , "홍길동"        // 이름(표시)
            , "영업팀"        // 부서(표시)
            , "6"            // 미사용연차(표시)
            , "20260812"     // 연차사용날짜(YYYYMMDD)
    };

    /**
     * 2차 촉진 일괄지정 .xlsx 양식 바이트를 생성한다.
     *
     * @param targets 조회조건 기준 대상자(미사용 연차수 포함). 각 1행 프리필(연차사용날짜 공란).
     * @return 생성된 .xlsx 바이트 배열
     */
    public static byte[] build(List<PromotionTargetRowResult> targets) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("연차일괄지정");

            CellStyle noticeStyle = workbook.createCellStyle();
            Font noticeFont = workbook.createFont();
            noticeFont.setBold(true);
            noticeFont.setColor(IndexedColors.RED.getIndex());
            noticeFont.setFontHeightInPoints((short) 11);
            noticeStyle.setFont(noticeFont);
            noticeStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle exampleStyle = workbook.createCellStyle();
            Font exampleFont = workbook.createFont();
            exampleFont.setItalic(true);
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleFont.setFontHeightInPoints((short) 10);
            exampleStyle.setFont(exampleFont);

            int totalCols = HEADERS.length;

            // 1행: 안내문.
            Row noticeRow = sheet.createRow(0);
            Cell noticeCell = noticeRow.createCell(0);
            noticeCell.setCellValue("4행부터 데이터가 저장됩니다. 한 행 = 한 사용자의 한 연차사용날짜(여러 날은 같은 USER_CD 로 행을 추가하세요). USER_CD 와 연차사용날짜만 등록에 사용됩니다.");
            noticeCell.setCellStyle(noticeStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            // 2행: 헤더.
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < totalCols; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3행: 예시.
            Row exampleRow = sheet.createRow(2);
            for (int i = 0; i < totalCols; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(EXAMPLE_ROW[i]);
                cell.setCellStyle(exampleStyle);
            }

            // 4행 ~: 대상자 1인 1행(연차사용날짜 공란).
            int rowIdx = DATA_START_ROW_INDEX;
            if (targets != null) {
                for (PromotionTargetRowResult t : targets) {
                    Row dataRow = sheet.createRow(rowIdx++);
                    dataRow.createCell(0).setCellValue(nz(t.userCd()));
                    dataRow.createCell(1).setCellValue(nz(t.userNm()));
                    dataRow.createCell(2).setCellValue(nz(t.nodeNm()));
                    dataRow.createCell(3).setCellValue(t.unusedDays() == null ? "" : t.unusedDays().stripTrailingZeros().toPlainString());
                    dataRow.createCell(4).setCellValue(""); // 연차사용날짜(관리자 입력)
                }
            }

            for (int i = 0; i < totalCols; i++) {
                sheet.setColumnWidth(i, 5000);
            }
            noticeRow.setHeightInPoints((short) 28);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /**
     * prafta-052 실패행 2시트(.xlsx) 빌더.
     *
     * <ul>
     *   <li>시트1(연차일괄지정): 업로드 양식과 동일 구조(안내/헤더/예시 + 실패 행 원본) — 그대로 재업로드 가능.</li>
     *   <li>시트2(실패사유): USER_CD / 사유.</li>
     * </ul>
     *
     * @param failSourceRows 실패 행 원본(양식 컬럼 순서, HEADERS 와 1:1)
     * @param failReasons    실패 사유(failSourceRows 와 같은 순서·길이)
     * @return 생성된 .xlsx 바이트 배열
     */
    public static byte[] buildFails(List<List<String>> failSourceRows, List<String[]> failReasons) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle noticeStyle = workbook.createCellStyle();
            Font noticeFont = workbook.createFont();
            noticeFont.setBold(true);
            noticeFont.setColor(IndexedColors.RED.getIndex());
            noticeFont.setFontHeightInPoints((short) 11);
            noticeStyle.setFont(noticeFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            int totalCols = HEADERS.length;

            // 시트1: 양식 + 실패 행(재업로드용).
            Sheet sheet1 = workbook.createSheet("연차일괄지정");
            Row noticeRow = sheet1.createRow(0);
            Cell noticeCell = noticeRow.createCell(0);
            noticeCell.setCellValue("4행부터 데이터가 저장됩니다. 사유는 [실패사유] 시트를 확인 후 수정하여 다시 업로드하세요.");
            noticeCell.setCellStyle(noticeStyle);
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            Row headerRow = sheet1.createRow(1);
            for (int i = 0; i < totalCols; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            // 3행(예시)은 비워둔다(파서가 skip). 4행부터 실패 원본.
            sheet1.createRow(2);
            int rowIdx = DATA_START_ROW_INDEX;
            if (failSourceRows != null) {
                for (List<String> src : failSourceRows) {
                    Row dataRow = sheet1.createRow(rowIdx++);
                    for (int c = 0; c < totalCols; c++) {
                        String v = (src != null && c < src.size() && src.get(c) != null) ? src.get(c) : "";
                        dataRow.createCell(c).setCellValue(v);
                    }
                }
            }
            for (int i = 0; i < totalCols; i++) {
                sheet1.setColumnWidth(i, 5000);
            }

            // 시트2: 실패 사유.
            Sheet sheet2 = workbook.createSheet("실패사유");
            String[] reasonHeaders = new String[] { "USER_CD", "사유" };
            Row rh = sheet2.createRow(0);
            for (int c = 0; c < reasonHeaders.length; c++) {
                Cell cell = rh.createCell(c);
                cell.setCellValue(reasonHeaders[c]);
                cell.setCellStyle(headerStyle);
            }
            int r2 = 1;
            if (failReasons != null) {
                for (String[] row : failReasons) {
                    Row dr = sheet2.createRow(r2++);
                    for (int c = 0; c < reasonHeaders.length; c++) {
                        String v = (row != null && c < row.length && row[c] != null) ? row[c] : "";
                        dr.createCell(c).setCellValue(v);
                    }
                }
            }
            sheet2.setColumnWidth(0, 5000);
            sheet2.setColumnWidth(1, 14000);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }
}
