<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 (드래그) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>참조 아차사고 조회</span>
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

        <!-- 본문 -->
        <div class="modal-body">
          <!-- 검색바 -->
          <div class="ref-search-bar">
            <input
              v-model="keyword"
              type="text"
              placeholder="경위·장소 검색어"
              @keyup.enter="fnSearch"
            />
            <button class="btn btn-report" @click="fnSearch">검색</button>
          </div>

          <!-- 후보(완료 아차사고) 테이블 -->
          <div class="ref-table-wrap">
            <table class="ref-table">
              <thead>
                <tr>
                  <th>사고번호</th>
                  <th>유형</th>
                  <th>위험도</th>
                  <th>발생일시</th>
                  <th>장소</th>
                  <th class="ref-table__act">연결</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="list.length === 0">
                  <td colspan="6" class="ref-empty">
                    조건에 맞는 완료 아차사고가 없습니다.
                  </td>
                </tr>
                <tr v-for="nm in list" :key="nm.nearMissId">
                  <td>{{ nm.nearMissId }}</td>
                  <td>{{ nm.incidentTypeNm }}</td>
                  <td>{{ nm.potentialSeverityNm }}</td>
                  <td>{{ nm.occurDtime }}</td>
                  <td class="ref-table__loc" :title="nm.locationDesc">
                    {{ nm.locationDesc }}
                  </td>
                  <td class="ref-table__act">
                    <button class="btn btn-report" @click="fnLink(nm)">
                      연결
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="$emit('close')">닫기</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({
  // 평가건 키 (사업장 + 공정 + 평가)
  siteCd: { type: String, default: "" },
  processCd: { type: String, default: "" },
  assessmentCd: { type: String, default: "" },
  // 연결 성공 시 부모(연결된 목록) 갱신 콜백
  onLinked: { type: Function, default: null },
});

const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const keyword = ref("");
const list = ref([]);

// 공통 키 파라미터
const linkKeys = () => ({
  siteCd: props.siteCd,
  processCd: props.processCd,
  assessmentCd: props.assessmentCd,
});

// 완료(SYS063='400') 아차사고 후보 검색 (같은 사업장 + 미연결 + 검색어)
const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/risklink01/available-near-miss", {
      params: {
        ...linkKeys(),
        keyword: keyword.value,
      },
    });

    if (response.status === 200) {
      list.value = response.data?.nearMissList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 연결 → 성공 시 부모 갱신 콜백 + 후보 재검색(연결된 건은 후보에서 제외됨)
const fnLink = async (nm) => {
  try {
    const response = await axios.post(
      "/webApi/risklink01/link",
      {
        ...linkKeys(),
        nearMissId: nm.nearMissId,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      if (typeof props.onLinked === "function") {
        props.onLinked();
      }
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "연결 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

onMounted(() => {
  fnSearch();
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  width: 860px;
  max-width: 92vw;
}

.ref-search-bar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.ref-search-bar input {
  flex: 1 1 auto;
  min-width: 0;
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
}

.ref-table-wrap {
  max-height: 50vh;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}

.ref-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  table-layout: fixed;
}

.ref-table th,
.ref-table td {
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.5rem 0.6rem;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ref-table thead th {
  position: sticky;
  top: 0;
  background: #f3f4f6;
  font-weight: 600;
  z-index: 1;
}

.ref-table__loc {
  white-space: normal;
}

.ref-table__act {
  width: 84px;
  text-align: center;
}

.ref-empty {
  color: #888;
  text-align: center;
  padding: 1.25rem 0;
}

.btn {
  padding: 0.3rem 0.9rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  font-family: "Pretendard", sans-serif;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background 0.2s,
    box-shadow 0.2s;
}

.ref-table__act .btn {
  padding: 0.25rem 0.7rem;
  font-size: 0.8rem;
}

.btn-cancel {
  background: #ffffff;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.btn-cancel:hover {
  background: #f9fafb;
}

.btn-report {
  background: #ffffff;
  color: #16a34a;
  border: 1px solid #16a34a;
}

.btn-report:hover {
  background: rgba(22, 163, 74, 0.06);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
