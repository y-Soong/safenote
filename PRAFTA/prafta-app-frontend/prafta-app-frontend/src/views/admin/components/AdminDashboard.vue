<!--
  AdminDashboard.vue — 관리자 모드 대시보드 위젯 묶음 (prafta-app-025 J1-10 B-5)
  - 작업 지시서: .claude/requests/app_requests/job_1/J1-10-B5-admin-dashboard.md
  - 위치: src/views/admin/components/AdminDashboard.vue
  - AdminLauncherView.vue 의 .dashboard placeholder(91~98행)를 본 컴포넌트로 교체한다.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/admin/dashboard/summary 조회 → 4 위젯 매핑(attendance/patrol/risk/nearMiss)
      (2) 위젯별 available=false → "권한 없음" 표시(C1: 서버 산출만 신뢰, 클라 역할 분기 금지)
      (3) loading/error/재시도 처리(AdminLauncher access-context 와 독립 try/catch — 한쪽 실패 격리)
      (4) 현장 전환 시 재조회(AdminLauncher 에서 site 변경 신호를 props/이벤트로 받거나 watch)
  - ⚠️ C1 원칙: 위젯 표시/권한은 서버 응답(available/카운트)만 신뢰. authCd 기반 클라 분기 금지.
  - 디자인 토큰: 부모(AdminLauncherView .admin-launcher-view)가 1회 선언한 토큰을 scoped 상속.
    본 컴포넌트는 var(--...) 만 사용(하드코딩/!important/비scoped 금지).
-->
<template>
  <section class="dashboard">
    <h2 class="dashboard__title">대시보드</h2>

    <!-- 대시보드 전체 로딩(최초 조회) -->
    <div v-if="isLoading" class="dashboard__state" aria-live="polite">불러오는 중...</div>

    <!-- 대시보드 조회 실패(전체) — 전체 화면을 막지 않고 영역만 -->
    <div v-else-if="loadError" class="dashboard__state dashboard__state--error" aria-live="polite">
      <p class="dashboard__state-text">대시보드를 불러오지 못했습니다</p>
      <button type="button" class="dashboard__retry" @click="onRetry">다시 시도</button>
    </div>

    <!-- 위젯 그리드 -->
    <div v-else class="widget-grid">
      <!-- 1) 근태 정보 (노드 스코프) -->
      <section class="widget">
        <header class="widget__hd">
          <svg class="icon widget__icon" width="18" height="18" aria-hidden="true"><use href="#i-admin-attd" /></svg>
          <h3 class="widget__title">근태 정보</h3>
        </header>
        <div v-if="attendance && attendance.available" class="widget__body">
          <div class="metric">
            <span class="metric__value">{{ attendance.checkedInCnt }}<span class="metric__total"> / {{ attendance.scheduledCnt }}</span></span>
            <span class="metric__label">출근 (실제 / 예정)</span>
          </div>
          <div class="metric">
            <span class="metric__value">{{ attendance.leaveCnt }}<span class="metric__unit">명</span></span>
            <span class="metric__label">당일 연차</span>
          </div>
        </div>
        <p v-else class="widget__denied">권한 없음</p>
      </section>

      <!-- 2) 순회 관리 (사업장 스코프) -->
      <section class="widget">
        <header class="widget__hd">
          <svg class="icon widget__icon" width="18" height="18" aria-hidden="true"><use href="#i-admin-safety" /></svg>
          <h3 class="widget__title">순회 관리</h3>
        </header>
        <div v-if="patrol && patrol.available" class="widget__body">
          <div class="metric">
            <span class="metric__value">{{ patrol.completedCnt }}<span class="metric__total"> / {{ patrol.targetCnt }}</span></span>
            <span class="metric__label">금일 점검 완료 (개소)</span>
          </div>
        </div>
        <p v-else class="widget__denied">권한 없음</p>
      </section>

      <!-- 3) 위험성 평가 (사업장 스코프) -->
      <section class="widget">
        <header class="widget__hd">
          <svg class="icon widget__icon" width="18" height="18" aria-hidden="true"><use href="#i-admin-safety" /></svg>
          <h3 class="widget__title">위험성 평가</h3>
        </header>
        <div v-if="risk && risk.available" class="widget__body">
          <div class="metric">
            <span class="metric__value" :class="{ 'metric__value--alert': risk.pendingCnt > 0 }">
              {{ risk.pendingCnt }}<span class="metric__unit">건</span>
            </span>
            <span class="metric__label">미처리 검토요청</span>
          </div>
        </div>
        <p v-else class="widget__denied">권한 없음</p>
      </section>

      <!-- 4) 아차사고 관리 (사업장 스코프) -->
      <section class="widget">
        <header class="widget__hd">
          <svg class="icon widget__icon" width="18" height="18" aria-hidden="true"><use href="#i-admin-safety" /></svg>
          <h3 class="widget__title">아차사고 관리</h3>
        </header>
        <div v-if="nearMiss && nearMiss.available" class="widget__body">
          <div class="metric">
            <span class="metric__value" :class="{ 'metric__value--alert': nearMiss.newCnt > 0 }">
              {{ nearMiss.newCnt }}<span class="metric__unit">건</span>
            </span>
            <span class="metric__label">신규 접수</span>
          </div>
        </div>
        <p v-else class="widget__denied">권한 없음</p>
      </section>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, watch, getCurrentInstance } from 'vue'

