<!--
  TbmInProgressView.vue — TBM 교육중(관리자 시작됨 ~ 종료/완료)
  - 작업 ID: PRAFTA-TBM-INPROG (분해: prafta-app-tbm-user-detail-plan.md §4 F6)
  - 진입: 시작전 화면 "시작하기" 상태조회(A5) IN_PROGRESS 확인 후, 또는 교육중 탭 재참여 후.
  - 구성: 교육내용(A6 contentBody) + 자료 슬라이드(TbmMaterialSlider) + 위험성 리스트(A7)
          + 중도퇴실(A9)/교육완료(A5 분기 → 퇴실 서명 시트).
  - 상태 감지: 폴링/푸시 아님. "교육완료" 클릭 시점 GET state(A5) → ENDED/COMPLETED 면 퇴실 서명 시트 open,
    아니면 "아직 관리자가 교육을 종료하지 않았습니다" 토스트(잔류).
  - 중도퇴실: confirm → A9(미이수 NOT_COMPLETED 종료). 비번/서명/사유 없음.
  - 디자인 토큰: TbmEntryView 세트를 .tbm-inprog-view 루트에 1회 선언.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격 + 시트 토글만.
-->
<template>
  <div class="tbm-inprog-view">
    <!-- 헤더 -->
    <header class="tbm-hd">
      <button type="button" class="tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-tbm-chev-left" />
        </svg>
      </button>
      <h1 class="tbm-hd__title">교육중</h1>
      <span class="tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main class="tbm-inprog-body">
      <p v-if="isLoading" class="tbm-state">불러오는 중…</p>

      <template v-else>
        <!-- 세션 제목 -->
        <h2 class="tbm-inprog__title">{{ session.title || 'TBM 세션' }}</h2>

        <!-- 교육 내용(리치 HTML) -->
        <section class="card">
          <p class="card__label">교육 내용</p>
          <!-- contentBody: 관리자 입력 리치 HTML. 교육내용 HTML 은 서버(AppTbm01ServiceImpl, Jsoup Safelist.relaxed)
               에서 저장형 XSS sanitize 후 전달됨. 프론트는 별도 sanitize 미적용(이중 방어 아님, 서버 단일 방어). -->
          <div class="tbm-content" v-html="session.contentBody"></div>
        </section>

        <!-- 교육 자료 슬라이드 -->
        <section class="tbm-section">
          <p class="tbm-section__label">교육 자료</p>
          <TbmMaterialSlider :materials="materials" />
        </section>

        <!-- 위험성평가 리스트 -->
        <section class="card">
          <p class="card__label">위험성평가</p>
          <p v-if="risks.length === 0" class="tbm-state tbm-state--sm">
            연계된 위험성평가가 없어요
          </p>
          <ul v-else class="risk-list">
            <li v-for="(r, i) in risks" :key="i">
              <button type="button" class="risk-list__item" @click="onOpenRisk(r)">
                <span class="risk-list__name">{{ r.displayName || riskFallbackName(r) }}</span>
                <svg class="icon risk-list__chev" width="18" height="18" aria-hidden="true">
                  <use href="#i-tbm-chev-right" />
                </svg>
              </button>
            </li>
          </ul>
        </section>
      </template>
    </main>

    <!-- 하단 액션 -->
    <footer class="tbm-inprog-actions">
      <button type="button" class="btn btn--ghost-danger" @click="onWithdraw">
        중도퇴실
      </button>
      <button
        type="button"
        class="btn btn--primary"
        :disabled="checkingState"
        @click="onComplete"
      >
        교육완료
      </button>
    </footer>

    <!-- 위험성 정보 시트 -->
    <TbmRiskInfoSheet v-model="riskSheetOpen" :risk="selectedRisk" />

    <!-- 퇴실 비번 + 서명 시트 -->
    <TbmExitSignSheet
      v-model="exitSheetOpen"
      :submitting="exitSubmitting"
      :error-msg="exitError"
      @submit="onExitSubmit"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-tbm-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-tbm-chev-right"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'

