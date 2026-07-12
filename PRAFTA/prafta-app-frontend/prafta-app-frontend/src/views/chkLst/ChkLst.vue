<!--
  ChkLst.vue — 안전점검 응답 화면 (prafta-app-011 화면 B, 전면 리디자인)
  - QR(route.query.qr) 파싱 → 체크포인트/항목 조회(GET /appApi/chkLst01/checklist-infos)
  - 항목별 양호/불량 응답 + 불량 사유/사진 입력 → 일괄 저장(POST /appApi/chkLst01/save-inspect-result, multipart)
  - 저장 계약(FormData: workDate/cmpnyCd/userCd/siteCd/chkptCd/items(JSON Blob)/files[itemCd]) 보존.
  - 응답 컨텍스트(checkpoint: chkptNm/siteNm/chklstType)가 있으면 사용, 없으면 graceful 폴백.
  - 저장 성공 시 화면 C(/SafetyInspectSaved)로 이동.
  - 색/간격은 본 화면 루트(.chk-view)의 scoped 디자인 토큰만 사용. 자식은 상속.
-->
<template>
  <div class="chk-view">
    <!-- 헤더 -->
    <header class="chk-hd">
      <button type="button" class="chk-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-chk-chev-left" />
        </svg>
      </button>
      <h1 class="chk-hd__title">안전점검</h1>
      <span class="chk-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="chk-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="chk-loading" aria-live="polite">불러오는 중...</div>

      <!-- 조회 실패 (체크포인트 없음/권한 등) -->
      <div v-else-if="loadFailed" class="chk-empty">
        <p class="chk-empty__title">점검 정보를 불러오지 못했어요</p>
        <p class="chk-empty__sub">{{ loadFailedMessage }}</p>
      </div>

      <template v-else>
        <!-- 컨텍스트 카드 -->
        <SafetyInspectContextCard
          :chkpt-name="checkpoint.chkptName"
          :chklst-type-name="checkpoint.chklstTypeName"
          :site-name="checkpoint.siteName"
          :work-date="workDate"
        />

        <!-- 진행 카운터 -->
        <SafetyInspectProgress
          :answered="answeredCount"
          :total="items.length"
          :ok-count="okCount"
          :bad-count="badCount"
        />

        <!-- 점검 항목 -->
        <SafetyInspectItem
          v-for="(item, idx) in items"
          :key="item.inspectItemCd"
          :item="item"
          :index="idx + 1"
          @update:answer="(v) => onAnswer(idx, v)"
          @update:reason="(v) => (item.answerDesc = v)"
          @update:photo="(v) => (item.photo = v)"
        />
      </template>
    </main>

    <!-- 푸터 -->
    <footer v-if="!isLoading && !loadFailed" class="chk-footer">
      <button
        type="button"
        class="chk-save"
        :class="{ 'chk-save--off': !canSave }"
        :disabled="!canSave || isSaving"
        @click="onSave"
      >
        <svg v-if="canSave" class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-chk-save" />
        </svg>
        {{ saveButtonText }}
      </button>
    </footer>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="chk-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-chk-chev-left"
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
          id="i-chk-save"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
          <polyline points="17 21 17 13 7 13 7 21" />
          <polyline points="7 3 7 8 15 8" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/api/axios'
import { revokePreview } from '@/utils/imagePicker'

import SafetyInspectContextCard from './components/SafetyInspectContextCard.vue'
import SafetyInspectProgress from './components/SafetyInspectProgress.vue'
import SafetyInspectItem from './components/SafetyInspectItem.vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통 alert (전역 $alert 우선, 없으면 window.alert)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
// 공통 confirm (전역 $confirm: boolean Promise)
const showConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)
const isSaving = ref(false)
const loadFailed = ref(false)
const loadFailedMessage = ref('다시 스캔해 주세요.')

const siteCd = ref('')
const chkptCd = ref('')
const workDate = ref('')

// 항목 리스트 (각: { inspectItemCd, inspectItemSubj, answerType, answerDesc, photo })
const items = ref([])

// 체크포인트 컨텍스트 (응답 checkpoint 우선, 없으면 폴백)
const checkpoint = reactive({
  chkptName: '',
  chklstTypeName: '',
  siteName: '',
})

