import ExcelJS from "exceljs";

/**
 * 스타일이 적용된 엑셀 파일을 생성하고 다운로드합니다.
 *
 * @param {Object} options
 * @param {string} options.fileName - 저장할 파일명 (예: "스케줄관리_2026-04.xlsx")
 * @param {Array<SheetConfig>} options.sheets - 시트 설정 배열
 *
 * @typedef {Object} SheetConfig
 * @property {string} name - 시트명
 * @property {Array<ColumnConfig>} columns - 컬럼 설정 배열
 * @property {Array<Array>} data - 데이터 행 배열 (각 행은 columns 순서에 맞는 값 배열)
 *
 * @typedef {Object} ColumnConfig
 * @property {string} header - 헤더 표시명
 * @property {boolean} fixed - true: 고정값(회색), false: 입력값(흰색)
 * @property {number} [width] - 컬럼 너비 (기본값: header 길이 기반 자동)
 */
export async function exportStyledExcel({ fileName, sheets }) {
  const COLORS = {
    headerBg: "FFDCFCE7", // 연한 초록 (green-100)
    headerFont: "FF166534", // 진한 초록 (green-900)
    headerBorder: "FF86EFAC", // 초록 border (green-300)
    fixedBg: "FFF3F4F6", // 연한 회색
    fixedFont: "FF6B7280", // 회색 텍스트
    inputBg: "FFFFFFFF", // 흰색
    inputFont: "FF111827", // 기본 텍스트
    border: "FFD1D5DB", // 일반 border
  };

  const wb = new ExcelJS.Workbook();

  for (const sheet of sheets) {
    const ws = wb.addWorksheet(sheet.name);

    // 컬럼 너비 설정
    ws.columns = sheet.columns.map((col) => ({
      width: col.width ?? Math.max(col.header.length * 2.5, 12),
    }));

    // 헤더 행 추가
    const headerRow = ws.addRow(sheet.columns.map((c) => c.header));
    headerRow.height = 20;
    headerRow.eachCell((cell, colIdx) => {
      cell.fill = {
        type: "pattern",
        pattern: "solid",
        fgColor: { argb: COLORS.headerBg },
      };
      cell.font = {
        bold: true,
        color: { argb: COLORS.headerFont },
        size: 10,
      };
      cell.alignment = { vertical: "middle", horizontal: "center" };
      cell.border = makeBorder(COLORS.headerBorder);

      // 고정 컬럼이면 border 색을 구분
      if (sheet.columns[colIdx - 1]?.fixed) {
        cell.fill = {
          type: "pattern",
          pattern: "solid",
          fgColor: { argb: "FFD1FAE5" }, // 고정 헤더는 살짝 더 진한 초록
        };
      }
    });

    // 데이터 행 추가
    for (const rowData of sheet.data) {
      const row = ws.addRow(rowData);
      row.height = 18;
      row.eachCell({ includeEmpty: true }, (cell, colIdx) => {
        const colCfg = sheet.columns[colIdx - 1];
        const isFixed = colCfg?.fixed ?? false;

        cell.fill = {
          type: "pattern",
          pattern: "solid",
          fgColor: { argb: isFixed ? COLORS.fixedBg : COLORS.inputBg },
        };
        cell.font = {
          color: { argb: isFixed ? COLORS.fixedFont : COLORS.inputFont },
          size: 10,
        };
        cell.alignment = {
          vertical: "middle",
          horizontal: isFixed ? "left" : "center",
          wrapText: true,
        };
        cell.border = makeBorder(COLORS.border);
      });
    }

    // 헤더 행 고정 (틀 고정)
    ws.views = [{ state: "frozen", ySplit: 1 }];
  }

  // 다운로드
  const buffer = await wb.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}

function makeBorder(color) {
  const side = { style: "thin", color: { argb: color } };
  return { top: side, left: side, bottom: side, right: side };
}
