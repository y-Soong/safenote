package com.prafta.web.user.user01.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-036 — 관리자 사용자 일괄 생성 엑셀 양식(.xlsx) 빌더.
 *
 * <p>구조 (D4):
 * <ol>
 *   <li>1행: 빨간 글씨 안내문 "4행부터 데이터가 저장됩니다." (헤더 컬럼 전체 머지)</li>
 *   <li>2행: 한글 헤더 (필수 5 + 선택 11 = 16 컬럼)</li>
 *   <li>3행: 예시 데이터 (업로드 파서에서 skip)</li>
 *   <li>4행 ~: 실제 저장 대상 데이터</li>
 * </ol>
 *
 * <p>컬럼 순서/이름은 {@link UserExcelRowParser#HEADERS} 와 1:1 대응해야 한다.
 */
public final class UserExcelTemplateBuilder {

    private UserExcelTemplateBuilder() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * PRAFTA-037-F4: Data Validation 드롭다운 적용 데이터 행 범위 (0-based inclusive).
     * {@link UserExcelRowParser#DATA_START_ROW_INDEX} 부터 시작, 업로드 상한(1000행)과 일치.
     */
    private static final int VALIDATION_DATA_LAST_ROW = UserExcelRowParser.DATA_START_ROW_INDEX + 999;

    /** 성별 허용 코드 (드롭다운). */
    private static final String[] GENDER_OPTIONS = new String[] { "M", "F" };

    /** 고용형태 [SYS041] 허용 코드 (드롭다운). User01ServiceImpl.ALLOWED_EMPLOYMENT_TYPES 와 동일. */
    private static final String[] EMPLOYMENT_TYPE_OPTIONS = new String[] {
            "REGULAR", "CONTRACT", "DAILY", "EXECUTIVE"
    };

    /** 경력 인정 사유 유형 [SYS042] 허용 코드 (드롭다운). User01ServiceImpl.ALLOWED_REASON_TYPES 와 동일. */
    private static final String[] REASON_TYPE_OPTIONS = new String[] {
            "CONTRACT_TO_REGULAR", "EXPERIENCE_DIFF", "EXPERIENCE_SAME",
            "GROUP_MOVE", "MA_TRANSFER", "OTHER"
    };

    // 컬럼 인덱스 ({@link UserExcelRowParser#HEADERS} 순서와 동일).
    private static final int COL_IDX_GENDER = 7;
    private static final int COL_IDX_EMPLOYMENT_TYPE = 11;
    private static final int COL_IDX_REASON_TYPE = 14;

    /** 예시 데이터 1행 — 파서에서 skip 되지만 사용자가 양식을 이해할 수 있도록 한 줄 채워둔다. */
    private static final String[] EXAMPLE_ROW = new String[] {
            "kim001"            // 사용자ID
            , "김프라프타"       // 사용자명
            , "user"             // 권한코드
            , "99999"            // 사업장번호
            , "NODE_001"         // 소속부서코드
            , "010-1234-5678"    // 휴대폰번호
            , "kim@example.com"  // 이메일
            , "M"                // 성별 (M/F)
            , "900101"           // 생년월일
            , ""                 // 직급코드
            , "20200101"         // 입사일
            , "REGULAR"          // 고용형태
            , ""                 // 계약종료일
            , "0"                // 경력인정개월수
            , ""                 // 경력인정사유유형
            , ""                 // 경력인정상세
    };

    /**
     * 사용자 일괄 생성 .xlsx 양식 바이트를 생성한다.
     *
     * @return 생성된 .xlsx 바이트 배열
     */
    public static byte[] build() {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("사용자생성");

            // 스타일 ----------------------------------------------------------
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

            // 1행: 안내문 -----------------------------------------------------
            int totalCols = UserExcelRowParser.HEADERS.length;
            Row noticeRow = sheet.createRow(0);
            Cell noticeCell = noticeRow.createCell(0);
            noticeCell.setCellValue("4행부터 데이터가 저장됩니다. 1행(안내)과 3행(예시) 사이의 2행은 헤더이며, 헤더 행은 수정/삭제하지 마세요.");
            noticeCell.setCellStyle(noticeStyle);
            // 컬럼 전체 머지(가독성)
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            // 2행: 헤더 -------------------------------------------------------
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < totalCols; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(UserExcelRowParser.HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3행: 예시 -------------------------------------------------------
            Row exampleRow = sheet.createRow(2);
            for (int i = 0; i < totalCols; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(EXAMPLE_ROW[i]);
                cell.setCellStyle(exampleStyle);
            }

            // 컬럼 너비 ------------------------------------------------------
            for (int i = 0; i < totalCols; i++) {
                sheet.setColumnWidth(i, 4500);
            }
            // 행 높이 (1행 안내문 가독성)
            noticeRow.setHeightInPoints((short) 24);

            // PRAFTA-037-F4: Data Validation 드롭다운 ----------------------
            // 데이터 입력 단계에서부터 허용 코드만 강제 — 업로드 시 행 검증 부담 경감 + UX 개선.
            applyDropdownValidation(sheet, COL_IDX_GENDER,
                    GENDER_OPTIONS,
                    "성별 선택",
                    "M(남) 또는 F(여) 중에서 선택해 주세요.");
            applyDropdownValidation(sheet, COL_IDX_EMPLOYMENT_TYPE,
                    EMPLOYMENT_TYPE_OPTIONS,
                    "고용형태 선택",
                    "REGULAR(정규직)/CONTRACT(계약직)/DAILY(일용직)/EXECUTIVE(임원) 중에서 선택해 주세요.");
            applyDropdownValidation(sheet, COL_IDX_REASON_TYPE,
                    REASON_TYPE_OPTIONS,
                    "사유 유형 선택",
                    "정의된 SYS042 사유 코드 중에서 선택해 주세요.");

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ApiException(CommonErrorCode.COMMON_500_001);
        }
    }

    /**
     * PRAFTA-037-F4 — 특정 컬럼의 데이터 행 범위(4행 ~ 1003행)에 명시적 드롭다운 검증을 적용한다.
     * 사용자가 허용 코드 외의 값을 입력하면 한국어 오류 메시지를 표시하고 차단한다.
     */
    private static void applyDropdownValidation(Sheet sheet, int colIdx, String[] options,
                                                String errorTitle, String errorBody) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        CellRangeAddressList range = new CellRangeAddressList(
                UserExcelRowParser.DATA_START_ROW_INDEX,
                VALIDATION_DATA_LAST_ROW,
                colIdx,
                colIdx
        );
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setSuppressDropDownArrow(false);  // 드롭다운 화살표 표시 (사용자가 코드 선택 편의)
        validation.setShowErrorBox(true);
        validation.createErrorBox(errorTitle, errorBody);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        sheet.addValidationData(validation);
    }
}
