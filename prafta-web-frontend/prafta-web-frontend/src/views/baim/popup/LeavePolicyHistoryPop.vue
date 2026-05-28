<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-wide leave-history-modal">
        <!-- 헤더 -->
        <div class="modal-header leave-history-header">
          <span>연차 부여 정책 변경 이력</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- 바디 -->
        <div class="modal-body leave-history-body">
          <!-- 로딩 -->
          <p v-if="isLoading" class="lh-state">조회 중입니다...</p>

          <!-- 비어 있음 -->
          <p v-else-if="historyList.length === 0" class="lh-state">
            변경 이력이 없습니다.
          </p>

          <!-- 이력 목록 -->
          <div v-else class="lh-table-wrap">
            <table class="lh-table">
              <thead>
                <tr>
                  <th>변경 유형</th>
                  <th>변경 사유</th>
                  <th>영향 인원</th>
                  <th>변경자</th>
                  <th>변경 일시</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in historyList" :key="row.histId">
                  <td>{{ changeTypeLabel(row.changeType) }}</td>
                  <td class="lh-reason">{{ row.changeReason || "-" }}</td>
                  <td>{{ affectedUserCount(row.impactSummary) }}</td>
                  <td>{{ row.insertNo || "-" }}</td>
                  <td>{{ row.insertDate || "-" }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="$emit('close')">
            닫기
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// ================ Props & Emits ================
defineEmits(["close"]);

// ================ Instance ================
const { proxy } = getCurrentInstance();

// ================ Refs ================
const historyList = ref([]);
const isLoading = ref(false);

// ================ Life Cycle ================
onMounted(() => {
  fnLoadHistory();
});

// ================ API ================
const fnLoadHistory = async () => {
  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/baim07/policy/history", {
      params: { page: 1, size: 50 },
    });
    // 응답: { history: { page, size, totalCount, items } }
    historyList.value = response.data?.history?.items ?? [];
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

// ================ Methods ================
const changeTypeLabel = (type) => {
  const map = {
    CREATE: "신규 등록",
    UPDATE: "변경",
    PRESET_CHANGE: "프리셋 변경",
  };
  return map[type] || type || "-";
};

// IMPACT_SUMMARY(JSON 문자열)에서 영향 인원 추출
const affectedUserCount = (impactSummary) => {
  if (!impactSummary) return "-";
  try {
    const parsed = JSON.parse(impactSummary);
    const cnt = parsed?.affectedUserCount;
    return cnt == null ? "-" : `${cnt}명`;
  } catch (e) {
    return "-";
  }
};
</script>

<style scoped>
@import "@/assets/css/modal-popup-guide.css";

.leave-history-modal {
  width: 100%;
  max-width: 900px;
}

.leave-history-header {
  background: rgba(22, 163, 74, 0.08);
}

.leave-history-body {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.lh-state {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  text-align: center;
  padding: 1.5rem 0;
  margin: 0;
}

.lh-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}

.lh-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
  min-width: 720px;
}

.lh-table th,
.lh-table td {
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.625rem;
  text-align: center;
  white-space: nowrap;
}

.lh-table th {
  background: rgba(22, 163, 74, 0.08);
  font-weight: 600;
  color: var(--color-primary-pressed);
}

.lh-table td.lh-reason {
  text-align: left;
  white-space: normal;
  color: var(--color-text);
}

/* 푸터 버튼 */
.btn {
  padding: 0.5rem 1rem;
  border-radius: var(--btn-radius);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.btn-secondary {
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  color: var(--color-text);
}

.btn-secondary:hover {
  background: var(--color-bg);
}
</style>
