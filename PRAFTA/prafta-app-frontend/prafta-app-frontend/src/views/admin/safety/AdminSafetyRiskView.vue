<!--
  AdminSafetyRiskView.vue — 관리자 모드 위험성평가 (조회 + 상태전환, b안)
  - 작업 ID: prafta-app-025 J1-6 (.claude/requests/app_requests/job_1/J1-6-admin-safety.md §8 화면명세 3)
  - 진입: AdminSafetyView '위험성평가' 카드 → router.push('/AdminSafetyRisk') (보호 라우트)
  - 성격: 웹 risk03 매핑. 상태 필터(SYS011) → 목록 → 상세(개선전 읽기) → 상태전환 액션 시트.
  - 권한: 진입 게이팅은 상위(AdminLauncher) access-context.SAFETY. 본 화면 EP가 사업장 스코프 + 상태 전이를 서버 재강제(C1).
  - 상태전환 MVP(서버 강제, 본 화면 게이팅은 UI 토글일 뿐): 전이표는 작업지시서 §4.5.
      · 001 평가요청 → 002 검토중 (개선예정일 + 임시조치 필수) / → 004 폐기
      · 002 검토중   → 003 검토완료 / → 004 폐기
      · 003/004      → 전환 없음(읽기 전용, 풀 재평가는 웹에서)
      ※ 빈도/강도 재산정 · 개선후 사진 등 풀 재평가는 웹 RiskAssessInfo 위임(모바일 미구현).
  - planner 라운드 스코프: template + style 완성. script 는 선언 + 단순 UI 토글(필터/시트/입력 v-model)만.
  - developer 라운드 스코프(TODO):
      (1) GET /appApi/admin/safety/risk-findings?assessmentStatus= → 목록
      (2) 카드 탭 → GET /appApi/admin/safety/risk-detail (siteCd/processCd/assessmentCd) → 상세
      (3) 상태전환 제출 → POST /appApi/admin/safety/risk-status (targetStatus + revalDate?/revalBeforeDesc?)
          → 성공/409(이미 처리됨)/422(허용 안 됨) 분기 + 목록 갱신
      (4) loading/empty/error 분기, SYS011 코드 라벨 매핑
  - 디자인 토큰: AdminLauncherView 세트를 .admin-safety-risk 루트에 1회 선언. 하드코딩 금지.
