<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal remnant-report-modal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- ============ 헤더 (드래그) ============ -->
        <div class="modal-header" @mousedown="startDrag">
          <span>소멸 임박 짜투리 잔여</span>
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

        <!-- ============ 안내 + 테이블 ============ -->
        <div class="viewBody">
          <!-- 문구 규칙(D9): "근로자 손해" 표현 금지 — "연차미사용수당 정산 대상" 으로 표기 -->
          <p class="rr-note">
            최소 사용단위 미만으로 남아 신청으로 소진할 수 없는 잔여입니다.
            미사용분은 연차미사용수당 정산 대상입니다.
          </p>

          <div class="table-wrapper rr-table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th width="30%">이름</th>
                  <th width="30%">잔여</th>
                  <th width="18%">구분</th>
                  <th>최근접 소멸일</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!rows || rows.length === 0">
                  <tr>
                    <td colspan="4" class="edu-grid-empty">대상 없음</td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="row in rows" :key="row.userCd">
                    <td>{{ row.userNm }}</td>
                    <td>{{ row.remnantText }}</td>
                    <td>
                      <span
                        class="rr-badge"
                        :class="{ 'rr-badge--dust': row.isRoundingDust }"
                      >
                        {{ row.isRoundingDust ? "절사 끝수" : "짜투리" }}
                      </span>
                    </td>
                    <td>{{ row.nearestExpireDate }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <!-- ===== 푸터 버튼 ===== -->
          <div class="btn-group" style="margin-top: 1rem">
            <button class="btn btn-second" type="button" @click="fnClose">
              닫기
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
defineProps({
  // 표시용 가공 완료 행 [{ userCd, userNm, remnantText, isRoundingDust, nearestExpireDate }]
  //   가공은 부모(Attd_09 fnLoadRemnantInfo)가 수행 — 본 팝업은 표시 전용
  rows: { type: Array, default: () => [] },
});
const emit = defineEmits(["close"]);

// ================ Instance & Composables ================
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Methods/Functions ================
// 모달 닫기
const fnClose = () => {
  emit("close");
};
</script>

<style scoped>
.remnant-report-modal {
  width: 100%;
  max-width: 640px;
}

/* ===== 안내 문구 ===== */
.rr-note {
  font-size: 0.8125rem;
  line-height: 1.4;
  color: var(--color-text-muted);
  margin: 0 0 0.75rem 0;
}

/* ===== 테이블 스크롤 바운딩 (행이 많아도 팝업이 화면을 넘지 않게) ===== */
.rr-table-wrapper {
  max-height: 50vh;
  overflow-y: auto;
}

/* ===== 구분 배지 ===== */
.rr-badge {
  display: inline-block;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}

.rr-badge--dust {
  background: var(--color-bg);
  color: var(--color-text-muted);
}
</style>
