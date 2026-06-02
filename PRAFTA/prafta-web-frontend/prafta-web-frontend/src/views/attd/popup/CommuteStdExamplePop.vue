<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        ref="modalRef"
        class="modal-content-normal commute-std-example-pop"
        :style="positionStyle"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>출퇴근 시간 표준화 계산 예시</span>
          <button type="button" class="icon-button" @click="emit('close')">
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

        <div class="cse-body">
          <p class="cse-lead">9시 출근, 18시 퇴근 근무타입 사용자의 경우</p>

          <div class="cse-tables">
            <div class="cse-table-block">
              <p class="cse-block-title">출근 (올림)</p>
              <div class="cse-table-wrap">
                <table class="cse-table">
                  <thead>
                    <tr>
                      <th>실제 출근</th>
                      <th v-for="s in stepLabels" :key="'in-h-' + s">
                        {{ s }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, ri) in arrivalRows" :key="'in-' + ri">
                      <td>{{ row.actual }}</td>
                      <td
                        v-for="(cell, ci) in row.results"
                        :key="ci"
                        class="cse-num"
                      >
                        {{ cell }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div class="cse-table-block">
              <p class="cse-block-title">퇴근 (내림)</p>
              <div class="cse-table-wrap">
                <table class="cse-table">
                  <thead>
                    <tr>
                      <th>실제 퇴근</th>
                      <th v-for="s in stepLabels" :key="'out-h-' + s">
                        {{ s }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, ri) in departureRows" :key="'out-' + ri">
                      <td>{{ row.actual }}</td>
                      <td
                        v-for="(cell, ci) in row.results"
                        :key="ci"
                        class="cse-num"
                      >
                        {{ cell }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <ul class="cse-notes">
            <li>
              출근: 근무 시작(09:00) 기준 이른 출근분을 표준화 단위로
              올림합니다.
            </li>
            <li>
              퇴근: 근무 종료(18:00) 기준 늦은 퇴근분을 표준화 단위로 내립니다.
            </li>
          </ul>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button
              type="button"
              class="btn btn-secondary"
              @click="emit('close')"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const emit = defineEmits(["close"]);

const stepLabels = ["5분", "10분", "15분", "30분"];

const arrivalRows = [
  { actual: "08:54", results: ["08:55", "09:00", "09:00", "09:00"] },
  { actual: "08:47", results: ["08:50", "08:50", "09:00", "09:00"] },
  { actual: "08:33", results: ["08:35", "08:40", "08:45", "09:00"] },
  { actual: "08:27", results: ["08:30", "08:30", "08:30", "08:30"] },
];

const departureRows = [
  { actual: "18:04", results: ["18:00", "18:00", "18:00", "18:00"] },
  { actual: "18:17", results: ["18:15", "18:10", "18:15", "18:00"] },
  { actual: "18:23", results: ["18:20", "18:20", "18:15", "18:00"] },
  { actual: "18:37", results: ["18:35", "18:30", "18:30", "18:30"] },
];

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2.8,
});

const positionStyle = computed(() => {
  const padding = 16;
  const modalW = 920;
  const modalH = 640;
  const maxX = window.innerWidth - (modalW + padding);
  const maxY = window.innerHeight - (modalH + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: `${y}px`, left: `${x}px` };
});
</script>

<style scoped>
.commute-std-example-pop {
  width: min(920px, calc(100vw - 2rem));
}

.cse-body {
  padding: 1rem 1.25rem 1.25rem;
  overflow: auto;
  flex: 1;
  min-height: 0;
  max-height: min(72vh, 640px);
}

.cse-lead {
  margin: 0 0 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #111827;
  line-height: 1.45;
}

.cse-table-block {
  min-width: 0;
}

.cse-tables {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem 1.25rem;
  align-items: start;
}

@media (max-width: 800px) {
  .cse-tables {
    grid-template-columns: 1fr;
  }
}

.cse-block-title {
  margin: 0 0 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #374151;
}

.cse-table-wrap {
  border: 1px solid #dee2e6;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.cse-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
}

.cse-table th,
.cse-table td {
  border: 1px solid #dee2e6;
  padding: 0.45rem 0.4rem;
  text-align: center;
  background: #fff;
}

.cse-table th {
  background: #f3f4f6;
  font-weight: 600;
  color: #374151;
}

.cse-table td:first-child,
.cse-table th:first-child {
  font-weight: 600;
  color: #111827;
}

.cse-num {
  font-variant-numeric: tabular-nums;
}

.cse-notes {
  margin: 1rem 0 0;
  padding-left: 1.1rem;
  font-size: 0.75rem;
  color: #6b7280;
  line-height: 1.55;
}

.cse-notes li + li {
  margin-top: 0.25rem;
}

.btn-secondary {
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f9fafb;
}
</style>