-->
<template>
  <div class="admin-safety-risk">
    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="asr-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-asr-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-asr-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-asr-close" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
      </defs>
    </svg>

    <!-- 헤더 -->
    <header class="asr-hd">
      <button type="button" class="asr-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-asr-chev-left" /></svg>
      </button>
      <h1 class="asr-hd__title">위험성평가</h1>
      <span class="asr-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 상태 필터 칩 -->
    <nav class="asr-filter" aria-label="진행상태 필터">
      <button
        v-for="f in statusFilters"
        :key="f.value"
        type="button"
        class="asr-chip"
        :class="{ 'is-active': activeStatus === f.value }"
        @click="onSelectStatus(f.value)"
      >
        {{ f.label }}
      </button>
    </nav>

    <!-- 본문: 평가 목록 -->
    <main class="asr-body">
      <div v-if="isLoading" class="asr-state" aria-live="polite">불러오는 중...</div>

      <div v-else-if="findings.length === 0" class="asr-state" aria-live="polite">
        조건에 맞는 위험성평가가 없어요
      </div>

      <ul v-else class="asr-list">
        <li v-for="f in findings" :key="f.assessmentCd" class="asr-item">
          <button type="button" class="asr-item__btn" @click="onOpenDetail(f)">
            <span class="asr-item__main">
              <span class="asr-item__title">{{ f.processNm }} / {{ f.hazardNm }}</span>
              <span class="asr-item__sub">{{ f.initAssessorNm }} · {{ formatDate(f.initAssessDate) }}</span>
            </span>
            <span class="asr-badge" :class="statusClass(f.assessmentStatus)">
              {{ f.assessmentStatusNm }}
            </span>
            <svg class="icon asr-item__chev" width="18" height="18" aria-hidden="true"><use href="#i-asr-chev-right" /></svg>
          </button>
        </li>
      </ul>
    </main>

    <!-- 상세 + 상태전환 시트 -->
    <div v-if="detailOpen" class="asr-sheet" role="dialog" aria-modal="true" aria-label="위험성평가 상세">
      <div class="asr-sheet__dim" @click="onCloseDetail" />
      <div class="asr-sheet__panel">
        <header class="asr-sheet__hd">
          <h2 class="asr-sheet__title">위험성평가 상세</h2>
          <button type="button" class="asr-sheet__close" aria-label="닫기" @click="onCloseDetail">
            <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-asr-close" /></svg>
          </button>
        </header>

        <div class="asr-sheet__body">
          <div v-if="isDetailLoading" class="asr-state" aria-live="polite">불러오는 중...</div>

          <template v-else-if="detail">
            <!-- 개선 전(읽기) -->
            <section class="asr-section">
              <h3 class="asr-section__title">개선 전</h3>
              <dl class="asr-read">
                <div class="asr-row"><dt>작업명</dt><dd>{{ detail.processNm }}</dd></div>
                <div class="asr-row"><dt>위험성분류</dt><dd>{{ detail.riskTypeNm }}</dd></div>
                <div class="asr-row"><dt>유해요인</dt><dd>{{ detail.hazardNm }}</dd></div>
                <div v-if="detail.initDesc" class="asr-row">
                  <dt>유해요인설명</dt><dd class="asr-row__multiline">{{ detail.initDesc }}</dd>
                </div>
                <div class="asr-row"><dt>평가요청자</dt><dd>{{ detail.initAssessorNm }}</dd></div>
                <div class="asr-row"><dt>평가요청일</dt><dd>{{ formatDate(detail.initAssessDate) }}</dd></div>
                <div class="asr-row">
                  <dt>위험도</dt>
                  <dd>빈도 {{ detail.initLikelihoodScore || '-' }} · 강도 {{ detail.initSeverityScore || '-' }} · {{ detail.initRiskLv || '-' }}</dd>
                </div>
              </dl>
              <div v-if="detail.initFilePath && detail.initFileName" class="asr-photo">
                <img :src="buildFileUrl(detail.initFilePath, detail.initFileName)" :alt="detail.initFileName || '유해요인 사진'" />
              </div>
            </section>

            <!-- 현재 진행상태 -->
            <section class="asr-section">
              <h3 class="asr-section__title">진행상태</h3>
              <span class="asr-badge asr-badge--lg" :class="statusClass(detail.assessmentStatus)">
                {{ detail.assessmentStatusNm }}
              </span>
            </section>

            <!-- 상태전환 액션 (UI 게이팅 — 서버가 최종 강제) -->
            <section v-if="canTransition" class="asr-section">
              <h3 class="asr-section__title">상태 전환</h3>

              <!-- 001 → 002 검토중: 개선예정일 + 임시조치 입력 노출 -->
              <template v-if="detail.assessmentStatus === '001'">
                <div class="asr-field">
                  <label class="asr-field__label">개선예정일자</label>
                  <DateStepperField v-model="revalDate" placeholder="개선예정일자 선택" />
                </div>
                <div class="asr-field">
                  <label class="asr-field__label" for="asr-reval-desc">임시조치 내용</label>
                  <textarea
                    id="asr-reval-desc"
                    v-model="revalBeforeDesc"
                    class="asr-field__textarea"
                    rows="4"
                    placeholder="개선완료 전 임시조치 내용을 입력해 주세요"
                  ></textarea>
                </div>
                <div class="asr-actions">
                  <button type="button" class="asr-btn asr-btn--primary" @click="onTransition('002')">검토중 전환</button>
                  <button type="button" class="asr-btn asr-btn--ghost" @click="onTransition('004')">폐기</button>
                </div>
              </template>

              <!-- 002 → 003 검토완료 / 004 폐기 -->
              <template v-else-if="detail.assessmentStatus === '002'">
                <div class="asr-actions">
                  <button type="button" class="asr-btn asr-btn--primary" @click="onTransition('003')">검토완료</button>
                  <button type="button" class="asr-btn asr-btn--ghost" @click="onTransition('004')">폐기</button>
                </div>
              </template>
            </section>

            <!-- 003/004: 읽기 전용 안내 -->
            <section v-else class="asr-section">
              <p class="asr-hint">추가 개선 평가(빈도·강도 재산정, 개선 후 사진)는 웹에서 진행해 주세요.</p>
            </section>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { buildFileUrl } from '@/utils/fileUrl'
import { formatYmdDisplay } from '@/utils/approvalFormat'
import DateStepperField from '@/components/common/DateStepperField.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통 alert 폴백(앱 전역 $alert 우선).
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 필터(SYS011 라이브 라벨): 001 검토요청 / 002 개선예정 / 003 개선완료 / 004 미처리대상. ──
//   배지 텍스트는 서버 assessmentStatusNm 을 그대로 쓰고, 칩 라벨은 필터 선택용 라이브 라벨로 표기.
const statusFilters = [
  { value: '', label: '전체' },
  { value: '001', label: '검토요청' },
  { value: '002', label: '개선예정' },
  { value: '003', label: '개선완료' },
  { value: '004', label: '미처리대상' },
]
const activeStatus = ref('')

// ── 현재 사업장(현장전환 권위) — access-context.currentSiteCd(폴백 토큰 gv_siteCd). 서버가 멤버십 재검증(C1). ──
const currentSiteCd = ref('')

