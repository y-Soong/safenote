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
          <div class="btn-group" style="margin-top: 1rem">
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

/** XLSX 다운로드 (팝업 내용 그대로) */
const fnExportExcel = () => {
  const summary = `요청 ${props.totalCount}건 중 ${props.successCount}건 성공 / ${props.failCount}건 실패`;
  const header = [props.identifierLabel, "비고"];
  const rows = (props.dataList || []).map((r) => [
    r.errorItem ?? "",
    r.message ?? "",
  ]);

  const wsData = [[summary], [], ["상세항목"], header, ...rows];
  const ws = XLSX.utils.aoa_to_sheet(wsData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "처리결과");
  XLSX.writeFile(wb, `처리결과_${new Date().toISOString().slice(0, 10)}.xlsx`);
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
