<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="modal-overlay" tabindex="0">
        <div class="modal-content">
          <div class="modal-body">
            <p style="white-space: pre-line">{{ message }}</p>
          </div>
          <!-- F-10 규약: 왼쪽=진행/확정(variant 로 primary/danger), 오른쪽=이탈(ghost), 폭 균등 -->
          <div class="modal-footer">
            <button
              class="btn"
              :class="variant === 'danger' ? 'btn-danger' : 'btn-primary'"
              @click="$emit('confirm')"
            >
              확인
            </button>
            <button class="btn btn-ghost" @click="$emit('cancel')">
              취소
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { defineProps } from 'vue'

defineProps({
  message: String,
  visible: {
    type: Boolean,
    default: true,
  },
  // F-10: 파괴적 확인은 'danger' — 미지정 시 기존과 동일한 안전(primary) 취급
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'danger'].includes(v),
  },
})
</script>

<style scoped>
/* 다른 앱 화면들과 동일한 로컬 토큰 재선언 관례(루트 :root 대신 컴포넌트 최상위 클래스에 스코프
   — :root 는 scoped 안에서도 전역으로 새어나가므로 사용 금지, feedback_static_import_unscoped_style_global_leak 참조) */
.modal-overlay {
  --color-primary: #16a34a;
  --color-primary-hover: #15803d;
  --color-danger: #ef4444;
  --color-border: #e5e7eb;
  --color-text: #374151;

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
  background: #ffffff;
  padding: 1.5rem;
  border-radius: 0.5rem;
  width: 90%;
  max-width: 360px;
  text-align: center;
}

.modal-footer {
  margin-top: 1rem;
  display: flex;
  gap: 1rem;
}

.modal-footer .btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
  border: none;
}
.btn-primary:hover {
  background: var(--color-primary-hover);
}

.btn-danger {
  background: var(--color-danger);
  color: #fff;
  border: none;
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text);
}
</style>
