<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <!-- 드래그되는 영역 -->
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 1. Title 영역 (여기서만 드래그 가능) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ JSON.parse(props.qrValue).qrTitle }}</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="viewBody qr-popup">
          <div class="popup-content">
            <!-- QR 코드 표시 -->
            <qrcode-vue :value="qrValue" :size="300" />
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnPrintQr">프린트</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
///* eslint-disable */
import { ref } from "vue";
import { defineProps } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import QrcodeVue from "qrcode.vue";

const modalRef = ref(null);

const props = defineProps({
  qrValue: { type: String, required: true },
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

function fnPrintQr() {
  const qrData = JSON.parse(props.qrValue); // ✅ JSON 변환
  const qrTitle = qrData.qrTitle; // ✅ siteCd 추출

  const canvas = document.querySelector("canvas");
  const dataUrl = canvas.toDataURL("image/png");

  const printWindow = window.open("", "_blank", "width=400,height=500");
  printWindow.document.write(`
    <html>
      <head>
        <title>QR Code Print</title>
        <style>
          body {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
          }
          .siteCd {
            font-size: 32px;
            font-weight: bold;
            margin-bottom: 20px;
          }
        </style>
      </head>
      <body>
        <!-- ✅ QR 위에 siteCd 출력 -->
        <div class="siteCd">${qrTitle}</div>
        <img src="${dataUrl}" />
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.print();
}
</script>

<style scoped>
.qr-popup {
  /* position: fixed; */
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}
.popup-content {
  background: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  min-width: 300px;
}
</style>
