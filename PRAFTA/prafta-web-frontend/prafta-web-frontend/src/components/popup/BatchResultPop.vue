<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>처리 결과</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- Summary + Table -->
        <div class="viewBody">
          <p class="batch-result-summary">
            요청 <span class="summary-total">{{ totalCount }}</span
            >건 중 <span class="summary-success">{{ successCount }}</span
            >건 성공 / <span class="summary-fail">{{ failCount }}</span
            >건 실패
          </p>
          <p class="batch-result-subtitle">상세항목</p>
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th width="20%">{{ identifierLabel }}</th>
                  <th>비고</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!dataList || dataList.length === 0">
                  <tr>
                    <td colspan="2" class="edu-grid-empty">
                      상세 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(row, idx) in dataList" :key="idx">
                    <td>{{ row.errorItem ?? "" }}</td>
                    <td class="cell-wrap">{{ row.message ?? "" }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnExportExcel">
              엑셀 출력
            </button>
            <button class="btn btn-second" @click="$emit('close')">확인</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref } from "vue";
import * as XLSX from "xlsx";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const props = defineProps({
  totalCount: { type: Number, default: 0 },
  successCount: { type: Number, default: 0 },
  failCount: { type: Number, default: 0 },
  identifierLabel: { type: String, default: "구분자" },
  dataList: {
    type: Array,
    default: () => [],
  },
});

defineEmits(["close"]);

const modalRef = ref(null);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

/*
 * prafta-052 — 엑셀 업로드 실패 행 재업로드용 양식 헤더(16컬럼).
 * [중요] 백엔드 UserExcelRowParser.HEADERS 와 순서·문자열을 동일하게 유지해야 한다.
 * 양식이 변경되면 백엔드 HEADERS 와 이 배열을 반드시 동시에 수정한다(drift 방지).
 */
const EXCEL_HEADERS = [
  "사용자ID(필수)",
  "사용자명(필수)",
  "권한코드(필수)",
  "사업장번호(필수)",
  "소속부서코드(필수)",
  "휴대폰번호(필수)",
  "이메일",
  "성별(M/F)",
  "생년월일(YYMMDD)",
  "직급코드",
  "입사일(YYYYMMDD)",
  "고용형태(REGULAR/CONTRACT/EXECUTIVE)",
  "경력인정개월수",
  "상세 설명",
];

/*
 * prafta-052 — 시트1 1행 안내문. 업로드 파서가 4행(인덱스 3)부터 데이터로 읽으므로,
 * 시트1을 양식과 동일 구조(1행 안내 / 2행 헤더 / 3행 예시 placeholder / 4행~ 데이터)로 만들어
 * 사용자가 셀만 수정해 그대로 재업로드할 수 있게 한다.
 */
const RETRY_NOTICE =
  "4행부터 데이터가 저장됩니다. 1행(안내)과 3행(예시) 사이의 2행은 헤더이며, 헤더 행은 수정/삭제하지 마세요. 셀 수정 후 이 시트를 그대로 업로드하세요.";

/** 셀 표시 폭 추정 — 비ASCII(한글 등)는 약 2배 폭으로 계산. */
const cellDisplayWidth = (value) => {
  let width = 0;
  for (const ch of String(value ?? "")) {
    width += ch.charCodeAt(0) > 127 ? 2 : 1;
  }
  return width;
};

/**
 * 행 배열들을 받아 컬럼별 권장 폭(wch) 목록 생성.
 * 헤더/데이터 중 최대 표시 폭 + 여백, 최소 10 ~ 최대 40 으로 클램프(너무 좁거나 과도하게 넓지 않게).
 */
const autoColWidths = (rows, colCount) => {
  const widths = new Array(colCount).fill(0);
  for (const row of rows) {
    for (let c = 0; c < colCount; c++) {
      const w = cellDisplayWidth(row?.[c]);
      if (w > widths[c]) widths[c] = w;
    }
  }
  return widths.map((w) => ({ wch: Math.min(Math.max(w + 2, 10), 40) }));
};