// ── 목록 상태 ─────────────────────────────────────────────────────────────
const isLoading = ref(false)
const findings = ref([]) // [{ siteCd, processCd, processNm, assessmentCd, assessmentStatus, assessmentStatusNm, hazardNm, riskTypeNm, initAssessorNm, initAssessDate, ... }]

// ── 상세 시트 상태 ────────────────────────────────────────────────────────
const detailOpen = ref(false)
const isDetailLoading = ref(false)
const detail = ref(null) // 단건 상세(목록 result + 개선전 사진 filePath 등)
const isSubmitting = ref(false) // 상태전환 제출 중복 방지

// ── 상태전환 입력(001→002 전용) ───────────────────────────────────────────
const revalDate = ref('') // 'YYYY-MM-DD' (서버 전송 시 '-' 제거 → YYYYMMDD)
const revalBeforeDesc = ref('')

// 전환 버튼 노출 여부(UI 토글 — 001/002 만). 서버가 전이 최종 강제(C1).
const canTransition = computed(() => {
  const s = detail.value?.assessmentStatus
  return s === '001' || s === '002'
})

// ── 보조: 상태 배지 클래스 / 일자 포맷 ───────────────────────────────────
const statusClass = (status) => {
  // SYS011: 001 검토요청 / 002 개선예정 / 003 개선완료 / 004 미처리대상 — 의미색 매핑(토큰).
  if (status === '001') return 'is-requested'
  if (status === '002') return 'is-reviewing'
  if (status === '003') return 'is-done'
  if (status === '004') return 'is-discarded'
  return ''
}
const formatDate = (v) => {
  // 'YYYY-MM-DD HH:mm'(서버 DATE_FORMAT) 또는 YYYYMMDD 혼재 대응.
  // formatYmdDisplay 는 숫자만 추출 후 앞 8자리로 'YYYY.MM.DD' 산출(시각 절삭 동작 유지, D1 점).
  return formatYmdDisplay(v)
}

// ── 현재 사업장 조회(현장전환 반영). 실패 시 토큰 사업장 폴백(서버가 최종 멤버십 재검증). ──
const loadCurrentSite = async () => {
  try {
    const { data } = await api.get('/appApi/admin/access-context')
    currentSiteCd.value = data?.currentSiteCd || sessionStorage.getItem('gv_siteCd') || ''
  } catch (e) {
    console.warn('[AdminSafetyRisk] access-context 조회 실패, 토큰 사업장 폴백:', e?.message)
    currentSiteCd.value = sessionStorage.getItem('gv_siteCd') || ''
  }
}

