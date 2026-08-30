import ExcelJS from "exceljs";

/**
 * TBM 증빙자료(반기) 엑셀 빌더 — 참고 양식(TBM_안전교육_증빙패키지_v2.xlsx) 재현.
 *
 * 시트 구성:
 *   ① 반기 교육실시 목록 — 세션별 1행(자사 개설 + 연동 공유 세션 자사 참석분)
 *   ② 근로자별 이수현황 — 인정시간 축 합산 + 법정 기준 2축(사무·판매 6h / 그 외 12h) 수식 판정.
 *      기타 교육(분)은 수기 입력 칸 — 입력 시 누적/충족 수식이 자동 재계산된다.
 *   ③.. 교육일지(건별 출력) — 체크한 세션당 1시트. 참석자 서명은 "서명함" 표기 유지(확정안).
 *       "5. 확인"의 주관자 서명만 실제 서명 이미지를 삽입한다(tbm04-manager-sign — 없으면 빈칸=수기용).
 *
 * 생성은 전부 브라우저(클라이언트)에서 수행한다 — 서버는 JSON 데이터만 공급(부하 회피 확정안).
 */

const C = {
  titleFont: { bold: true, size: 14, color: { argb: "FF111827" } },
  metaFont: { size: 10, color: { argb: "FF374151" } },
  noteFont: { size: 9, color: { argb: "FF6B7280" } },
  headerFill: { type: "pattern", pattern: "solid", fgColor: { argb: "FFDCFCE7" } },
  headerFont: { bold: true, size: 10, color: { argb: "FF166534" } },
  sectionFill: { type: "pattern", pattern: "solid", fgColor: { argb: "FFF3F4F6" } },
  sectionFont: { bold: true, size: 11, color: { argb: "FF111827" } },
  inputFill: { type: "pattern", pattern: "solid", fgColor: { argb: "FFFFFBEB" } }, // 수기 입력 칸(연노랑)
  cellFont: { size: 10, color: { argb: "FF111827" } },
  border: {
    top: { style: "thin", color: { argb: "FFD1D5DB" } },
    left: { style: "thin", color: { argb: "FFD1D5DB" } },
    bottom: { style: "thin", color: { argb: "FFD1D5DB" } },
    right: { style: "thin", color: { argb: "FFD1D5DB" } },
  },
};

// 시트명에 쓸 수 없는 문자 제거 + 31자 제한(엑셀 스펙).
const safeSheetName = (name) =>
  String(name)
    .replace(/[\\/?*[\]:]/g, "-")
    .slice(0, 31);

// SESSION_CD(T+YYYYMMDD+SEQ, SEQ 가변길이) → 문서번호 TBM-YYYY-MMDD-NN.
export const docNoOf = (sessionCd) => {
  const s = String(sessionCd ?? "");
  if (!/^T\d{10,}$/.test(s)) return s || "-";
  const ymd = s.slice(1, 9);
  const seq = String(parseInt(s.slice(9), 10)).padStart(2, "0");
  return `TBM-${ymd.slice(0, 4)}-${ymd.slice(4, 8)}-${seq}`;
};

// 'yyyy-MM-dd HH:mm' → 각 부분. 없으면 ''.
const datePart = (dt) => (dt ? String(dt).slice(0, 10) : "");
const timePart = (dt) => (dt ? String(dt).slice(11, 16) : "");

// 리치 HTML → 표시용 텍스트(태그 제거 + 블록 경계 개행).
export const htmlToText = (html) => {
  if (!html) return "";
  try {
    const withBreaks = String(html)
      .replace(/<br\s*\/?>/gi, "\n")
      .replace(/<\/(p|div|li|h[1-6]|tr)>/gi, "\n");
    const doc = new DOMParser().parseFromString(withBreaks, "text/html");
    return (doc.body.textContent || "").replace(/\n{3,}/g, "\n\n").trim();
  } catch {
    return String(html);
  }
};

const applyRowBorder = (row, fromCol, toCol) => {
  for (let c = fromCol; c <= toCol; c++) {
    const cell = row.getCell(c);
    cell.border = C.border;
    if (!cell.font) cell.font = C.cellFont;
  }
};

