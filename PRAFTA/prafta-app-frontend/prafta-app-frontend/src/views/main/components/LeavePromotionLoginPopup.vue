<!--
  LeavePromotionLoginPopup.vue — 연차 사용촉진 1차 로그인 안내 팝업 (모바일 앱, 신규)
  - 작업 ID: prafta-com-008-A-7 (분해: .claude/requests/common/refs/prafta-com-008/prafta-com-008-A-decomposition.md)
  - UI 명세: UI-app-008-A-1
  - 정책 출처: 작업지시서 §2-1(로그인 시 촉진 화면 + PUSH), attd/08-leave §8.5.2(AXIS7 사용촉진)
  - 참조 패턴: NoticeLoginPopup.vue (딤 + 카드 + 푸터, 디자인토큰 루트 1회 재선언, 본문만 스크롤)
  - planner 라운드: template + scoped style 완성, script 는 props/emits/ref 선언 + TODO 만.
  - developer 라운드:
      · 진행 중 1차 촉진 컨텍스트는 부모(MainView)가 GET /appApi/leavepromo01/active 로 조회해 props 로 전달.
      · "계획 등록" → 부모가 router.push('/LeavePromotionPlan') (본 컴포넌트는 emit 만).
      · "나중에" → 닫기(스누즈 정책 D-A8 확정 시 본문 보완).
-->
<template>
  <div v-if="open && promotion" class="lp-popup" role="dialog" aria-modal="true">
    <div class="lp-popup__dim" aria-hidden="true"></div>

    <div class="lp-popup__card">
      <header class="lp-popup__head">
        <p class="lp-popup__heading">연차 사용촉진</p>
        <button
          type="button"
          class="lp-popup__close"
          aria-label="닫기"
          @click="onLater"
        >
          ✕
        </button>
      </header>

      <div class="lp-popup__body">
        <!-- 만료 임박 안내 -->
        <p class="lp-popup__lead">
          소멸 임박 연차가
          <strong class="lp-popup__num">{{ promotion.remainingDays }}</strong>
          일 남았습니다.
        </p>
        <p class="lp-popup__desc">
          사용 계획을 등록해 주세요. 미등록 잔여분은 회사가 직권 지정할 수 있습니다.
        </p>

        <!-- 보유/만료 요약 -->
        <dl class="lp-popup__meta">
          <div class="lp-popup__meta-row">
            <dt>미지정 잔여</dt>
            <dd>{{ promotion.remainingDays }}일</dd>
          </div>
          <div class="lp-popup__meta-row">
            <dt>사용 기한</dt>
            <dd>{{ formatYmd(promotion.availTo) }}</dd>
          </div>
        </dl>
      </div>

      <!-- F-10 규약: 왼쪽=진행/확정(계획 등록), 오른쪽=이탈(나중에) -->
      <footer class="lp-popup__footer">
        <button
          type="button"
          class="lp-popup__btn lp-popup__btn--primary"
          @click="onRegister"
        >
          계획 등록 ▶
        </button>
        <button
          type="button"
          class="lp-popup__btn lp-popup__btn--ghost"
          @click="onLater"
        >
          나중에
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { getCurrentInstance } from 'vue'

import { formatYmdDisplay } from '@/utils/approvalFormat'

// 공통 alert 폴백(앱 전역 $alert 우선) — developer 가 에러 안내에 사용
const { proxy } = getCurrentInstance() || { proxy: null }
// eslint-disable-next-line no-unused-vars
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const props = defineProps({
  // 팝업 표시 여부 (v-model:open)
  open: {
    type: Boolean,
    default: false,
  },
  // 진행 중 1차 촉진 컨텍스트 (GET /appApi/leavepromo01/active 응답 — 부모가 전달)
  //   { remainingDays, availTo(YYYYMMDD), grantedDays, planFromYmd, planToYmd }
  // 진행 중 촉진이 없으면 null → 팝업 미노출.
  promotion: {
    type: Object,
    default: null,
  },
})

// open(v-model) + 액션 결과를 부모(MainView)로 전달
const emit = defineEmits(['update:open', 'register', 'later'])

// YYYYMMDD → "YYYY.MM.DD" (표시 단일 출처 위임, D1)
const formatYmd = (ymd) => formatYmdDisplay(ymd)

// 계획 등록 → 부모가 /LeavePromotionPlan 라우팅. (router 이동은 developer/부모 영역)
const onRegister = () => {
  emit('register')
  emit('update:open', false)
}

// 나중에/닫기 → 팝업만 닫음.
//   TODO(developer): 스누즈 정책(D-A8) 확정 시 ack-snooze 성 호출 추가.
const onLater = () => {
  emit('later')
  emit('update:open', false)
}
</script>

<style scoped>
/*
 * 디자인 토큰 — NoticeLoginPopup 과 동일하게 팝업 루트에 1회 재선언.
 * (팝업은 오버레이로 렌더되어 상속이 끊기므로 자체 선언 필요)
 */
.lp-popup {
  --color-primary: #16a34a;
  --color-warning-text-deep: #9a3412;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --radius-md: 10px;
  --radius-lg: 14px;

  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

.lp-popup__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.lp-popup__card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 360px;
  max-height: 70vh;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  color: var(--color-text-primary);
}

.lp-popup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 8px;
  flex-shrink: 0;
}
.lp-popup__heading {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.lp-popup__close {
  width: 28px;
  height: 28px;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  font-size: 16px;
  cursor: pointer;
}

.lp-popup__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 16px 12px;
  border-top: 0.5px solid var(--color-border-light);
}
.lp-popup__lead {
  margin: 4px 0 8px;
  font-size: 15px;
  line-height: 1.5;
  color: var(--color-text-primary);
}
.lp-popup__num {
  color: var(--color-primary);
  font-weight: 700;
}
.lp-popup__desc {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.lp-popup__meta {
  margin: 0;
  padding: 12px;
  background: var(--color-border-light);
  border-radius: var(--radius-md);
}
.lp-popup__meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.lp-popup__meta-row + .lp-popup__meta-row {
  margin-top: 6px;
}
.lp-popup__meta-row dt {
  color: var(--color-text-secondary);
}
.lp-popup__meta-row dd {
  margin: 0;
  font-weight: 600;
  color: var(--color-text-primary);
}

.lp-popup__footer {
  flex-shrink: 0;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 0.5px solid var(--color-border-light);
}
.lp-popup__btn {
  flex: 0 0 auto;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
}
.lp-popup__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.lp-popup__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 0.5px solid var(--color-border);
}
</style>