import TbmMaterialSlider from './components/TbmMaterialSlider.vue'
import TbmRiskInfoSheet from './components/TbmRiskInfoSheet.vue'
import TbmExitSignSheet from './components/TbmExitSignSheet.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선, 없으면 window)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 반응형 상태(developer: 조회 결과로 채움) ──────────────────────
const isLoading = ref(false)

// 세션 콘텐츠: { sessionCd, title, contentBody }
const session = ref({
  sessionCd: '',
  title: '',
  contentBody: '',
})

// 자료 묶음(≤3): 슬라이더가 기대하는 형태 [{ mtrlCd, title, items:[{ type, url, desc, sortIdx }] }]
const materials = ref([])

// 위험성 리스트(SessionRiskItem): [{ displayName, processNm, riskTypeNm, hazardNm, assessmentStatusNm, ... }]
const risks = ref([])

// 시트 토글 + 선택 항목
const riskSheetOpen = ref(false)
const selectedRisk = ref({})
const exitSheetOpen = ref(false)

// 교육완료 클릭 시 상태조회(A5) 진행 가드
const checkingState = ref(false)

// 퇴실(exit) 제출 상태
const exitSubmitting = ref(false)
const exitError = ref('')
// 중도퇴실(withdraw) 제출 가드
const isSubmitting = ref(false)

// displayName 없을 때 표시 폴백(단순 조합 — 값 가공 아님)
const riskFallbackName = (r) => {
  return [r.processNm, r.riskTypeNm, r.hazardNm].filter(Boolean).join(' · ') || '위험성평가'
}

// 교육 콘텐츠/자료 조회 — GET /appApi/tbm/sessions/{sessionCd}/content (A6)
//   백엔드 item 필드(itemDesc/previewUrl/url/sortIdx)를 슬라이더가 기대하는 키(desc/url/sortIdx)로 매핑.
//   previewUrl 은 서버가 발급한 서명 절대 URL(파일형 항목). 파일형은 previewUrl, 외부링크형은 url 사용.
const loadContent = async () => {
  const { data } = await api.get(`/appApi/tbm/sessions/${session.value.sessionCd}/content`)
  session.value.contentBody = data?.contentBody || ''
  const mtrls = Array.isArray(data?.materials) ? data.materials : []
  materials.value = mtrls.map((m) => ({
    mtrlCd: m.mtrlCd,
    title: m.title,
    items: (Array.isArray(m.items) ? m.items : []).map((it) => ({
      type: it.type,
      url: it.previewUrl || it.url || '',
      desc: it.itemDesc || '',
      sortIdx: it.sortIdx,
    })),
  }))
}

// 위험성평가 조회 — GET /appApi/tbm/sessions/{sessionCd}/risks (A7)
const loadRisks = async () => {
  const { data } = await api.get(`/appApi/tbm/sessions/${session.value.sessionCd}/risks`)
  risks.value = Array.isArray(data?.risks) ? data.risks : []
}

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/TbmHub')
}

// 위험성 항목 클릭 → 정보 시트 오픈
const onOpenRisk = (risk) => {
  selectedRisk.value = risk
  riskSheetOpen.value = true
}

