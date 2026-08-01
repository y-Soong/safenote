<!--
  SafetyCameraPermissionView.vue — 카메라 사용 불가 폴백 (prafta-app-011 케이스 6)
  - 라이트 헤더 + 경고 아이콘 + 사유별 안내 + [취소]/[설정으로 이동 | 다시 시도].
  - reason prop 으로 실패 사유별 문구/버튼 분기 (2026-08-01 갤럭시 검은 화면 건 후속):
      denied(기본) = 권한 거부 → 설정으로 이동
      busy         = 카메라 점유/개방 실패 → 다시 시도
      error        = 타임아웃 등 그 외 → 다시 시도
  - 부모(QrScanner/AdminSiteOps)가 cancel / open-settings / retry 이벤트를 받아 처리.
-->
<template>
  <div class="cam-perm">
    <!-- 라이트 헤더 -->
    <header class="cp-hd">
      <button type="button" class="cp-hd__close" aria-label="닫기" @click="$emit('cancel')">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-cp-x" />
        </svg>
      </button>
      <h1 class="cp-hd__title">QR 스캔</h1>
      <span class="cp-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="cp-body">
      <div class="cp-ico" aria-hidden="true">
        <svg
          width="32"
          height="32"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="1" y1="1" x2="23" y2="23" />
          <path
            d="M21 21H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3m3-3h6l2 3h4a2 2 0 0 1 2 2v9.34m-7.72-2.06a4 4 0 1 1-5.56-5.56"
          />
        </svg>
      </div>
      <h2 class="cp-title">{{ copy.title }}</h2>
      <p class="cp-desc">{{ copy.desc }}</p>
    </main>

    <!-- 푸터: denied=설정으로 이동 / busy·error=다시 시도 -->
    <footer class="cp-footer">
      <button type="button" class="cp-btn cp-btn--secondary" @click="$emit('cancel')">취소</button>
      <button
        v-if="reason === 'denied'"
        type="button"
        class="cp-btn cp-btn--primary"
        @click="$emit('open-settings')"
      >
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-cp-settings" />
        </svg>
        설정으로 이동
      </button>
      <button v-else type="button" class="cp-btn cp-btn--primary" @click="$emit('retry')">
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-cp-retry" />
        </svg>
        다시 시도
      </button>
    </footer>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="cp-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-cp-x"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
        <symbol
          id="i-cp-settings"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="3" />
          <path
            d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"
          />
        </symbol>
        <symbol
          id="i-cp-retry"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="23 4 23 10 17 10" />
          <polyline points="1 20 1 14 7 14" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 실패 사유(qrCameraStart.js 의 CAMERA_FAIL 어휘와 동일). 미지정 시 기존 동작(denied) 보존.
const props = defineProps({
  reason: {
    type: String,
    default: 'denied',
    validator: (v) => ['denied', 'busy', 'error'].includes(v),
  },
})

defineEmits(['cancel', 'open-settings', 'retry'])

// 사유별 안내 문구
const COPY = {
  denied: {
    title: '카메라 접근 권한이 필요해요',
    desc: 'QR 코드를 인식하려면 카메라 사용 권한을 허용해 주세요. 설정 앱에서 PRAFTA 권한을 켤 수 있어요.',
  },
  busy: {
    title: '카메라를 사용할 수 없어요',
    desc: '다른 앱이 카메라를 사용 중일 수 있어요. 카메라를 쓰는 앱을 닫은 뒤 다시 시도해 주세요.',
  },
  error: {
    title: '카메라를 켜지 못했어요',
    desc: '일시적인 문제일 수 있어요. 잠시 후 다시 시도해 주세요. 계속 안 되면 앱을 완전히 종료했다가 다시 실행해 주세요.',
  },
}
const copy = computed(() => COPY[props.reason] || COPY.denied)
</script>

<style scoped>
.cam-perm {
  /* 디자인 토큰 (scoped) */
  --color-primary: #16a34a;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.cp-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  padding-top: env(safe-area-inset-top);
  border-bottom: 1px solid var(--color-border-light);
}
.cp-hd__close {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.cp-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.cp-hd__spacer {
  width: 44px;
}

/* 본문 */
.cp-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  text-align: center;
  gap: 8px;
}
.cp-ico {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-warning-tint);
  color: var(--color-warning);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.cp-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.cp-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  max-width: 280px;
}

/* 푸터 */
.cp-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  display: flex;
  gap: 8px;
}
.cp-btn {
  flex: 1;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-family: inherit;
}
.cp-btn--primary {
  background: var(--color-primary);
  color: #ffffff;
}
.cp-btn--secondary {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}

.cp-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
