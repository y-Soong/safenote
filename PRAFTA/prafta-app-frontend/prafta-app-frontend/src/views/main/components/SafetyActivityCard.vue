<!--
  SafetyActivityCard.vue — 안전 활동 카드 (안전점검 시작 + 위험성 발굴 + 아차사고)
  - 상세 요청서 §3.4, §4.4
  - 근무시간 게이트 정책 (PRAFTA-022): 근무 중(WORKING)에만 안전점검·위험성 발굴 사용 가능 — 차단 배너 노출
  - 매핑 (작업 요청서 본문 명시):
      "안전점검 시작" → 기존 fnDayChkLst() 라우팅 (/QrScanner) 보존 — 근무중에만 허용
      "위험성 발굴"   → 기존 fnRisk_01()    라우팅 (/Risk_01)  보존 — 근무중에만 허용
      "아차사고 보고" → prafta-app-012 보고 화면 (/NearMissReport) — 즉시성 예외(게이트 미적용, 항상 활성)
      "사건 관리"     → prafta-app-012 목록 화면 (/NearMissManageList) — 안전직군에게만 노출
    실제 router.push 는 부모(MainView)에서 emit 받아 처리한다.
  - 레이아웃: 네모난 버튼 그리드 대신 한 줄(row) 리스트로 표현.
-->
<template>
  <div class="card">
    <div class="head-row">
      <p class="card-title">안전 활동</p>
      <button
        type="button"
        class="head-chev"
        aria-label="자세히 보기"
        @click="$emit('click:detail')"
      >
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>
    </div>

    <!-- 차단 배너 (근무중 아님 — 출근 전·퇴근 후) -->
    <div v-if="blocked" class="blocked-banner">
      <svg class="icon" width="14" height="14" aria-hidden="true">
        <use href="#i-lock" />
      </svg>
      <span>근무 중에만 이용할 수 있어요</span>
    </div>

    <!-- 액션 리스트 (한 줄씩) -->
    <div class="action-list">
      <!-- 안전점검 시작 (근무중에만 허용) -->
      <button
        type="button"
        class="action-row"
        :class="{ 'action-row--disabled': blocked }"
        :disabled="blocked"
        @click="onSafetyCheck"
      >
        <svg class="icon row-icon" width="20" height="20" aria-hidden="true">
          <use href="#i-clipboard" />
        </svg>
        <span class="row-label">안전점검 시작</span>
        <svg class="icon row-chev" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>

      <!-- 위험성 발굴 (근무중에만 허용) -->
      <button
        type="button"
        class="action-row"
        :class="{ 'action-row--disabled': blocked }"
        :disabled="blocked"
        @click="onRiskDiscovery"
      >
        <svg class="icon row-icon" width="20" height="20" aria-hidden="true">
          <use href="#i-camera" />
        </svg>
        <span class="row-label">위험성 발굴</span>
        <svg class="icon row-chev" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>

      <!-- 아차사고 보고 (근로자) — 즉시성 예외: 근무중 게이트 미적용(항상 활성) — PRAFTA-022 -->
      <button
        type="button"
        class="action-row"
        @click="onNearMissReport"
      >
        <svg class="icon row-icon" width="20" height="20" aria-hidden="true">
          <use href="#i-camera" />
        </svg>
        <span class="row-label">아차사고 보고</span>
        <svg class="icon row-chev" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>

      <!-- 사건 관리 (관리자/안전직군) — 사업장 권한 최종 판정은 서버 -->
      <button v-if="isSafetyManager" type="button" class="action-row" @click="onNearMissManage">
        <svg class="icon row-icon" width="20" height="20" aria-hidden="true">
          <use href="#i-clipboard" />
        </svg>
        <span class="row-label">사건 관리</span>
        <svg class="icon row-chev" width="18" height="18" aria-hidden="true">
          <use href="#i-chev" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  // true(근무중 아님) 면 차단 배너 노출 + 안전점검·위험성 발굴 row disabled (근무중에만 허용 — PRAFTA-022).
  // 아차사고 보고는 즉시성 예외로 게이트 미적용(항상 활성).
  blocked: {
    type: Boolean,
    default: false,
  },
  // 안전직군 여부 — "사건 관리" row 노출 게이팅 (서버가 최종 판정)
  isSafetyManager: {
    type: Boolean,
    default: false,
  },
})

// kebab-case 로 통일 — Vue 3 자동 정규화 의존 금지 (부모 @click:safety-check 매칭)
const emit = defineEmits([
  'click:detail',
  'click:safety-check',
  'click:risk-discovery',
  'click:near-miss-report',
  'click:near-miss-manage',
])

// "안전점검 시작" → 기존 fnDayChkLst() → router.push('/QrScanner')
// 부모(MainView)가 emit 받아서 라우팅 처리.
const onSafetyCheck = () => {
  if (props.blocked) return
  emit('click:safety-check')
}

// "위험성 발굴" → 기존 fnRisk_01() → router.push('/Risk_01')
const onRiskDiscovery = () => {
  if (props.blocked) return
  emit('click:risk-discovery')
}

// "아차사고 보고" → /NearMissReport (즉시성 예외: 근무중 게이트 미적용 — 항상 보고 가능)
const onNearMissReport = () => {
  emit('click:near-miss-report')
}

// "사건 관리" → /NearMissManageList (안전직군 노출)
const onNearMissManage = () => {
  emit('click:near-miss-manage')
}
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 0.5px solid var(--color-border);
  padding: 16px;
  margin-bottom: 12px;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.head-chev {
  background: transparent;
  border: 0;
  padding: 4px;
  min-width: 44px;
  min-height: 44px;
  margin: -10px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-family: inherit;
}

.blocked-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: var(--color-warning-tint);
  border-radius: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--color-warning-text-strong);
}

/* 액션 리스트 — 한 줄씩 표현 */
.action-list {
  display: flex;
  flex-direction: column;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 2px;
  background: transparent;
  border: 0;
  border-top: 0.5px solid var(--color-border-light);
  cursor: pointer;
  font-family: inherit;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
  text-align: left;
}

.action-row:first-child {
  border-top: 0;
}

.action-row .row-label {
  flex: 1;
}

.action-row .row-icon {
  color: var(--color-primary);
}

.action-row .row-chev {
  color: var(--color-text-tertiary);
}

.action-row--disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.action-row--disabled .row-icon {
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
