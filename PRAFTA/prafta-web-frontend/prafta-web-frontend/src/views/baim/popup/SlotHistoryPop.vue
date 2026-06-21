<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal modal-content-slot-history"
        :style="positionStyle"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>슬롯 변경이력</span>
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

        <div class="modal-body-slot-history">
          <p class="modal-desc">
            {{ siteNm }} · {{ slotNo }}번 슬롯의 최근 30일 점유/해제 이력입니다.
          </p>

          <div class="history-table-wrap">
            <table class="history-table">
              <thead>
                <tr>
                  <th>발급채널</th>
                  <th>점유일시</th>
                  <th>해제일시</th>
                  <th>점유자</th>
                  <th>연락처</th>
                  <th>해제자</th>
                  <th>해제유형</th>
                  <th>사유</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="isLoading">
                  <td colspan="8" class="state-cell">불러오는 중...</td>
                </tr>
                <tr v-else-if="historyList.length === 0">
                  <td colspan="8" class="state-cell">이력이 없습니다.</td>
                </tr>
                <tr v-else v-for="item in historyList" :key="item.hisId">
                  <td>{{ item.issueChannelNm || "-" }}</td>
                  <td>{{ item.occupyDtime || "-" }}</td>
                  <td>
                    <span v-if="item.releaseDtime">{{ item.releaseDtime }}</span>
                    <span v-else class="badge-active">점유 중</span>
                  </td>
                  <td>{{ item.userNmMasked || "-" }}</td>
                  <td>{{ item.mblNoMasked || "-" }}</td>
                  <td>{{ item.releaseUser || "-" }}</td>
                  <td>{{ item.releaseTypeNm || "-" }}</td>
                  <td class="reason-cell">{{ item.releaseReason || "-" }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer">
          <div class="footer-actions">
            <button class="btn btn-secondary" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";

const props = defineProps({
  siteCd: { type: String, default: "" },
  siteNm: { type: String, default: "" },
  slotNo: { type: String, default: "" },
});
defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2,
});

const historyList = ref([]);
const isLoading = ref(false);

const positionStyle = computed(() => {
  const padding = 16;
  const modalWidth = 880;
  const modalHeight = 560;
  const maxX = window.innerWidth - (modalWidth + padding);
  const maxY = window.innerHeight - (modalHeight + padding);
  const x = Math.max(padding, Math.min(maxX, position.value.x));
  const y = Math.max(padding, Math.min(maxY, position.value.y));
  return { top: y + "px", left: x + "px" };
});

const fnLoadHistory = async () => {
  if (!props.siteCd || !props.slotNo) return;

  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/baim05/daily-user-slot-his", {
      params: {
        siteCd: props.siteCd,
        slotNo: props.slotNo,
      },
    });

    if (response.status === 200) {
      historyList.value = response.data?.slotHisList ?? [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "이력 조회 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    isLoading.value = false;
  }
};

onMounted(fnLoadHistory);
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.modal-content-slot-history {
  width: 880px;
  max-width: 95vw;
  max-height: 85vh;
}

.modal-body-slot-history {
  padding: 1rem 1.5rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.modal-desc {
  margin: 0 0 1rem;
  font-size: 0.8125rem;
  color: #6b7280;
  line-height: 1.5;
}

.history-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
  white-space: nowrap;
}

.history-table th,
.history-table td {
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #f1f3f5;
  text-align: center;
}

.history-table thead th {
  background: #f9fafb;
  color: #374151;
  font-weight: 600;
  position: sticky;
  top: 0;
  /* sticky 헤더가 스크롤되는 본문 위에 그려지도록 z-index 부여 */
  z-index: 1;
}

.history-table tbody tr:last-child td {
  border-bottom: none;
}

.state-cell {
  padding: 1.5rem 0;
  color: #9ca3af;
}

.reason-cell {
  white-space: normal;
  max-width: 200px;
  text-align: left;
}

.badge-active {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  background: #ecfdf5;
  color: #16a34a;
  font-size: 0.75rem;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 1rem 1.5rem;
  background: var(--modal-footer-bg, #f9fafb);
  border-top: 1px solid var(--modal-border, #e5e7eb);
}

.footer-actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.btn-secondary {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #374151;
}

.btn-secondary:hover {
  background: #f9fafb;
}
</style>
