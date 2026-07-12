<!--
  TransferNoticeSheet.vue — 소속이동 안내 시트 (모바일 앱, 신규)
  - 작업 ID: PRAFTA-WEB_001-5 (설계: .claude/requests/web_requests/PRAFTA-WEB_001-plan.md §6 UI-003)
  - 정책 출처: 공통 §10(알림 고지) / app-033 게이트 패턴(로그인 후 안내)
  - 백엔드 계약(재사용):
      GET  /appApi/user01/my-transfer-notice
        → { hasNotice, reservation:{ reservationId, moveDate(YYYYMMDD),
             toSiteNm, toNodeNm, defaultSchNm, moveReason, guideMessages[] } }
      POST /appApi/user01/transfer-notice/ack  body { reservationId }
  - 확정 결정: 안내는 advisory — 확인(ack) 실패해도 시트는 닫는다(기능 차단 아님).
  - 표시: 소속이동일 + 이동 사업장명/부서명/기본근무타입명(없으면 줄 생략)/사유 + 안내문구(guideMessages).
  - 참조 패턴: NoticeLoginPopup / LeavePromotionLoginPopup (딤 + 카드, 디자인토큰 루트 1회 재선언).
    단 본 화면은 "안내 시트"이므로 하단에서 올라오는 바텀시트 형태로 구성한다.
  - API/router 호출은 부모(MainView)가 담당. 본 컴포넌트는 표시 + emit('ack') 만.
-->
<template>
  <Transition name="tn-sheet">
    <div v-if="open && notice" class="tn-sheet" role="dialog" aria-modal="true">
      <!-- 딤 배경 (실수 닫힘 방지 — 확인 버튼으로만 닫힘) -->
      <div class="tn-sheet__dim" aria-hidden="true"></div>

      <!-- 바텀 시트 카드 -->
      <div class="tn-sheet__card">
        <header class="tn-sheet__head">
          <span class="tn-sheet__grip" aria-hidden="true"></span>
          <p class="tn-sheet__heading">소속이동 안내</p>
        </header>

        <div class="tn-sheet__body">
          <p class="tn-sheet__lead">회원님은 아래와 같이 소속이동 예정입니다.</p>

          <dl class="tn-sheet__list">
            <div class="tn-sheet__row">
              <dt>이동일</dt>
              <dd>{{ moveDateText }}</dd>
            </div>
            <div class="tn-sheet__row">
              <dt>이동 사업장</dt>
              <dd>{{ notice.toSiteNm }}</dd>
            </div>
            <div class="tn-sheet__row">
              <dt>이동 부서</dt>
              <dd>{{ notice.toNodeNm }}</dd>
            </div>
            <!-- 기본 근무타입은 정규직만 존재 → 값이 있을 때만 노출(없으면 줄 생략) -->
            <div v-if="notice.defaultSchNm" class="tn-sheet__row">
              <dt>기본 근무타입</dt>
              <dd>{{ notice.defaultSchNm }}</dd>
            </div>
            <div class="tn-sheet__row">
              <dt>사유</dt>
              <dd>{{ notice.moveReason }}</dd>
            </div>
          </dl>

          <!-- 안내 문구(서버 guideMessages) — 진행 중 결재 종료 등 -->
          <ul v-if="notice.guideMessages && notice.guideMessages.length" class="tn-sheet__guide">
            <li v-for="(g, idx) in notice.guideMessages" :key="idx">{{ g }}</li>
          </ul>
        </div>

        <footer class="tn-sheet__footer">
          <button
            type="button"
            class="tn-sheet__btn tn-sheet__btn--primary"
            :disabled="acking"
            @click="onAck"
          >
            확인
          </button>
        </footer>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'

import { formatYmdDisplay } from '@/utils/approvalFormat'

