<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide modal-content-sch-hist"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>스케줄 변경이력</span>
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
                  <th>적용일시</th>
                  <th>1근무시간</th>
                  <th>1휴게시간</th>
                  <th>2근무시간</th>
                  <th>2휴게시간</th>
                  <th>사용여부</th>
                  <th>변경자</th>
                  <th>변경일시</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!schHistList || schHistList.length === 0">
                  <tr>
                    <td colspan="7" class="edu-grid-empty">
                      변경이력이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(item, idx) in schHistList" :key="idx">
                    <td>{{ item.applyDate ?? "-" }}</td>
                    <td>{{ item.fstSchTime ?? "-" }}</td>
                    <td>{{ item.fstSchBrkMin ?? "-" }}</td>
                    <td>{{ item.secSchTime ?? "-" }}</td>
                    <td>{{ item.secSchBrkMin ?? "-" }}</td>
                    <td>{{ item.useYn ?? "-" }}</td>
                    <td>{{ item.chgUserNm ?? item.userNm ?? "-" }}</td>
                    <td>{{ item.chgDt ?? item.insertDate ?? "-" }}</td>
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

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();

// ================ Props & Emits ================
const props = defineProps({
  schData_p: { type: Object, default: null },
});
defineEmits(["close"]);

// ================ Instance & Composables ================
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Refs (Variables) ================
const positionStyle = computed(() => {
  const padding = 16;
  const maxX = window.innerWidth - (1000 + padding);
  const maxY = window.innerHeight - (400 + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

/** API 연동 시 schHistList에 데이터 할당 */
const schHistList = ref([]);

// ================ Life Cycle Functions ================
onMounted(() => {
  fnSearch();
});

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/attd01/sch-hist-lists", {
      params: {
        schCd: props.schData_p?.schCd,
        siteCd: props.schData_p?.siteCd,
      },
    });

    if (response.status === 200) {
      schHistList.value = response.data.schHistList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";
    await proxy.$alert(msg);
  }
};
</script>

<style scoped>
.modal-content-sch-hist {
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
