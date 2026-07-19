<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <!-- 드래그되는 영역 -->
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 1. Title 영역 (여기서만 드래그 가능) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ headerTitle }}</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="viewBody qr-popup">
          <div class="popup-content">
            <!-- QR 코드 표시 -->
            <qrcode-vue :value="qrValue" :size="300" />
            <!-- 코드값 표시: 웹에는 스캐너가 없어 수동 입력(Tbm 입실 팝업의 스캔 입력행 폴백)으로
                 처리할 수 있도록 식별 코드를 함께 노출한다. displayCode 전달 시에만 표시(하위호환). -->
            <div v-if="displayCode" class="code-row">
              <span class="code-value">{{ displayCode }}</span>
              <button class="btn btn-primary code-copy-btn" @click="fnCopyCode">
                {{ copied ? "복사됨" : "복사" }}
              </button>
            </div>
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
import { ref, computed } from "vue";
import { defineProps } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import QrcodeVue from "qrcode.vue";

const modalRef = ref(null);

const props = defineProps({
  qrValue: { type: String, required: true },
  // T1-03: 헤더/프린트 제목. URL 등 비-JSON qrValue 를 인코딩할 때 별도 prop 으로 제목을 받는다.
  //        미지정 시 기존 동작(JSON qrValue 의 qrTitle 파싱)으로 하위 호환.
  title: { type: String, default: "" },
  // QR 아래에 함께 표시할 식별 코드(일용직 userCd 등). 미전달 시 표시하지 않음(하위호환).
  displayCode: { type: String, default: "" },
});

// 코드값 복사 상태(버튼 라벨 피드백용).
const copied = ref(false);
async function fnCopyCode() {
  try {
    await navigator.clipboard.writeText(props.displayCode);
  } catch {
    // clipboard API 불가 환경(비보안 컨텍스트 등) 폴백.
    const ta = document.createElement("textarea");
    ta.value = props.displayCode;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand("copy");
    document.body.removeChild(ta);
  }
  copied.value = true;
  setTimeout(() => (copied.value = false), 1500);
}

// 제목 결정: title prop 우선. 없으면 qrValue 가 JSON 일 때만 qrTitle 파싱(파싱 실패해도 빈 문자열).
const headerTitle = computed(() => {
  if (props.title) return props.title;
  try {
    return JSON.parse(props.qrValue)?.qrTitle ?? "";
  } catch {
    return "";
  }
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

function fnPrintQr() {
  // T1-03: 제목은 headerTitle(컴퓨티드) 사용. URL 등 비-JSON qrValue 에서도 안전.
  const qrTitle = headerTitle.value;

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
          .userCode {
            font-size: 24px;
            font-family: monospace;
            letter-spacing: 1px;
            margin-top: 16px;
          }
        </style>
      </head>
      <body>
        <!-- ✅ QR 위에 siteCd 출력 -->
        <div class="siteCd">${qrTitle}</div>
        <img src="${dataUrl}" />
        ${props.displayCode ? `<div class="userCode">${props.displayCode}</div>` : ""}
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

/* 코드값 표시행: QR 아래 식별 코드 + 복사 버튼 */
.code-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm, 8px);
  margin-top: var(--spacing-md, 12px);
}
.code-value {
  font-family: monospace;
  font-size: var(--font-size-lg, 18px);
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--color-text-primary, #111827);
  user-select: all; /* 클릭 시 전체 선택 — 수동 복사 편의 */
}
.code-copy-btn {
  padding: 0 var(--btn-padding-sm, 10px);
  min-height: var(--btn-height-sm, 26px);
}
</style>