/**
 * @param {Object} p
 * @param {string} p.cmpnyLabel   회사 표기(예: "파마리서치 (001)")
 * @param {string} p.siteLabel    사업장 표기(선택 사업장명 또는 "전체")
 * @param {string} p.periodLabel  "2026년 하반기"
 * @param {string} p.fromDate     YYYY-MM-DD
 * @param {string} p.toDate      YYYY-MM-DD
 * @param {Array}  p.sessions     시트1 대상 세션(EvidenceSessionResult[] — 반기 전체)
 * @param {Array}  p.workers      시트2 근로자 집계(EvidenceWorkerSummaryResult[])
 * @param {Object} p.details      건별 상세 { sessionList, attendeeList, riskList, mtrlList } (체크분 누적)
 * @param {Object} [p.managerSigns] 주관자 서명 이미지 맵 { [sessionCd]: { buffer: ArrayBuffer, extension: 'png'|'jpeg' } }
 * @param {string} p.fileName
 */
export async function buildTbmEvidenceExcel(p) {
  const wb = new ExcelJS.Workbook();
  const printDate = new Date().toISOString().slice(0, 10);

  // ═══════════════ 시트1: 반기 교육실시 목록 ═══════════════
  {
    const ws = wb.addWorksheet("반기 교육실시 목록");
    ws.columns = [
      { width: 5 }, { width: 11 }, { width: 30 }, { width: 22 }, { width: 10 },
      { width: 13 }, { width: 11 }, { width: 9 }, { width: 9 }, { width: 12 }, { width: 18 },
    ];
    ws.mergeCells("A1:K1");
    ws.getCell("A1").value = "안전보건 정기교육(TBM) 실시 목록 — 반기 총괄";
    ws.getCell("A1").font = C.titleFont;
    ws.mergeCells("A2:K2");
    ws.getCell("A2").value =
      `사업장: ${p.cmpnyLabel} · ${p.siteLabel}  ·  대상 기간: ${p.periodLabel} (${p.fromDate} ~ ${p.toDate})  ·  출력일: ${printDate}  ·  회차별 상세는 문서번호의 교육일지(건별) 참조`;
    ws.getCell("A2").font = C.metaFont;
    ws.mergeCells("A3:K3");
    ws.getCell("A3").value =
      "※ 산업안전보건법 제29조 정기교육 시간 인정용 · 고용노동부 TBM 정기교육 시간 인정 지침(2024.4.) 요건 반영 · 3년 이상 보존 · 시간은 세션 인정시간(분) 기준";
    ws.getCell("A3").font = C.noteFont;

    const header = ws.addRow([]);
    const headRow = ws.addRow([
      "No", "실시 일자", "교육 제목", "개설사·사업장", "주관자",
      "교육시간", "인정시간(분)", "참여 인원", "이수 인원", "위험성평가 연계", "문서번호(일지)",
    ]);
    void header;
    headRow.eachCell((cell) => {
      cell.fill = C.headerFill;
      cell.font = C.headerFont;
      cell.alignment = { vertical: "middle", horizontal: "center" };
      cell.border = C.border;
    });

    let totalMinutes = 0;
    (p.sessions || []).forEach((s, i) => {
      totalMinutes += Number(s.eduMinutes) || 0;
      const orgLabel = s.hostCmpnyNm
        ? `${s.hostCmpnyNm} · ${s.siteNm ?? ""} (연동)`
        : (s.siteNm ?? "");
      const row = ws.addRow([
        i + 1,
        datePart(s.endedAt) || datePart(s.startedAt),
        s.title ?? "",
        orgLabel,
        s.managerUserNm ?? "",
        `${timePart(s.startedAt)}~${timePart(s.endedAt)}`,
        Number(s.eduMinutes) || 0,
        Number(s.attendanceCount) || 0,
        Number(s.completedCount) || 0,
        Number(s.riskCount) > 0 ? `${s.riskCount}건` : "-",
        docNoOf(s.sessionCd),
      ]);
      applyRowBorder(row, 1, 11);
      row.getCell(1).alignment = { horizontal: "center" };
      [6, 7, 8, 9, 10].forEach((cIdx) => (row.getCell(cIdx).alignment = { horizontal: "center" }));
    });

    const sumRow = ws.addRow([
      "합계 (실시 횟수 / 총 인정시간)", "", "", "", "", "",
      totalMinutes, "", "", "",
      `${(p.sessions || []).length}회 실시 · 총 ${Math.floor(totalMinutes / 60)}시간 ${totalMinutes % 60}분`,
    ]);
    ws.mergeCells(`A${sumRow.number}:F${sumRow.number}`);
    applyRowBorder(sumRow, 1, 11);
    sumRow.font = { bold: true, size: 10 };
  }

  // ═══════════════ 시트2: 근로자별 이수현황 ═══════════════
  {
    const ws = wb.addWorksheet("근로자별 이수현황");
    ws.columns = [
      { width: 5 }, { width: 12 }, { width: 9 }, { width: 9 }, { width: 14 },
      { width: 12 }, { width: 12 }, { width: 10 }, { width: 22 }, { width: 24 },
    ];
    ws.mergeCells("A1:J1");
    ws.getCell("A1").value = "근로자별 안전보건 정기교육 이수 현황 (반기)";
    ws.getCell("A1").font = C.titleFont;
    ws.mergeCells("A2:J2");
    ws.getCell("A2").value =
      `사업장: ${p.cmpnyLabel} · ${p.siteLabel}  ·  대상 기간: ${p.periodLabel} (${p.fromDate} ~ ${p.toDate})  ·  출력일: ${printDate}`;
    ws.getCell("A2").font = C.metaFont;
    ws.mergeCells("A3:J3");
    ws.getCell("A3").value =
      "※ 법정 기준(산업안전보건법 시행규칙 별표4): 사무직·판매직 매반기 6시간 / 그 외 근로자 매반기 12시간. 두 기준의 충족 여부를 나란히 표기하니 근로자 직종에 해당하는 열을 확인하세요. 관리감독자는 연간 16시간(별도 관리). '기타 교육(분)' 노란 칸에 집체·온라인 등 TBM 외 교육을 수기 입력하면 누적·충족이 자동 재계산됩니다.";
    ws.getCell("A3").font = C.noteFont;
    ws.getRow(3).height = 40;
    ws.getCell("A3").alignment = { wrapText: true, vertical: "top" };

    ws.addRow([]);
    const headRow = ws.addRow([
      "No", "성명", "고용형태", "TBM 횟수", "TBM 인정시간(분)",
      "기타 교육(분)", "누적 합계(분)", "누적(시간)",
      "사무·판매직 기준(반기 6h)", "그 외 근로자 기준(반기 12h)",
    ]);
    headRow.eachCell((cell) => {
      cell.fill = C.headerFill;
      cell.font = C.headerFont;
      cell.alignment = { vertical: "middle", horizontal: "center", wrapText: true };
      cell.border = C.border;
    });
    headRow.height = 28;

    (p.workers || []).forEach((w, i) => {
      const row = ws.addRow([
        i + 1,
        w.userNm ?? "",
        w.userTypeCd === "DAILY" ? "일용직" : "정규직",
        Number(w.tbmCount) || 0,
        Number(w.tbmMinutes) || 0,
        0, // 기타 교육(분) — 수기 입력 칸
        null, null, null, null,
      ]);
      const r = row.number;
      row.getCell(7).value = { formula: `E${r}+F${r}` };
      row.getCell(8).value = { formula: `ROUND((E${r}+F${r})/60,1)` };
      row.getCell(9).value = {
        formula: `IF(H${r}>=6,"충족","미달 ("&ROUND(6-H${r},1)&"h 부족)")`,
      };
      row.getCell(10).value = {
        formula: `IF(H${r}>=12,"충족","미달 ("&ROUND(12-H${r},1)&"h 부족)")`,
      };
      applyRowBorder(row, 1, 10);
      [1, 3, 4, 5, 6, 7, 8, 9, 10].forEach(
        (cIdx) => (row.getCell(cIdx).alignment = { horizontal: "center" }),
      );
      row.getCell(6).fill = C.inputFill; // 수기 입력 칸 표시
    });
  }

  // ═══════════════ 시트3..N: 교육일지(건별 출력) ═══════════════
  const details = p.details || {};
  const attendeesBySession = groupBy(details.attendeeList, "sessionCd");
  const risksBySession = groupBy(details.riskList, "sessionCd");
  const mtrlsBySession = groupBy(details.mtrlList, "sessionCd");

  for (const s of details.sessionList || []) {
    const docNo = docNoOf(s.sessionCd);
    const ws = wb.addWorksheet(safeSheetName(`일지 ${docNo}`));
    ws.columns = [
      { width: 5 }, { width: 13 }, { width: 12 }, { width: 12 }, { width: 15 },
      { width: 15 }, { width: 13 }, { width: 10 }, { width: 10 },
    ];

    ws.mergeCells("A1:I1");
    ws.getCell("A1").value = "작업 전 안전점검회의(TBM) 교육일지 [건별 출력]";
    ws.getCell("A1").font = C.titleFont;
    ws.mergeCells("A2:I2");
    ws.getCell("A2").value =
      "※ 교육 1건당 1장 · '반기 교육실시 목록'의 문서번호와 매칭 · 시작·종료 시각과 참석 기록은 시스템(앱) 자동 기록값";
    ws.getCell("A2").font = C.noteFont;

    // ── 1. 교육 개요 ──
    const sec1 = ws.addRow(["1. 교육 개요"]);
    ws.mergeCells(`A${sec1.number}:I${sec1.number}`);
    sec1.getCell(1).fill = C.sectionFill;
    sec1.getCell(1).font = C.sectionFont;

    const orgLabel = s.hostCmpnyNm
      ? `${s.hostCmpnyNm} · ${s.siteNm ?? ""} (연동 세션)`
      : (s.siteNm ?? "");
    const overviewRows = [
      ["문서번호", docNo, "교육 제목", s.title ?? ""],
      ["사업장", orgLabel, "주관자(강사)", s.managerUserNm ?? ""],
      [
        "실시 일자",
        datePart(s.endedAt) || datePart(s.startedAt),
        "교육 시간",
        `${timePart(s.startedAt)} ~ ${timePart(s.endedAt)} (인정 ${Number(s.eduMinutes) || 0}분)`,
      ],
    ];
    for (const [k1, v1, k2, v2] of overviewRows) {
      const row = ws.addRow([k1, "", "", v1, "", k2, "", v2, ""]);
      const r = row.number;
      ws.mergeCells(`A${r}:C${r}`);
      ws.mergeCells(`D${r}:E${r}`);
      ws.mergeCells(`F${r}:G${r}`);
      ws.mergeCells(`H${r}:I${r}`);
      row.getCell(1).fill = C.sectionFill;
      row.getCell(6).fill = C.sectionFill;
      applyRowBorder(row, 1, 9);
    }

    // ── 2. 교육 내용 ──
    ws.addRow([]);
    const sec2 = ws.addRow(["2. 교육 내용 (당일 작업내용 · 위험요인 · 안전대책)"]);
    ws.mergeCells(`A${sec2.number}:I${sec2.number}`);
    sec2.getCell(1).fill = C.sectionFill;
    sec2.getCell(1).font = C.sectionFont;
    const bodyRow = ws.addRow([htmlToText(s.contentBody) || "-"]);
    ws.mergeCells(`A${bodyRow.number}:I${bodyRow.number}`);
    bodyRow.getCell(1).alignment = { wrapText: true, vertical: "top" };
    bodyRow.height = Math.min(
      180,
      Math.max(40, (htmlToText(s.contentBody).split("\n").length + 1) * 14),
    );
    applyRowBorder(bodyRow, 1, 9);

    // ── 3. 연계 기록 ──
    ws.addRow([]);
    const sec3 = ws.addRow(["3. 연계 기록 (해당 시)"]);
    ws.mergeCells(`A${sec3.number}:I${sec3.number}`);
    sec3.getCell(1).fill = C.sectionFill;
    sec3.getCell(1).font = C.sectionFont;

    const risks = risksBySession[s.sessionCd] || [];
    const riskText = risks.length
      ? risks.map((r) => [r.processNm, r.hazardNm].filter(Boolean).join(" > ")).join("\n")
      : "-";
    const mtrls = mtrlsBySession[s.sessionCd] || [];
    const mtrlText = mtrls.length ? mtrls.map((m) => m.mtrlTitle).join("\n") : "-";
    const gpsText =
      s.gpsVerifyTypeCd === "AUTO"
        ? `GPS 자동 검증 · 반경 ${s.gpsVerifyRadiusM ?? "-"}m 이내 · 앱 기록(시작/종료 시각 시스템 자동 기록)`
        : s.gpsVerifyTypeCd === "MANUAL"
          ? "관리자 수동 확인 · 앱 기록(시작/종료 시각 시스템 자동 기록)"
          : "위치 검증 미사용";
    for (const [label, text] of [
      ["위험성평가 연계", riskText],
      ["교육자료", mtrlText],
      ["위치 검증", gpsText],
    ]) {
      const row = ws.addRow([label, "", text]);
      const r = row.number;
      ws.mergeCells(`A${r}:B${r}`);
      ws.mergeCells(`C${r}:I${r}`);
      row.getCell(1).fill = C.sectionFill;
      row.getCell(3).alignment = { wrapText: true, vertical: "top" };
      const lines = String(text).split("\n").length;
      if (lines > 1) row.height = Math.min(100, lines * 14 + 6);
      applyRowBorder(row, 1, 9);
    }

    // ── 4. 참석자 명단 ──
    ws.addRow([]);
    const sec4 = ws.addRow(["4. 참석자 명단"]);
    ws.mergeCells(`A${sec4.number}:I${sec4.number}`);
    sec4.getCell(1).fill = C.sectionFill;
    sec4.getCell(1).font = C.sectionFont;

    const attHead = ws.addRow([
      "No", "성명", "고용형태", "소속", "입실 시각", "종료 시각", "이수 여부", "서명", "",
    ]);
    ws.mergeCells(`H${attHead.number}:I${attHead.number}`);
    attHead.eachCell((cell) => {
      cell.fill = C.headerFill;
      cell.font = C.headerFont;
      cell.alignment = { horizontal: "center" };
      cell.border = C.border;
    });

    const atts = attendeesBySession[s.sessionCd] || [];
    let completedCnt = 0;
    atts.forEach((a, i) => {
      if (a.completionStatusCd === "COMPLETED") completedCnt += 1;
      const row = ws.addRow([
        i + 1,
        a.userNm ?? "",
        a.userTypeCd === "DAILY" ? "일용직" : "정규직",
        a.cmpnyNm ?? "",
        a.entryAt ?? "-",
        a.exitAt ?? "-",
        a.completionStatusCd === "COMPLETED"
          ? "이수"
          : a.completionStatusCd === "NOT_COMPLETED"
            ? "미이수"
            : "미완료",
        a.signedYn === "Y" ? "서명함" : "-",
        "",
      ]);
      ws.mergeCells(`H${row.number}:I${row.number}`);
      applyRowBorder(row, 1, 9);
      [1, 3, 5, 6, 7, 8].forEach(
        (cIdx) => (row.getCell(cIdx).alignment = { horizontal: "center" }),
      );
    });
    const attSum = ws.addRow([
      `합계 (참석 ${atts.length}명 / ${completedCnt}명 이수)`, "", "", "", "", "", "", "", "",
    ]);
    ws.mergeCells(`A${attSum.number}:I${attSum.number}`);
    attSum.font = { bold: true, size: 10 };
    applyRowBorder(attSum, 1, 9);

    // ── 5. 확인 (tbm04-manager-sign: 주관자 서명 단독 — 안전관리자 확인 란 제거 확정) ──
    ws.addRow([]);
    const sec5 = ws.addRow(["5. 확인"]);
    ws.mergeCells(`A${sec5.number}:I${sec5.number}`);
    sec5.getCell(1).fill = C.sectionFill;
    sec5.getCell(1).font = C.sectionFont;
    // 레이아웃: A:B=라벨 / C:F=서명 이미지 영역(없으면 빈칸=수기용) / G:I=서명 일시.
    const confirmRow = ws.addRow(["주관자 서명", "", "", "", "", "", "서명 일시", "", ""]);
    const cr = confirmRow.number;
    ws.mergeCells(`A${cr}:B${cr}`);
    ws.mergeCells(`C${cr}:F${cr}`);
    ws.mergeCells(`G${cr}:I${cr}`);
    confirmRow.getCell(1).fill = C.sectionFill;
    // 라벨/일시 가로·세로 중앙 정렬(기존 좌하단 붙음 결함 수정).
    confirmRow.getCell(1).alignment = { horizontal: "center", vertical: "middle" };
    confirmRow.getCell(7).value =
      s.managerSignYn === "Y" && s.managerSignedAt ? `서명 일시: ${s.managerSignedAt}` : "";
    confirmRow.getCell(7).alignment = { horizontal: "center", vertical: "middle" };

    // 서명 이미지 삽입(C:F 병합영역 내부 앵커). 없으면 빈칸 유지(수기 서명 공간, 높이 34).
    const sign = p.managerSigns?.[s.sessionCd];
    if (sign?.buffer && s.managerSignYn === "Y") {
      confirmRow.height = 64;
      const imgId = wb.addImage({
        buffer: sign.buffer,
        extension: sign.extension === "jpeg" ? "jpeg" : "png",
      });
      // tl 은 0-based {col, row}. cr 은 1-based 행번호 → row: cr-1 + 여백.
      ws.addImage(imgId, {
        tl: { col: 2.1, row: cr - 1 + 0.08 }, // C열 안쪽 여백
        ext: { width: 150, height: 75 }, // 캔버스 2:1 비율 축소
        editAs: "oneCell",
      });
    } else {
      confirmRow.height = 34;
    }
    applyRowBorder(confirmRow, 1, 9);
  }

  // ═══════════════ 다운로드 ═══════════════
  const buffer = await wb.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = p.fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

const groupBy = (list, key) => {
  const map = {};
  for (const item of list || []) {
    const k = item?.[key];
    if (!map[k]) map[k] = [];
    map[k].push(item);
  }
  return map;
};