// 중도퇴실: confirm → POST /appApi/tbm/sessions/{sessionCd}/withdraw (A9, NOT_COMPLETED)
//           성공(멱등: alreadyProcessed 도 정상 처리) 시 /TbmHub 복귀. 비번/서명/사유 없음.
const onWithdraw = async () => {
  if (isSubmitting.value) return
  const ok = await askConfirm('중도 퇴실하시겠어요? 미이수로 처리돼요.')
  if (!ok) return
  isSubmitting.value = true
  try {
    await api.post(`/appApi/tbm/sessions/${session.value.sessionCd}/withdraw`)
    await showAlert('중도 퇴실 처리되었어요.')
    router.replace('/TbmHub')
  } catch (e) {
    console.error('[TbmInProgress] withdraw 실패:', e?.message)
    showAlert(e?.response?.data?.message || '퇴실 처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isSubmitting.value = false
  }
}

// 교육완료: GET /appApi/tbm/sessions/{sessionCd}/state (A5) → statusCd 분기
//   COMPLETED → exitSheetOpen=true (퇴실 비번+서명)
//   그 외     → '아직 관리자가 교육을 종료하지 않았습니다' 안내($alert, 잔류)
const onComplete = async () => {
  if (checkingState.value) return
  checkingState.value = true
  try {
    const { data } = await api.get(`/appApi/tbm/sessions/${session.value.sessionCd}/state`)
    if (data?.statusCd === 'COMPLETED') {
      exitError.value = ''
      exitSheetOpen.value = true
    } else {
      showAlert('아직 관리자가 교육을 종료하지 않았습니다.')
    }
  } catch (e) {
    console.error('[TbmInProgress] state 조회 실패:', e?.message)
    showAlert('상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    checkingState.value = false
  }
}

// 퇴실 서명 시트 submit → POST /appApi/tbm/exit (multipart: sessionCd, exitPwd, item=서명PNG)
//   TbmEntryView exit 페이로드 흐름 이식. 성공 시 '이수 완료' 안내 후 /TbmHub 복귀.
const onExitSubmit = async ({ exitPwd, signFile }) => {
  if (exitSubmitting.value) return
  exitError.value = ''
  if (!signFile) {
    exitError.value = '서명 이미지를 만들지 못했어요. 다시 시도해 주세요.'
    return
  }

  // FormData 구성(Content-Type 은 axios 가 multipart 로 자동 설정 — 인터셉터가 gv_* append).
  const formData = new FormData()
  formData.append('sessionCd', session.value.sessionCd)
  formData.append('exitPwd', exitPwd)
  formData.append('item', signFile)

  exitSubmitting.value = true
  try {
    await api.post('/appApi/tbm/exit', formData)
    exitSheetOpen.value = false
    await showAlert('정상적으로 교육을 이수했습니다.')
    router.replace('/TbmHub')
  } catch (e) {
    console.error('[TbmInProgress] exit 실패:', e?.message)
    exitError.value = e?.response?.data?.message || '종료하지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    exitSubmitting.value = false
  }
}

// ── 진입 ────────────────────────────────────────────────────────
onMounted(async () => {
  const sessionCd = route.query.sessionCd || ''
  if (!sessionCd) {
    showAlert('세션 정보가 없어 화면을 열 수 없어요.')
    router.replace('/TbmHub')
    return
  }
  session.value = {
    sessionCd,
    title: route.query.title || '',
    contentBody: '',
  }
  isLoading.value = true
  try {
    // content(A6) + risks(A7) 병렬 로드
    await Promise.all([loadContent(), loadRisks()])
  } catch (e) {
    console.error('[TbmInProgress] 교육 정보 조회 실패:', e?.message)
    showAlert('교육 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(TbmEntryView 세트와 동일) */
.tbm-inprog-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
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
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
}

/* 헤더 */
.tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tbm-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.tbm-hd__spacer {
  width: 36px;
}

/* 본문 */
.tbm-inprog-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.tbm-inprog__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.tbm-state--sm {
  margin: var(--space-md) 0;
  font-size: 13px;
}

/* 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__label {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 교육 내용(리치 HTML) */
.tbm-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  word-break: break-word;
}
.tbm-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-md);
}
.tbm-content :deep(p) {
  margin: 0 0 var(--space-sm);
}

/* 섹션 라벨(자료) */
.tbm-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.tbm-section__label {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 위험성 리스트 */
.risk-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.risk-list__item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding: 12px 0;
  border-bottom: 0.5px solid var(--color-border-light);
  background: transparent;
  border-left: 0;
  border-right: 0;
  border-top: 0;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.risk-list li:last-child .risk-list__item {
  border-bottom: 0;
}
.risk-list__name {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-word;
}
.risk-list__chev {
  color: var(--color-text-tertiary);
}

/* 하단 액션 */
.tbm-inprog-actions {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--ghost-danger {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border: 1.5px solid var(--color-danger);
}

/* 스프라이트 */
.tbm-sprite {
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
