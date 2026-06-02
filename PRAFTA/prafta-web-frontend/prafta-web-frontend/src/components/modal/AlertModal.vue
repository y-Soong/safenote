<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="internalVisible"
        class="modal-overlay prafta-modal-popup prafta-modal-alert-confirm"
        tabindex="0"
        ref="overlayRef"
        @keydown.stop
      >
        <div class="modal-content">
          <div class="modal-body">
            <p style="white-space: pre-line">{{ message }}</p>
          </div>
          <div class="modal-footer">
            <button
              class="btn btn-primary"
              ref="okBtnFcs"
              @click="handleConfirm"
            >
              확인
            </button>
            <!-- <button
              class="btn btn-primary"
              @click="handleConfirm"
              :disabled="clickLocked"
            >
              확인
            </button> -->
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits, nextTick, onMounted } from "vue";
// onMounted,
const props = defineProps({
  visible: Boolean,
  message: {
    type: String,
    default: "알림 내용이 없습니다.",
  },
});

const emit = defineEmits(["close", "confirm"]);

const internalVisible = ref(props.visible);
const overlayRef = ref(null);
const clickLocked = ref(false); // 🔐 중복 클릭 방지용
const okBtnFcs = ref(null);

// 전역 중복 방지 상태 설정 (optional)
window.__alertOpen__ = true;

// props로 받은 visible 변경 감지
watch(
  () => props.visible,
  (val) => {
    internalVisible.value = val;
    if (val) {
      clickLocked.value = false; // 재활성화
      window.__alertOpen__ = true;
      nextTick(() => okBtnFcs.value?.focus());
    } else {
      window.__alertOpen__ = false;
    }
  }
);

const handleConfirm = () => {
  if (clickLocked.value) return; // 이중 실행 방지
  clickLocked.value = true;

  emit("confirm");
  emit("close");

  // 약간의 지연 후 unlock 처리 (안전망)
  setTimeout(() => {
    clickLocked.value = false;
    window.__alertOpen__ = false;
  }, 500);
};

onMounted(() => {
  okBtnFcs.value?.focus();
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
}

/* .btn.btn-primary {
  background-color: #3b82f6;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 4px;
  cursor: pointer;
}
.btn.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
} */
</style>
