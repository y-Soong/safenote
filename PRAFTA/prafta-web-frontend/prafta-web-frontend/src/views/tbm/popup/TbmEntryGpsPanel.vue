<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>입실자 / GPS 거리</span>
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

        <div class="panel-meta">
          <span class="meta-item">검증 반경: {{ radiusM_p || "-" }}m</span>
          <span class="meta-item">입실 {{ entries.length }}명</span>
        </div>

        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>이름</th>
                  <th>입실시각</th>
                  <th>거리(m)</th>
                  <th>입실유형</th>
                  <th>처리</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="isLoading">
                  <td colspan="5" class="grid-msg">조회 중...</td>
                </tr>
                <tr v-else-if="entries.length === 0">
                  <td colspan="5" class="grid-msg">입실자가 없습니다.</td>
                </tr>
                <tr
                  v-for="row in entries"
                  :key="row.attendanceCd"
                  :class="{ 'row-over': isOver(row) }"
                >
                  <td>{{ row.userNm }}</td>
                  <td>{{ row.entryAt }}</td>
                  <td>
                    <span :class="isOver(row) ? 'dist-over' : 'dist-ok'">
                      {{
                        row.entryDistanceM == null ? "-" : row.entryDistanceM
                      }}
                      <span v-if="isOver(row)" class="over-badge"
                        >반경 초과</span
                      >
                    </span>
                  </td>
                  <td>{{ entryTypeNm(row.entryTypeCd) }}</td>
                  <td>
                    <button
                      class="btn btn-second btn-sm"
                      :disabled="isBusy"
                      @click="fnEject(row)"
                    >
                      내보내기
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";

const { proxy } = getCurrentInstance();
const { open: openPop, close: closePop } = useModal();

const props = defineProps({
  sessionCd_p: String,
  radiusM_p: Number,
  onSearch: Function,
});
defineEmits(["close"]);

const modalRef = ref(null);
const entries = ref([]);
const isLoading = ref(false);
const isBusy = ref(false);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(() => {
  fnSearch();
});

// 반경 초과 판정(표시용). 거리 미산출(null)은 초과로 보지 않음.
const isOver = (row) => {
  const r = Number(props.radiusM_p);
  if (!r || row.entryDistanceM == null) return false;
  return Number(row.entryDistanceM) > r;
};

const entryTypeNm = (cd) => {
  switch (cd) {
    case "SELF_DEVICE":
      return "본인앱";
    case "MANAGER_DIRECT":
      return "관리자 입실";
    case "MANAGER_QR_SCAN":
      return "관리자 QR";
    default:
      return cd || "-";
  }
};

// 입실자 명단 조회. SessionAttendanceListResponse: { sessionCd, totalCount, attendanceList }
const fnSearch = async () => {
  if (isLoading.value) return;
  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/tbm02/session-attendances", {
      params: { sessionCd: props.sessionCd_p },
    });

    if (response.status === 200) {
      entries.value = response.data?.attendanceList || [];
    }
  } catch (err) {
    entries.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

const fnEject = (row) => {
  openPop(ReasonInputModal, {
    title: "입실자 내보내기",
    message: `${row.userNm} 님을 내보내시겠습니까? 사유를 입력해 주세요.`,
    placeholder: "내보내기 사유를 입력해 주세요.",
    required: true,
    onConfirm: async (reason) => {
      closePop();
      if (isBusy.value) return;
      isBusy.value = true;
      try {
        const response = await axios.post(
          "/webApi/tbm02/eject-attendance",
          {
            sessionCd: props.sessionCd_p,
            attendanceCd: row.attendanceCd,
            reason,
          },
          { headers: { "Content-Type": "application/json" } }
        );

        if (response.status === 200) {
          await proxy.$alert(`${row.userNm} 님을 내보냈습니다.`);
          await fnSearch();
          if (typeof props.onSearch === "function") props.onSearch();
        }
      } catch (err) {
        await proxy.$alert(
          resolveApiErrorMessage(err, "내보내기 중 오류가 발생했습니다.")
        );
      } finally {
        isBusy.value = false;
      }
    },
    onCancel: () => closePop(),
  });
};
</script>

<style scoped>
.panel-meta {
  display: flex;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}

.meta-item {
  font-size: var(--btn-font);
}

.grid-msg {
  text-align: center;
  padding: 1rem;
  color: var(--color-text-muted);
}

.row-over {
  background: var(--color-warning-bg);
}

.dist-ok {
  color: var(--color-text);
}

.dist-over {
  color: var(--color-danger);
  font-weight: 600;
}

.over-badge {
  display: inline-block;
  margin-left: 0.4rem;
  padding: 1px 6px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  background: var(--color-danger);
  color: var(--color-surface);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