// ───────────────────────────────────────────────────────────
// QR 파싱 (siteCd/chkptCd 추출)
// ───────────────────────────────────────────────────────────
const parseQr = (raw) => {
  if (!raw) return {}
  let s = Array.isArray(raw) ? raw[0] : raw
  if (typeof s === 'object') return s
  s = String(s).trim()
  try {
    const o = JSON.parse(s)
    if (o && (o.siteCd || o.chkptCd)) return o
  } catch {
    /* noop */
  }
  try {
    const o = JSON.parse(decodeURIComponent(s))
    if (o && (o.siteCd || o.chkptCd)) return o
  } catch {
    /* noop */
  }
  if (s.includes('=')) {
    const o = {}
    s.split('&').forEach((p) => {
      const [k, v] = p.split('=')
      if (k) o[decodeURIComponent(k)] = decodeURIComponent(v || '')
    })
    if (o.siteCd || o.chkptCd) return o
  }
  if (s.includes('|')) {
    const [a, b] = s.split('|')
    if (a && b) return { siteCd: a.trim(), chkptCd: b.trim() }
  }
  return {}
}

// 오늘(YYYYMMDD) — 반드시 기기 로컬시간 기준.
//   $util.getToday() 는 toISOString() 기반이라 UTC 날짜를 돌려준다(KST 00~09시엔 전날).
//   여기서는 화면 표시용 일자이므로 로컬 기준으로 직접 계산한다.
//   실제 저장되는 WORK_DATE 는 서버가 KST 기준으로 재결정한다(InspectResultSaveParam).
const getTodayYmd = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}`
}

// ───────────────────────────────────────────────────────────
// 파생값
// ───────────────────────────────────────────────────────────
// 항목 응답 완료 여부: 양호이거나, 불량+사유 1자 이상
const isItemAnswered = (item) => {
  if (item.answerType === 'Y') return true
  if (item.answerType === 'N') return (item.answerDesc || '').trim().length > 0
  return false
}

const answeredCount = computed(() => items.value.filter(isItemAnswered).length)
const okCount = computed(() => items.value.filter((i) => i.answerType === 'Y').length)
const badCount = computed(() => items.value.filter((i) => i.answerType === 'N').length)

// 입력된 응답이 1건이라도 있는지 (이탈 모달 판단)
const hasAnyInput = computed(() =>
  items.value.some((i) => i.answerType === 'Y' || i.answerType === 'N'),
)

// 저장 활성: 전 항목 응답 + 모든 불량 사유 충족
const canSave = computed(() => items.value.length > 0 && items.value.every(isItemAnswered))

// 미선택(answerType == null) 항목 수 — "남음" 카운터 기준 (브리프 §3.6)
const unselectedCount = computed(() => items.value.filter((i) => i.answerType === null).length)

const saveButtonText = computed(() => {
  if (isSaving.value) return '저장 중...'
  if (canSave.value) return '저장'
  // 미선택 항목이 있을 때만 "N개 남음" 표시; 전원 선택했으나 불량 사유 미입력은 단순 "저장"
  if (unselectedCount.value > 0) return `저장 (${unselectedCount.value}개 남음)`
  return '저장'
})

// ───────────────────────────────────────────────────────────
// 항목 응답 변경
// ───────────────────────────────────────────────────────────
const onAnswer = (idx, type) => {
  const item = items.value[idx]
  if (!item) return
  if (type === 'N') {
    // 양호 → 불량 전환: 사유 초기화 (§3.3)
    item.answerType = 'N'
    item.answerDesc = ''
  } else {
    // 불량 → 양호 전환: 사유/사진 폐기 (§3.3)
    if (item.photo?.previewUrl) revokePreview(item.photo.previewUrl)
    item.answerType = 'Y'
    item.answerDesc = ''
    item.photo = null
  }
}

// ───────────────────────────────────────────────────────────
// 컨텍스트 폴백 구성
//   1순위: 응답 checkpoint 객체(chkptNm/siteNm/chklstType)
//   2순위: checklistInfos[0] row 에 동봉된 chkptNm/siteNm
//   3순위: QR/세션 값
// ───────────────────────────────────────────────────────────
const applyCheckpoint = (data, firstRow) => {
  const cp = data?.checkpoint || {}
  checkpoint.chkptName =
    cp.chkptNm || cp.chkptName || firstRow?.chkptNm || firstRow?.chkptName || ''
  checkpoint.siteName =
    cp.siteNm ||
    cp.siteName ||
    firstRow?.siteNm ||
    firstRow?.siteName ||
    sessionStorage.getItem('gv_siteNm') ||
    ''
  // 타입명 마스터 부재 → 응답에 typeName 이 있을 때만 표시(없으면 생략)
  checkpoint.chklstTypeName = cp.chklstTypeName || cp.chklstTypeNm || firstRow?.chklstTypeName || ''
}

// ───────────────────────────────────────────────────────────
// 조회
// ───────────────────────────────────────────────────────────
const fetchChecklist = async () => {
  try {
    const res = await api.get('/appApi/chkLst01/checklist-infos', {
      params: { siteCd: siteCd.value, chkptCd: chkptCd.value },
      validateStatus: () => true,
    })

    if (res.status >= 200 && res.status < 300) {
      const data = res.data || {}
      const list = Array.isArray(data) ? data : data.checklistInfos || []

      if (!list.length) {
        loadFailed.value = true
        loadFailedMessage.value = '등록된 점검 항목이 없어요. 관리자에게 문의해 주세요.'
        return
      }

      items.value = list.map((x) => ({
        inspectItemCd: x.inspectItemCd,
        inspectItemSubj: x.inspectItemSubj,
        answerType: null, // 신규 화면은 미답이 기본 (하드코딩 inspectValue 무시)
        answerDesc: '',
        photo: null,
      }))

      applyCheckpoint(data, list[0])
    } else if (res.status === 403) {
      // 타 사업장 QR 차단 (SITE_MISMATCH)
      loadFailed.value = true
      const siteNm = res.data?.userSiteName || sessionStorage.getItem('gv_siteNm') || ''
      loadFailedMessage.value = siteNm
        ? `소속 사업장(${siteNm})의 점검 개소가 아니에요. 다시 스캔해 주세요.`
        : '소속 사업장의 점검 개소가 아니에요. 다시 스캔해 주세요.'
    } else if (res.status === 404) {
      // 미등록 체크포인트 (CHKPT_NOT_FOUND)
      loadFailed.value = true
      loadFailedMessage.value = '등록되지 않은 QR 코드예요. 다시 스캔해 주세요.'
    } else {
      loadFailed.value = true
      loadFailedMessage.value =
        res.data?.message || res.data?.error || '잠시 후 다시 시도해 주세요.'
    }
  } catch (e) {
    console.error('[ChkLst] 점검 항목 조회 실패:', e?.message)
    loadFailed.value = true
    loadFailedMessage.value = '네트워크 오류가 발생했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    isLoading.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 저장 (기존 multipart 계약 유지)
// ───────────────────────────────────────────────────────────
const buildFileName = (itemCd, originalName = 'photo.jpg') => {
  const ts = new Date().toISOString().replace(/[:.]/g, '')
  const safe = String(originalName).replace(/[^\w.-]+/g, '_')
  return `${itemCd}_${ts}_${safe}`
}

const onSave = async () => {
  if (!canSave.value || isSaving.value) return
  isSaving.value = true
  try {
    // items 페이로드 (기존 계약 키 유지: itemCd / inspectValue / answerDesc / fileName)
    const payloadItems = items.value.map((row) => ({
      itemCd: row.inspectItemCd,
      inspectValue: row.answerType,
      answerDesc: row.answerType === 'N' ? row.answerDesc || '' : '',
      fileName: row.photo?.file?.name || null,
    }))

    const formData = new FormData()
    formData.append('workDate', workDate.value)
    formData.append('cmpnyCd', sessionStorage.getItem('gv_cmpnyCd') || '')
    formData.append('userCd', sessionStorage.getItem('gv_userCd') || '')
    formData.append('siteCd', siteCd.value || '')
    formData.append('chkptCd', chkptCd.value || '')
    formData.append('items', new Blob([JSON.stringify(payloadItems)], { type: 'application/json' }))

    // 불량 사진: files[itemCd] (기존 계약). 양호 항목은 첨부 없음.
    items.value.forEach((row) => {
      const file = row.photo?.file
      if (row.answerType === 'N' && file) {
        formData.append(
          `files[${row.inspectItemCd}]`,
          file,
          buildFileName(row.inspectItemCd, file.name),
        )
      }
    })

    const res = await api.post('/appApi/chkLst01/save-inspect-result', formData, {
      timeout: 60 * 1000,
      validateStatus: () => true,
    })

    if (res.status >= 200 && res.status < 300) {
      // 저장 완료 요약: 응답값 우선, 없으면 메모리값 폴백
      const data = res.data || {}
      const summary = {
        chkptName: data.chkptName || checkpoint.chkptName || '',
        okCount: Number.isFinite(data.okCount) ? data.okCount : okCount.value,
        badCount: Number.isFinite(data.badCount) ? data.badCount : badCount.value,
      }
      // 메모리 미리보기 URL 정리
      cleanupPreviews()
      saved.value = true // 이탈 모달 우회 플래그
      router.replace({
        path: '/SafetyInspectSaved',
        query: {
          chkptName: summary.chkptName,
          okCount: String(summary.okCount),
          badCount: String(summary.badCount),
        },
      })
    } else {
      const msg = res.data?.message || res.data?.error || `HTTP ${res.status}`
      await showAlert(`저장에 실패했어요. (${msg})`)
    }
  } catch (e) {
    console.error('[ChkLst] 저장 실패:', e?.message)
    await showAlert('저장 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isSaving.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 이탈 처리
// ───────────────────────────────────────────────────────────
const saved = ref(false)

const onBack = async () => {
  if (hasAnyInput.value && !saved.value) {
    const ok = await showConfirm('입력 중인 점검 응답이 사라져요. 계속할까요?')
    if (!ok) return
  }
  cleanupPreviews()
  router.push('/MainView')
}

const cleanupPreviews = () => {
  items.value.forEach((i) => {
    if (i.photo?.previewUrl) revokePreview(i.photo.previewUrl)
  })
}

// ───────────────────────────────────────────────────────────
// 라이프사이클
// ───────────────────────────────────────────────────────────
// siteCd/chkptCd 형식 검증 정규식 (브리프 §수정항목2)
const QR_CD_PATTERN = /^[A-Za-z0-9_-]{1,50}$/

onMounted(() => {
  const parsed = parseQr(route.query.qr)
  siteCd.value = parsed.siteCd || ''
  chkptCd.value = parsed.chkptCd || ''
  workDate.value = getTodayYmd()

  if (!siteCd.value || !chkptCd.value) {
    isLoading.value = false
    loadFailed.value = true
    loadFailedMessage.value = 'QR 코드를 인식하지 못했어요. 다시 스캔해 주세요.'
    return
  }

  // 형식 검증: 허용 패턴 미충족 시 loadFailed 경로
  if (!QR_CD_PATTERN.test(siteCd.value) || !QR_CD_PATTERN.test(chkptCd.value)) {
    isLoading.value = false
    loadFailed.value = true
    loadFailedMessage.value = 'QR 코드를 인식하지 못했어요. 다시 스캔해 주세요.'
    return
  }

  fetchChecklist()
})

onBeforeUnmount(() => {
  cleanupPreviews()
})
</script>

<style scoped>
.chk-view {
  /* 디자인 토큰 (시안 토큰 → scoped 변수). 자식 컴포넌트는 var(--...) 상속. */
  --color-primary: #16a34a;
  --color-primary-deep: #15803d;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 — 앱 전역 표준(risk-hd / nmr-hd)과 동형. 클래스 prefix는 chk-hd 유지. */
.chk-hd {
  min-height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  padding: 0 16px;
  padding-top: max(env(safe-area-inset-top), 12px);
  border-bottom: 0.5px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 10;
}
.chk-hd__back {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.chk-hd__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}
.chk-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.chk-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px 88px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chk-loading,
.chk-empty {
  padding: 48px 16px;
  text-align: center;
}
.chk-loading {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.chk-empty__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.chk-empty__sub {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

/* 푸터 */
.chk-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
}
.chk-save {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--color-primary);
  color: #ffffff;
  border: 0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.chk-save--off {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
  border: 1px solid var(--color-border);
  cursor: not-allowed;
}

.chk-sprite {
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
