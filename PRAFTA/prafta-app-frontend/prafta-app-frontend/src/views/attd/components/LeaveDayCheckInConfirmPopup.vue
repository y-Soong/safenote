<!--
  LeaveDayCheckInConfirmPopup.vue — 자발 연차일 출근 확인 안내 팝업 (모바일 앱, 신규)
  - 작업 ID: prafta-com-008-B-6 (분해: .claude/requests/common/refs/prafta-com-008/prafta-com-008-B-decomposition.md)
  - UI 명세: UI-com-008-B-1
  - 정책 출처: prafta-com-008-B-labor-refusal.md §5(자발 연차일 출근 허용 + 안내), §7(메시지 템플릿)
  - 참조 패턴: views/main/components/LeavePromotionLoginPopup.vue
              (딤 + 카드 + 푸터, 오버레이라 디자인토큰 루트 1회 재선언, 본문만 스크롤)
  - planner 라운드: template + scoped style 완성, script 는 props/emits 선언 + TODO 만.
  - developer 라운드:
      · 출근 액션(MainView/출근 시트)에서 그날 leave_use 종일 존재(home-summary isLeaveDay)
        && 촉진 아님이면, 출근 API 호출 전에 본 팝업을 띄운다(@confirm 시에만 실제 check-in 호출).
      · 노무수령거부(촉진+비휴일) 차단은 본 팝업 대상 아님 — 서버 4xx → $alert 로 §7 차단 문구 안내(재시도 불가).
  - 역할 분담: 본 컴포넌트는 "확인/취소" emit 만. check-in API/router/store 연동은 developer.
-->
<template>
  <div v-if="open" class="ldc-popup" role="dialog" aria-modal="true">
    <div class="ldc-popup__dim" aria-hidden="true" @click="onCancel"></div>

    <div class="ldc-popup__card">
      <header class="ldc-popup__head">
        <p class="ldc-popup__heading">연차일 출근 확인</p>
      </header>

      <div class="ldc-popup__body">
        <!-- §7 자발 연차일 안내 문구 (노무사 검토 후 확정 — 임시) -->
        <p class="ldc-popup__lead">
          오늘은 <strong class="ldc-popup__date">{{ formatYmd(workYmd) }}</strong>
          연차일로 등록되어 있습니다.
        </p>
        <p class="ldc-popup__desc">
          지정된 근무 스케줄이 아닌데 출근하시겠습니까?
        </p>

        <!-- 초과근무 불가 안내(자발 연차일 출근은 OT 신청이 차단됨, §5) -->
        <p class="ldc-popup__note">
          연차일에 등록된 근무는 초과근무를 올릴 수 없습니다.
          초과근무가 필요하면 먼저 연차일자를 조정해 주세요.
        </p>
      </div>

      <footer class="ldc-popup__footer">
        <button
          type="button"
          class="ldc-popup__btn ldc-popup__btn--ghost"
          @click="onCancel"
        >
          취소
        </button>
        <button
          type="button"
          class="ldc-popup__btn ldc-popup__btn--primary"
          :disabled="submitting"
          @click="onConfirm"
        >
          출근하기
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  // 팝업 표시 여부 (v-model:open)
  open: {
    type: Boolean,
    default: false,
  },
  // 출근 대상일 (YYYYMMDD) — 부모가 전달
  workYmd: {
    type: String,
    default: '',
  },
  // 출근 API 진행 중 버튼 비활성 — 부모(developer)가 제어
  submitting: {
    type: Boolean,
    default: false,
  },
})

// open(v-model) + 확인/취소 결과를 부모로 전달
const emit = defineEmits(['update:open', 'confirm', 'cancel'])

// YYYYMMDD → "YYYY.MM.DD"
const formatYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return ''
  return `${ymd.slice(0, 4)}.${ymd.slice(4, 6)}.${ymd.slice(6, 8)}`
}

// 출근하기 → 부모가 실제 check-in API 호출.
//   TODO(developer): @confirm 수신 시 POST /appApi/attd01/check-in 호출 + 성공/실패 분기.
//   닫기는 API 결과 확정 후 부모가 update:open=false (낙관적 닫기 금지 — 실패 시 재시도 위해).
const onConfirm = () => {
  emit('confirm')
}

// 취소 → 출근 중단·팝업 닫기.
const onCancel = () => {
  emit('cancel')
  emit('update:open', false)
}
</script>

<style scoped>
/*
 * 디자인 토큰 — LeavePromotionLoginPopup 과 동일하게 팝업 루트에 1회 재선언.
 * (팝업은 오버레이로 렌더되어 상속이 끊기므로 자체 선언 필요)
 */
.ldc-popup {
  --color-primary: #16a34a;
  --color-warning-bg: #fffbeb;
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

.ldc-popup__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.ldc-popup__card {
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

.ldc-popup__head {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 16px 8px;
  flex-shrink: 0;
}
.ldc-popup__heading {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.ldc-popup__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 16px 12px;
  border-top: 0.5px solid var(--color-border-light);
}
.ldc-popup__lead {
  margin: 4px 0 8px;
  font-size: 15px;
  line-height: 1.5;
  color: var(--color-text-primary);
}
.ldc-popup__date {
  color: var(--color-primary);
  font-weight: 700;
}
.ldc-popup__desc {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}
.ldc-popup__note {
  margin: 0;
  padding: 10px 12px;
  background: var(--color-warning-bg);
  border-radius: var(--radius-md);
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-warning-text-deep);
}

.ldc-popup__footer {
  flex-shrink: 0;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 0.5px solid var(--color-border-light);
}
.ldc-popup__btn {
  flex: 0 0 auto;
  height: 38px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
}
.ldc-popup__btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.ldc-popup__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.ldc-popup__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 0.5px solid var(--color-border);
}
</style>