/**
 * XLSX 다운로드.
 * - 엑셀 업로드 실패분(원본 행 sourceRow 보유): 재업로드용 2시트(실패 항목 + 실패 사유).
 * - 그 외(그리드 체크저장 등 원본 행 없음): 기존 단일 시트(사용자ID + 비고) 그대로.
 */
const fnExportExcel = () => {
  const list = props.dataList || [];

  // prafta-052 — 원본 행이 하나라도 존재하면 2시트 모드(부분 성공/혼합 방어).
  const hasSourceRow = list.some(
    (r) => Array.isArray(r.sourceRow) && r.sourceRow.length > 0
  );

  if (!hasSourceRow) {
    // 기존 단일 시트 경로(그리드 저장 등) — 회귀 방지를 위해 동작 그대로 유지.
    const summary = `요청 ${props.totalCount}건 중 ${props.successCount}건 성공 / ${props.failCount}건 실패`;
    const header = [props.identifierLabel, "비고"];
    const rows = list.map((r) => [r.errorItem ?? "", r.message ?? ""]);

    const wsData = [[summary], [], ["상세항목"], header, ...rows];
    const ws = XLSX.utils.aoa_to_sheet(wsData);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "처리결과");
    XLSX.writeFile(
      wb,
      `처리결과_${new Date().toISOString().slice(0, 10)}.xlsx`
    );
    return;
  }

  // prafta-052 — 엑셀 업로드 실패분: 재업로드용 2시트.
  // 시트1 "실패 항목" — 양식과 동일 구조(파서 DATA_START_ROW_INDEX=3 정합).
  const examplePlaceholder = new Array(EXCEL_HEADERS.length).fill(""); // 3행 예시(파서 skip 대상)
  const sheet1Rows = list.map((r) =>
    Array.isArray(r.sourceRow) ? r.sourceRow : []
  );
  const ws1Data = [
    [RETRY_NOTICE],
    EXCEL_HEADERS,
    examplePlaceholder,
    ...sheet1Rows,
  ];
  const ws1 = XLSX.utils.aoa_to_sheet(ws1Data);
  // 1행 안내문을 헤더 전체 컬럼으로 병합 — 양식 가독성.
  ws1["!merges"] = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: EXCEL_HEADERS.length - 1 } },
  ];
  // 컬럼 폭 자동 맞춤(헤더+데이터 기준, 안내행은 병합 셀이라 제외).
  ws1["!cols"] = autoColWidths(
    [EXCEL_HEADERS, ...sheet1Rows],
    EXCEL_HEADERS.length
  );

  // 시트2 "실패 사유" — 행번호는 양식 4행 기준(index+4: 1행 안내/2행 헤더/3행 예시).
  const ws2Header = ["엑셀 행번호", "사용자ID", "실패 사유", "에러코드"];
  const ws2Rows = list.map((r) => [
    (r.index ?? 0) + 4,
    r.errorItem ?? "",
    r.message ?? "",
    r.errorCode ?? "",
  ]);
  const ws2 = XLSX.utils.aoa_to_sheet([ws2Header, ...ws2Rows]);
  // 시트2도 컬럼 폭 자동 맞춤(사유 메시지가 길어 짤림 방지).
  ws2["!cols"] = autoColWidths([ws2Header, ...ws2Rows], ws2Header.length);

  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws1, "실패 항목");
  XLSX.utils.book_append_sheet(wb, ws2, "실패 사유");
  XLSX.writeFile(
    wb,
    `사용자업로드_실패항목_${new Date().toISOString().slice(0, 10)}.xlsx`
  );
};
</script>

<style scoped>
.batch-result-summary {
  font-size: 1rem;
  color: var(--color-text-strong, #111827);
  margin: 0 0 0.5rem 0;
}

.batch-result-summary .summary-total {
  font-weight: 700;
  color: #111827;
  font-size: 1.1rem;
}

.batch-result-summary .summary-success {
  font-weight: 700;
  color: #15803d;
  font-size: 1.1rem;
}

.batch-result-summary .summary-fail {
  font-weight: 700;
  color: #dc2626;
  font-size: 1.1rem;
}

.batch-result-subtitle {
  font-size: 0.875rem;
  color: var(--color-text-muted, #4b5563);
  margin: 0 0 0.5rem 0;
}
</style>
