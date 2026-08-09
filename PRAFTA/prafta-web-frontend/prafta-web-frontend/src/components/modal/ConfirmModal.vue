<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-show="true"
        ref="overlayRef"
        class="modal-overlay prafta-modal-popup prafta-modal-alert-confirm"
        tabindex="-1"
        @keydown.self.stop
      >
        <div class="modal-content">
          <div class="modal-body">
            <p style="white-space: pre-line">{{ message }}</p>
          </div>
          <!-- F-10 규약: 왼쪽=진행/확정(색은 variant 로 안전=primary/파괴=danger), 오른쪽=이탈(ghost),
               폭 균등(컨테이너를 꽉 채우는 게 아니라 두 라벨 중 더 넓은 쪽 기준으로 동일 폭 — equalBtnWidth). -->
          <div class="modal-footer">
            <button
              ref="confirmBtnRef"
              class="btn"
              :class="variant === 'danger' ? 'btn-danger' : 'btn-primary'"
              :style="equalBtnWidth ? { width: equalBtnWidth + 'px' } : null"
              @click="$emit('confirm')"
            >
              확인
            </button>
            <button
              ref="cancelBtnRef"
              class="btn btn-ghost"
              :style="equalBtnWidth ? { width: equalBtnWidth + 'px' } : null"
              @click="$emit('cancel')"
            >
              취소
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { defineProps, defineEmits, ref, onMounted, nextTick } from "vue";

defineProps({
  message: String,
  // F-10: 파괴적 확인(삭제 등)은 'danger' 로 호출 — 미지정 시 기존과 동일한 안전(primary) 취급
  variant: {
    type: String,
    default: "primary",
    validator: (v) => ["primary", "danger"].includes(v),
  },
});

defineEmits(["confirm", "cancel", "close"]);

const overlayRef = ref(null);
const confirmBtnRef = ref(null);
const cancelBtnRef = ref(null);
// F-10 규약(폭 균등)의 실제 적용값 — 두 버튼 중 더 넓은 라벨의 자연폭을 재서 양쪽에 동일하게 적용.
// 최초 렌더는 null(각자 자연폭)이라 컨테이너를 꽉 채우던 이전 문제로 되돌아가지 않는다.
const equalBtnWidth = ref(null);

onMounted(() => {
  nextTick(() => {
    overlayRef.value?.focus();

    const w1 = confirmBtnRef.value?.getBoundingClientRect().width || 0;
    const w2 = cancelBtnRef.value?.getBoundingClientRect().width || 0;
    const max = Math.max(w1, w2);
    if (max > 0) equalBtnWidth.value = Math.ceil(max);
  });
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999; /* 충분히 높은 값 */
}

.modal-content {
  background: white;
  padding: 1.5rem;
  border-radius: 0.5rem;
  width: 90%;
  max-width: 360px;
  text-align: center;
}

.modal-footer {
  margin-top: 1rem;
  display: flex;
  justify-content: center;
  gap: 0.75rem;
}

.btn-danger {
  background: var(--color-danger, #ef4444);
  color: #ffffff;
  border: none;
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
}
</style>