import api from '@/api/axios'

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (AdminLauncherView 패턴 동일)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 현장 전환 시 재조회 트리거용 — AdminLauncherView 가 currentSiteCd 를 prop 으로 전달하면 watch 한다.
//   (developer: prop 채택 시 defineProps + watch 로 재조회 연결. 단순 진입 1회면 onMounted 만으로 충분.)
const props = defineProps({
  siteCd: { type: String, default: '' },
})

// ── 상태 ───────────────────────────────────────────────────────────────────
const isLoading = ref(true)
const loadError = ref(false)

// 위젯 데이터(서버 산출만 신뢰 — C1). 각 블록에 available 포함.
const attendance = ref(null) // { available, checkedInCnt, scheduledCnt, leaveCnt }
const patrol = ref(null) // { available, targetCnt, completedCnt }
const risk = ref(null) // { available, pendingCnt }
const nearMiss = ref(null) // { available, newCnt }

// ── 조회 ───────────────────────────────────────────────────────────────────
// developer: GET /appApi/admin/dashboard/summary 호출 → 4 위젯 매핑.
//   현장전환 대응이 필요하면 props.siteCd 를 params 로 전달(서버가 멤버십 재검증).
const loadSummary = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    // 현장전환 대응: props.siteCd 가 있으면 쿼리로 전달(서버가 멤버십 USE_YN='Y' 재검증).
    const params = props.siteCd ? { siteCd: props.siteCd } : {}
    const { data } = await api.get('/appApi/admin/dashboard/summary', { params })

    // 서버 산출값만 신뢰(available/카운트) — 클라 역할 분기 없음(C1).
    attendance.value = data?.attendance || null
    patrol.value = data?.patrol || null
    risk.value = data?.risk || null
    nearMiss.value = data?.nearMiss || null
  } catch (e) {
    // 401/403 토큰 에러는 axios 인터셉터가 처리. 그 외는 영역 에러로 표시(전체 화면 막지 않음).
    console.warn('[AdminDashboard] summary 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 재시도(에러 상태에서 다시 시도)
const onRetry = () => {
  loadSummary()
}

// 부모(AdminLauncherView) "당겨서 새로고침"에서 호출하는 재조회 트리거.
//   현장 전환(props.siteCd 변경)은 watch 로 자동 재조회되지만, 당겨서 새로고침은 siteCd 가
//   바뀌지 않으므로 watch 가 발화하지 않는다. 이를 위해 명시적 refresh 를 expose 한다.
defineExpose({ refresh: loadSummary })

onMounted(() => {
  loadSummary()
})

// 현장 전환 재조회: 부모(AdminLauncherView)가 currentSiteCd 를 prop 으로 전달하므로,
//   값이 실제로 바뀐 경우에만 재조회한다(최초 빈값→값 세팅 시 중복 호출 방지).
watch(
  () => props.siteCd,
  (next, prev) => {
    if (next && next !== prev) loadSummary()
  },
)
</script>

<style scoped>
/* 대시보드 (AdminLauncherView 기존 .dashboard 톤 계승) */
.dashboard {
  margin-bottom: var(--space-lg);
}
.dashboard__title {
  margin: 0 0 var(--space-sm);
  padding: 0 var(--space-xs);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* 전체 로딩/에러 상태 */
.dashboard__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  min-height: 120px;
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-size: 13px;
}
.dashboard__state--error {
  border-style: solid;
}
.dashboard__state-text {
  margin: 0;
}
.dashboard__retry {
  padding: var(--space-xs) var(--space-md);
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

/* 위젯 2열 그리드 */
.widget-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-md);
}

.widget {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  min-height: 96px;
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.widget__hd {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.widget__icon {
  color: var(--color-primary);
}
.widget__title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.widget__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.widget__denied {
  margin: 0;
  padding: var(--space-sm) 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 지표 */
.metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.metric__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.1;
}
.metric__value--alert {
  color: var(--color-danger);
}
.metric__total {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-tertiary);
}
.metric__unit {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  margin-left: 2px;
}
.metric__label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.icon {
  display: inline-block;
  vertical-align: middle;
}
</style>
