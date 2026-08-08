<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide modal-content-sch-assigned"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>배정 현황</span>
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

        <div class="modal-body-hist">
          <div class="hist-info-row">
            <span class="hist-label">근무코드</span>
            <span class="hist-value">{{ schData_p?.schNo ?? "-" }}</span>
          </div>

          <div
            class="table-box overflow-x-auto rounded-md border border-slate-300"
            style="--box-h: 50vh; --box-ox: auto"
          >
            <table class="data-grid w-full text-sm text-left rtl:text-right">
              <thead>
                <tr>
                  <ThSortable
                    label="사용자명"
                    col-key="userNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.userNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="부서"
                    col-key="nodeNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.nodeNm"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="최초 배정일"
                    col-key="firstWorkYmd"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.firstWorkYmd"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="최근 배정일"
                    col-key="lastWorkYmd"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.lastWorkYmd"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="배정 일수"
                    col-key="assignedDayCount"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.assignedDayCount"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                </tr>
              </thead>
              <tbody>
                <template
                  v-if="!assignedUserList || assignedUserList.length === 0"
                >
                  <tr>
                    <td colspan="5" class="edu-grid-empty">
                      배정된 근로자가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(item, idx) in sortedData" :key="idx">
                    <td>{{ item.userNm ?? "-" }}</td>
                    <td>{{ item.nodeNm ?? "-" }}</td>
                    <td>{{ item.firstWorkYmd ?? "-" }}</td>
                    <td>{{ item.lastWorkYmd ?? "-" }}</td>
                    <td>{{ item.assignedDayCount ?? "-" }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
// ================ Imports ================
import { ref, getCurrentInstance, computed, onMounted } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();

// ================ Props & Emits ================
const props = defineProps({
  schData_p: { type: Object, default: null }, // { schCd, schNo, siteCd, ... } — Attd_01_1.vue 그리드 행
});
defineEmits(["close"]);

// ================ Drag ================
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});
const positionStyle = computed(() => {
  const padding = 16;
  const maxX = window.innerWidth - (1000 + padding);
  const maxY = window.innerHeight - (400 + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

// ================ 반응형 상태 ================
const assignedUserList = ref([]);

// ================ 정렬/컬럼폭 (기존 composable 재사용) ================
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(assignedUserList);
const { colWidths, onResize } = useColumnResize({
  userNm: 120,
  nodeNm: 120,
  firstWorkYmd: 110,
  lastWorkYmd: 110,
  assignedDayCount: 90,
});

// ================ Life Cycle Functions ================
onMounted(() => {
  fnSearch();
});

const fnSearch = async () => {
  const schCd = props.schData_p?.schCd;
  if (proxy.$util.isEmpty(schCd)) {
    return;
  }
  try {
    const response = await axios.get(`/webApi/attd01/${schCd}/assigned-users`, {
      params: {
        siteCd: props.schData_p?.siteCd,
      },
    });

    if (response.status === 200) {
      assignedUserList.value = response.data.assignedUserResultList ?? [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
.modal-content-sch-assigned {
  max-width: 1000px;
  width: min(92vw, 1000px);
}
.modal-body-hist {
  padding: 1.2rem;
  overflow-x: hidden;
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.hist-info-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.hist-label {
  font-weight: 500;
  color: var(--color-text-strong, #111827);
  min-width: 5rem;
}
.hist-value {
  color: var(--color-text, #374151);
}
.table-box {
  overflow-x: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
}
.data-grid th,
.data-grid td {
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.data-grid th {
  background: var(--color-bg, #f9fafb);
  font-weight: 600;
  color: var(--color-text-strong, #111827);
}
.data-grid tbody tr:last-child td {
  border-bottom: none;
}
.edu-grid-empty {
  text-align: center;
  padding: 2rem;
  color: var(--color-text-muted, #6b7280);
}
</style>
