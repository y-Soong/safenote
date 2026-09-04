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
                  <th class="ref-table__no">No</th>
                  <th class="ref-table__chk">
                    <input
                      type="checkbox"
                      :checked="isAllChecked"
                      @change="fnToggleAll($event)"
                    />
                  </th>
                  <th class="ref-table__id">사고번호</th>
                  <th class="ref-table__desc">경위</th>
                  <th class="ref-table__dt">발생일시</th>
                  <th class="ref-table__loc-th">장소</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="list.length === 0">
                  <td colspan="6" class="ref-empty">
                    조건에 맞는 완료 아차사고가 없습니다.
                  </td>
                </tr>
                <tr v-for="(nm, idx) in list" :key="nm.nearMissId">
                  <td class="ref-table__no">{{ idx + 1 }}</td>
                  <td class="ref-table__chk">
                    <input
                      type="checkbox"
                      :value="nm.nearMissId"
                      v-model="checkedIds"
                    />
                  </td>
                  <td class="ref-table__id">{{ nm.nearMissId }}</td>
                  <td class="ref-table__desc" :title="nm.description">
                    {{ nm.description }}
                  </td>
                  <td class="ref-table__dt">{{ nm.occurDtime }}</td>
                  <td class="ref-table__loc" :title="nm.locationDesc">
                    {{ nm.locationDesc }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 푸터 (왼쪽=진행/확정(저장), 오른쪽=이탈(닫기).
             폭은 텍스트 크기만큼, 우측 정렬 — 공용 modal-footer 규약 그대로 사용) -->
        <div class="modal-footer">
          <button class="btn btn-save" @click="fnApplySelection">저장</button>
          <button class="btn btn-cancel" @click="$emit('close')">닫기</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({
  // 평가건 키 (사업장 + 공정 + 평가)
  siteCd: { type: String, default: "" },
  processCd: { type: String, default: "" },
  assessmentCd: { type: String, default: "" },
  // 이미 연결/보류중이라 중복 선택을 막을 nearMissId 목록(후보에서 회색/제외용 — 현재는 미사용 안전값)
  preselectedIds: { type: Array, default: () => [] },
  // 체크 항목 일괄 전달 콜백(즉시 INSERT 폐기, 보류-저장 모델)
  onApply: { type: Function, default: null },
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
// 체크된 nearMissId 목록(보류 상태). 저장 클릭 시 부모로 일괄 emit.
const checkedIds = ref([]);

// 전체 선택 여부(현재 목록 기준)
const isAllChecked = computed(
  () => list.value.length > 0 && checkedIds.value.length === list.value.length
);

// 전체 체크 토글
const fnToggleAll = (evt) => {
  if (evt.target.checked) {
    checkedIds.value = list.value.map((nm) => nm.nearMissId);
  } else {
    checkedIds.value = [];
  }
};

// 공통 키 파라미터
const linkKeys = () => ({
  siteCd: props.siteCd,
  processCd: props.processCd,
  assessmentCd: props.assessmentCd,
});

// 완료(SYS063='300') 아차사고 후보 검색 (같은 사업장 + 미연결 + 검색어)
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
      // 검색 시 현재 목록에 없는 체크 항목 정리
      checkedIds.value = checkedIds.value.filter((id) =>
        list.value.some((nm) => nm.nearMissId === id)
      );
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 체크된 아차사고 객체 배열을 부모로 일괄 전달 후 닫기(DB 반영은 부모 저장 시)
const fnApplySelection = () => {
  const selected = list.value.filter((nm) =>
    checkedIds.value.includes(nm.nearMissId)
  );
  if (typeof props.onApply === "function") {
    props.onApply(selected);
  }
  emit("close");
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
  /* 경위·장소 가독을 위해 50vw 폭(기존 860px 대비 확대). */
  width: 50vw;
  min-width: 720px;
  max-width: 92vw;
  /* 기본 .modal-content 의 20px 패딩 제거 → 헤더/본문/푸터가 박스 끝에 밀착. */
  padding: 0;
}

.ref-search-bar {
  display: flex;
  align-items: center;
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

/* 컬럼 폭 배분: 장소·경위를 넓게. No/체크박스는 좁게. */
.ref-table__no {
  width: 5%;
  text-align: center;
}
.ref-table__chk {
  width: 6%;
  text-align: center;
}
.ref-table__id {
  width: 16%;
}
.ref-table__desc {
  width: 26%;
  white-space: normal;
}
.ref-table__dt {
  width: 15%;
}
.ref-table__loc-th {
  width: 32%;
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

.btn-save {
  background: #16a34a;
  color: #ffffff;
  border: none;
}

.btn-save:hover {
  background: #15803d;
}

.btn-report {
  background: #ffffff;
  color: #16a34a;
  border: 1px solid #16a34a;
}

.btn-report:hover {
  background: rgba(22, 163, 74, 0.06);
}

/* 2026-09-05: 푸터 버튼을 내용 폭 + 우측 정렬로 변경.
   종전에는 `.modal-footer .btn { flex: 1 }` 로 좌우 균등 확장이라 두 버튼이 팝업 가로를 꽉 채웠다.
   이 로컬 규칙을 걷어내면 공용 규약(modal-popup-guide.css `.prafta-modal-popup .modal-footer`
   = justify-content: flex-end + gap 8px)이 그대로 살아나, 버튼이 텍스트 크기(.btn padding 0 11px)
   만큼만 차지하며 오른쪽에 붙는다. 버튼 순서(왼=저장 / 오=닫기)는 그대로 유지. */

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
