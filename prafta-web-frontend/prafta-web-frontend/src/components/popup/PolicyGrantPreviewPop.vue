<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal policy-grant-preview-modal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- ============ 헤더 (드래그) ============ -->
        <div class="modal-header" @mousedown="startDrag">
          <span>정책 기준 부여 미리보기</span>
          <button class="icon-button" type="button" @click="fnClose">
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

        <!-- ============ 요약 + 테이블 ============ -->
        <div class="viewBody">
          <!-- ===== PRORATE 폴백 안내 배너 (prafta-022 보완, prorateFallback일 때만) ===== -->
          <div v-if="prorateFallback" class="pg-notice-banner" role="note">
            {{ noticeText }}
          </div>

          <!-- ===== 요약 줄 (prafta-032 D6: 재발급 집계 제거) ===== -->
          <p class="pg-summary">
            선택 <span class="pg-summary-total">{{ selectedCount }}</span
            >명 · 신규부여
            <span class="pg-summary-new">{{ newGrantCount }}</span
            >명 · 변경없음
            <span class="pg-summary-nochange">{{ noChangeCount }}</span
            >명
          </p>

          <!-- ===== 상세 테이블 (prafta-032 D6: 처리방식·취소예정 컬럼 제거) ===== -->
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th width="35%">직원명</th>
                  <th width="20%" class="pg-th-right">추가예정</th>
                  <th>비고</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!rows || rows.length === 0">
                  <tr>
                    <td colspan="3" class="edu-grid-empty">
                      미리볼 대상이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(row, idx) in rows" :key="row.userCd ?? idx">
                    <td>{{ fnUserName(row.userCd) }}</td>
                    <td class="pg-td-right">{{ row.addDays ?? 0 }}일</td>
                    <td class="cell-wrap">{{ row.note ?? "" }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <!-- ===== 푸터 버튼 ===== -->
          <div class="btn-group" style="margin-top: 1rem">
            <button class="btn btn-second" type="button" @click="fnClose">
              취소
            </button>
            <button
              class="btn btn-primary"
              type="button"
              :disabled="isApplying"
              @click="fnApply"
            >
              적용
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ================ Imports ================
import { ref } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

// ================ Props & Emits ================
const props = defineProps({
  // 프리뷰 집계 결과 (prafta-032 D6: 재발급 집계 reissueCount 제거)
  selectedCount: { type: Number, default: 0 },
  newGrantCount: { type: Number, default: 0 },
  noChangeCount: { type: Number, default: 0 },
  // 직원별 처리 상세 [{ userCd, addDays, note }] (prafta-032 D6: handlingType·cancelCount 제거)
  rows: { type: Array, default: () => [] },
  // userCd → { hireDate, userNm } 이름 매핑 (부모 Attd_09 의 userInfoMap)
  userInfoMap: { type: Object, default: () => ({}) },
  // 첫해 방식 PRORATE 폴백 안내 (prafta-022 보완) — true면 상단 배너 표시
  prorateFallback: { type: Boolean, default: false },
  noticeText: { type: String, default: "" },
  // [적용] 확인 콜백 — 실제 적용 API 호출은 부모(Attd_09)가 수행
  //   (useModal이 onClose만 자동 배선하므로, 확인 신호는 onConfirm prop 콜백으로 받는다)
  onConfirm: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Refs (Variables) ================
// 적용 진행 중 중복 클릭 방지 (실제 호출은 부모가 수행, 모달은 신호만)
const isApplying = ref(false);

// ================ Methods/Functions ================
// userCd → 직원명 (userInfoMap 매핑, 없으면 userCd 그대로)
const fnUserName = (userCd) => {
  const info = props.userInfoMap?.[userCd];
  return info?.userNm || userCd || "-";
};

// [적용] — 확인 신호만 부모로 전달 (UI 전용). 부모가 적용 API 호출 후 모달을 닫는다.
const fnApply = () => {
  if (isApplying.value) return;
  isApplying.value = true;
  // 확인 신호는 onConfirm prop 콜백으로만 전달한다.
  // (주의) useModal이 onConfirm prop을 vnode props로 그대로 넘기면 Vue가 이를 'confirm'
  //   이벤트 리스너로도 자동 배선한다. 따라서 emit('confirm')과 props.onConfirm()을 함께
  //   호출하면 부모 콜백(적용 POST)이 두 번 실행되는 중복 부여 버그가 발생한다.
  if (typeof props.onConfirm === "function") {
    props.onConfirm();
  }
};

// 모달 닫기 (취소)
const fnClose = () => {
  emit("close");
};
</script>

<style scoped>
.policy-grant-preview-modal {
  width: 100%;
  max-width: 760px;
}

/* ===== PRORATE 폴백 안내 배너 (prafta-022 보완) ===== */
.pg-notice-banner {
  font-size: 0.8125rem;
  line-height: 1.4;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-text);
  border-radius: var(--btn-radius);
  padding: 0.625rem 0.875rem;
  margin: 0 0 0.75rem 0;
}

/* ===== 요약 줄 (BatchResultPop summary 패턴) ===== */
.pg-summary {
  font-size: 1rem;
  color: var(--color-text-strong);
  margin: 0 0 0.75rem 0;
}

.pg-summary-total {
  font-weight: 700;
  color: var(--color-text-strong);
  font-size: 1.1rem;
}

.pg-summary-new {
  font-weight: 700;
  color: var(--color-primary-pressed);
  font-size: 1.1rem;
}

.pg-summary-nochange {
  font-weight: 700;
  color: var(--color-text-muted);
  font-size: 1.1rem;
}

/* ===== 테이블 우측 정렬 셀 ===== */
.pg-th-right {
  text-align: right;
}

.pg-td-right {
  text-align: right;
}
</style>