const props = defineProps({
  // 시트 표시 여부 (v-model:open)
  open: {
    type: Boolean,
    default: false,
  },
  // 미확인 소속이동 예약 (GET /appApi/user01/my-transfer-notice 의 reservation — 부모가 전달)
  //   { reservationId, moveDate, toSiteNm, toNodeNm, defaultSchNm, moveReason, guideMessages:[] }
  notice: {
    type: Object,
    default: null,
  },
  // ack 처리 중(중복 클릭 방지) — 부모가 제어
  acking: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:open', 'ack'])

// 이동일(YYYYMMDD) → "YYYY.MM.DD" (표시 단일 출처 위임, 앱 날짜포맷 관례)
const moveDateText = computed(() => formatYmdDisplay(props.notice?.moveDate))

// 확인 → 부모가 ack(POST) 수행. 실패해도 부모가 시트를 닫는다(advisory).
const onAck = () => {
  if (props.acking) return
  emit('ack', props.notice?.reservationId)
}
</script>

<style scoped>
/*
 * 디자인 토큰 — NoticeLoginPopup 과 동일하게 시트 루트에 1회 재선언.
 * (시트는 MainView 밖 오버레이로 렌더되어 상속이 끊기므로 자체 선언 필요)
 */
.tn-sheet {
  --color-primary: #16a34a;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-info-tint: #eff6ff;
  --color-info-strong: #1d4ed8;
  --radius-md: 10px;
  --radius-lg: 14px;

  position: fixed;
  inset: 0;
  z-index: 1001;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

.tn-sheet__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

/* 바텀 시트 카드 — 하단에서 올라오는 형태(상단 모서리만 라운드). 본문만 스크롤. */
.tn-sheet__card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  background: var(--color-surface);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  overflow: hidden;
  color: var(--color-text-primary);
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

.tn-sheet__head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 8px;
  flex-shrink: 0;
}
.tn-sheet__grip {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: var(--color-border);
}
.tn-sheet__heading {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

/* 본문 — 이 영역만 스크롤 */
.tn-sheet__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 16px 12px;
  border-top: 0.5px solid var(--color-border-light);
}
.tn-sheet__lead {
  margin: 4px 0 12px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text-primary);
}

.tn-sheet__list {
  margin: 0;
  padding: 12px;
  background: var(--color-border-light);
  border-radius: var(--radius-md);
}
.tn-sheet__row {
  display: flex;
  gap: 12px;
  font-size: 13px;
  line-height: 1.5;
}
.tn-sheet__row + .tn-sheet__row {
  margin-top: 8px;
}
.tn-sheet__row dt {
  width: 84px;
  flex-shrink: 0;
  color: var(--color-text-secondary);
}
.tn-sheet__row dd {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-weight: 600;
  color: var(--color-text-primary);
  word-break: break-word;
}

.tn-sheet__guide {
  margin: 12px 0 0;
  padding: 10px 12px 10px 26px;
  background: var(--color-info-tint);
  border-radius: var(--radius-md);
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-info-strong);
}
.tn-sheet__guide li + li {
  margin-top: 4px;
}

.tn-sheet__footer {
  flex-shrink: 0;
  display: flex;
  padding: 12px 16px 16px;
  border-top: 0.5px solid var(--color-border-light);
}
.tn-sheet__btn {
  flex: 1 1 auto;
  height: 44px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
}
.tn-sheet__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.tn-sheet__btn--primary:disabled {
  opacity: 0.6;
  cursor: default;
}

/* 바텀 시트 등장/퇴장 — 카드는 아래에서 올라오고 딤은 페이드 */
.tn-sheet-enter-active,
.tn-sheet-leave-active {
  transition: opacity 0.2s ease;
}
.tn-sheet-enter-active .tn-sheet__card,
.tn-sheet-leave-active .tn-sheet__card {
  transition: transform 0.25s ease;
}
.tn-sheet-enter-from,
.tn-sheet-leave-to {
  opacity: 0;
}
.tn-sheet-enter-from .tn-sheet__card,
.tn-sheet-leave-to .tn-sheet__card {
  transform: translateY(100%);
}
</style>
