<template>
  <Teleport to="body">
    <Transition name="fade">
      <div class="modal-overlay" tabindex="0">
        <div class="modal-content">
          <div class="modal-body">
            <p style="white-space: pre-line">{{ message }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-primary" @click="$emit('confirm')">확인</button>
            <button class="btn btn-primary" @click="$emit('cancel')">취소</button>
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
})
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
  gap: 1rem;
}

/* .btn.btn-primary {
  background-color: #3b82f6;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  margin: 0.5rem;
  border-radius: 4px;
  cursor: pointer;
}
.btn.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
} */
</style>