// ── H3 위험성평가 목록 조회 ─────────────────────────────────────────────────
const loadFindings = async () => {
  isLoading.value = true
  try {
    const params = {}
    if (currentSiteCd.value) params.siteCd = currentSiteCd.value
    if (activeStatus.value) params.assessmentStatus = activeStatus.value
    const { data } = await api.get('/appApi/admin/safety/risk-findings', { params })
    findings.value = Array.isArray(data?.findings) ? data.findings : []
  } catch (e) {
    const message = e?.response?.data?.message
    findings.value = []
    await showAlert(message || '위험성평가 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

// ── 핸들러 ────────────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/AdminSafety')
}

// 상태 필터 선택(UI 토글) → 재조회.
const onSelectStatus = (value) => {
  activeStatus.value = value
  loadFindings()
}

// ── H4 카드 상세 조회(개선전 읽기 + 현재 상태). ───────────────────────────────
const onOpenDetail = async (item) => {
  detailOpen.value = true
  // 입력값 초기화(이전 시트 잔여 방지).
  revalDate.value = ''
  revalBeforeDesc.value = ''
  detail.value = item // 우선 목록 항목 표시(상세 응답 도착 시 교체)
  isDetailLoading.value = true
  try {
    const { data } = await api.get('/appApi/admin/safety/risk-detail', {
      params: {
        siteCd: item.siteCd || currentSiteCd.value || undefined,
        processCd: item.processCd,
        assessmentCd: item.assessmentCd,
      },
    })
    if (data?.detail) detail.value = data.detail
  } catch (e) {
    const message = e?.response?.data?.message
    await showAlert(message || '상세를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    isDetailLoading.value = false
  }
}
const onCloseDetail = () => {
  detailOpen.value = false
  detail.value = null
  revalDate.value = ''
  revalBeforeDesc.value = ''
}

// ── H5 상태전환 제출(서버가 전이/권한/동시성 최종 강제). ────────────────────────
const onTransition = async (targetStatus) => {
  if (isSubmitting.value || !detail.value) return

  // 프론트 1차 검증: 002 전환은 개선예정일 + 임시조치 필수(서버도 재검증).
  if (targetStatus === '002') {
    if (!revalDate.value || !revalBeforeDesc.value.trim()) {
      await showAlert('개선예정일과 임시조치 내용을 입력해 주세요.')
      return
    }
  }

  isSubmitting.value = true
  try {
    await api.post('/appApi/admin/safety/risk-status', {
      siteCd: detail.value.siteCd || currentSiteCd.value,
      processCd: detail.value.processCd,
      assessmentCd: detail.value.assessmentCd,
      targetStatus,
      // 002 전환만 개선예정일/임시조치 전송('-' 제거 → YYYYMMDD).
      revalDate: targetStatus === '002' ? revalDate.value.replace(/-/g, '') : undefined,
      revalBeforeDesc: targetStatus === '002' ? revalBeforeDesc.value : undefined,
    })
    onCloseDetail()
    await showAlert('상태가 변경되었습니다.')
    await loadFindings()
  } catch (e) {
    const code = e?.response?.data?.errorCode
    const message = e?.response?.data?.message
    if (code === 'SAFETY_409_001') {
      // 동시 전환(이미 처리됨) — 안내 후 시트 닫고 목록 갱신.
      onCloseDetail()
      await showAlert(message || '이미 처리되어 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.')
      await loadFindings()
    } else if (code === 'SAFETY_422_001') {
      await showAlert(message || '허용되지 않는 상태 전환입니다.')
    } else {
      await showAlert(message || '상태 변경에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    }
  } finally {
    isSubmitting.value = false
  }
}

// 진입 시: 현재 사업장 확정 → 목록 조회(전체).
onMounted(async () => {
  await loadCurrentSite()
  await loadFindings()
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView 세트) — 자식 scoped 상속, 하드코딩 금지 */
.admin-safety-risk {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-info-tint: #eff6ff;
  --color-info-text: #1d4ed8;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;

  height: 100vh;
  height: 100dvh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.asr-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
}
.asr-hd__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
}
.asr-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.asr-hd__spacer {
  min-width: 44px;
}

/* 상태 필터 칩 */
.asr-filter {
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  overflow-x: auto;
}
.asr-chip {
  flex: 0 0 auto;
  padding: 6px 12px;
  background: var(--color-bg);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.asr-chip.is-active {
  background: var(--color-primary-tint);
  border-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

/* 본문 */
.asr-body {
  flex: 1;
  min-height: 0;
  padding: 12px 16px;
  overflow-y: auto;
}
.asr-state {
  padding: 40px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 목록 */
.asr-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.asr-item__btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.asr-item__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.asr-item__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.asr-item__sub {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.asr-item__chev {
  color: var(--color-text-tertiary);
}

/* 상태 배지 */
.asr-badge {
  flex: 0 0 auto;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  background: var(--color-border-light);
}
.asr-badge--lg {
  font-size: 13px;
  padding: 5px 12px;
}
.asr-badge.is-requested {
  color: var(--color-warning-text);
  background: var(--color-warning-tint);
}
.asr-badge.is-reviewing {
  color: var(--color-info-text);
  background: var(--color-info-tint);
}
.asr-badge.is-done {
  color: var(--color-primary);
  background: var(--color-primary-tint);
}
.asr-badge.is-discarded {
  color: var(--color-text-tertiary);
  background: var(--color-border-light);
}

/* 상세 시트 */
.asr-sheet {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.asr-sheet__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
}
.asr-sheet__panel {
  position: relative;
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}
.asr-sheet__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 0.5px solid var(--color-border);
}
.asr-sheet__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.asr-sheet__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  min-height: 36px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
}
.asr-sheet__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px 24px;
}

/* 상세 섹션 */
.asr-section {
  margin-bottom: 18px;
}
.asr-section__title {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-secondary);
}
.asr-read {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.asr-row {
  display: flex;
  gap: 10px;
}
.asr-row dt {
  flex: 0 0 84px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.asr-row dd {
  flex: 1;
  margin: 0;
  font-size: 13px;
  color: var(--color-text-primary);
}
.asr-row__multiline {
  white-space: pre-wrap;
  word-break: break-word;
}
.asr-photo {
  margin-top: 10px;
}
.asr-photo img {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

/* 입력 필드 */
.asr-field {
  margin-bottom: 12px;
}
.asr-field__label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.asr-field__input,
.asr-field__textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  box-sizing: border-box;
}
.asr-field__textarea {
  resize: vertical;
}

/* 액션 버튼 */
.asr-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.asr-btn {
  flex: 1;
  padding: 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  border: 0.5px solid var(--color-border);
}
.asr-btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border-color: var(--color-primary);
}
.asr-btn--ghost {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border-color: var(--color-border);
}

.asr-hint {
  margin: 0;
  padding: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
.asr-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
