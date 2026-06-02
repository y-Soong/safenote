<!--
  SafetyQrErrorOverlay.vue — 미등록/형식오류 QR 토스트 (prafta-app-011 케이스 7)
  - 카메라 뷰파인더 위 하단 absolute 배치 Danger 토스트.
  - 부모(QrScanner)가 message 를 주입. 닫기/재스캔은 부모의 하단 refresh 버튼이 담당.
-->
<template>
  <div class="qr-err" role="alert">
    <span class="qr-err__ico" aria-hidden="true">
      <svg
        width="20"
        height="20"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
    </span>
    <div class="qr-err__body">
      <p class="qr-err__title">등록되지 않은 QR 코드예요</p>
      <p class="qr-err__sub">{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: '점검 개소의 QR 코드인지 확인 후 다시 스캔해 주세요.',
  },
})
</script>

<style scoped>
.qr-err {
  position: absolute;
  z-index: 3;
  left: 16px;
  right: 16px;
  bottom: calc(100px + env(safe-area-inset-bottom));
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: rgba(239, 68, 68, 0.95);
  color: #ffffff;
  border-radius: 12px;
  padding: 14px 16px;
}
.qr-err__ico {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.qr-err__body {
  flex: 1;
  min-width: 0;
}
.qr-err__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}
.qr-err__sub {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.85);
}
</style>
