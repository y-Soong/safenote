<!--
  NearMissReport.vue — 근로자 아차사고 보고 화면 (모바일 앱, 신규)
  - 작업 ID: PRAFTA-app-012-3 (분해: .claude/requests/app_requests/prafta-app-012-plan.md)
  - UI 명세: UI-app-012-1 (.claude/requests/app_requests/prafta-app-012-ui-spec.md)
  - 설계 출처: .claude/context/near-miss-incident-design.md §5-A
  - planner 라운드 스코프: template + style 완성. 단일 사진 첨부 + 코드 라디오/셀렉트 조립.
  - developer 라운드 스코프: 코드 목록 조회, A1 multipart 호출, 성공/에러 라우팅, reset.
  - 디자인 토큰: MainView(.home-view) 세트를 .near-miss-report 루트에 1회 선언.
  - 첨부 패턴: Risk_01.vue 의 오프스크린 file input + 미리보기 + multipart('item') 동일.
-->
<template>
  <div class="near-miss-report">
    <!-- 헤더 -->
    <header class="nmr-hd">
      <button type="button" class="nmr-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-nmr-chev-left" />
        </svg>
      </button>
      <h1 class="nmr-hd__title">아차사고 보고</h1>
      <span class="nmr-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문(스크롤) -->
    <main class="nmr-body">
      <!-- 발생일시 (필수) -->
      <section class="nmr-field">
        <p class="nmr-label">발생일시 <span class="nmr-req" aria-hidden="true">*</span></p>
        <div class="nmr-dtime">
          <DateStepperField v-model="occurDate" placeholder="발생일자" />
          <TimeStepperField v-model="occurTime" placeholder="발생시각" />
        </div>
      </section>

      <!-- 발생장소 (선택) -->
      <section class="nmr-field">
        <p class="nmr-label">발생장소 <span class="nmr-optional">(선택)</span></p>
        <input
          v-model="locationDesc"
          type="text"
          class="nmr-input"
          placeholder="예) 3공정 컨베이어 옆"
          maxlength="200"
        />
      </section>

      <!-- 경위 (필수) -->
      <section class="nmr-field">
        <p class="nmr-label">
          무슨 일이 있었나요? <span class="nmr-req" aria-hidden="true">*</span>
        </p>
        <textarea
          v-model="description"
          class="nmr-textarea"
          rows="5"
          placeholder="목격한 상황을 적어주세요"
          maxlength="500"
        ></textarea>
      </section>

      <!-- 잠재중대성 (SYS062, 선택) -->
      <section class="nmr-field">
        <p class="nmr-label">사고였다면 피해 정도는? <span class="nmr-optional">(선택)</span></p>
        <div class="nmr-radio-group">
          <label
            v-for="opt in severityOptions"
            :key="opt.code"
            class="nmr-radio"
            :class="{ 'nmr-radio--on': potentialSeverityCd === opt.code }"
          >
            <input type="radio" name="severity" :value="opt.code" v-model="potentialSeverityCd" />
            <span>{{ opt.label }}</span>
          </label>
        </div>
      </section>

      <!-- 사진 (단일 첨부, 선택) -->
      <section class="nmr-field">
        <p class="nmr-label">사진 <span class="nmr-optional">(선택)</span></p>
        <div class="nmr-photo" @click="triggerFileInput">
          <div v-if="previewImage" class="nmr-photo__preview">
            <img :src="previewImage.url" alt="첨부 사진 미리보기" />
            <button type="button" class="nmr-photo__remove" @click.stop="removePreview">
              삭제
            </button>
          </div>
          <div v-else class="nmr-photo__empty">
            <svg class="icon" width="28" height="28" aria-hidden="true">
              <use href="#i-nmr-camera" />
            </svg>
            <span>사진 촬영 / 선택</span>
          </div>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="nmr-photo__input"
          @change="onFileSelected"
        />
      </section>

      <!-- 즉시조치 (선택) -->
      <section class="nmr-field">
        <p class="nmr-label">즉시 조치 <span class="nmr-optional">(선택)</span></p>
        <input
          v-model="immediateActionDesc"
          type="text"
          class="nmr-input"
          placeholder="예) 후진경보음 점검 요청"
          maxlength="500"
        />
      </section>
    </main>

    <!-- 푸터 -->
    <footer class="nmr-footer">
      <button type="button" class="nmr-submit" :disabled="isSubmitting" @click="onSubmit">
        {{ isSubmitting ? '보고 중...' : '보고하기' }}
      </button>
    </footer>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="nmr-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-nmr-chev-left"
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
          id="i-nmr-camera"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path
            d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"
          />
          <circle cx="12" cy="13" r="4" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import DateStepperField from '@/components/common/DateStepperField.vue'
import TimeStepperField from '@/components/common/TimeStepperField.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (MainView 패턴 동일)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 코드 옵션 (SYS062 잠재중대성).
//   - 진입 시 /comApi/baseinfo/syst-info-lists 로 SYS062 를 동적 조회한다(JoinUser 패턴).
//   - 조회 실패 시 아래 정적 fallback(코드 시드와 동일) 으로 폼이 동작하도록 유지한다.
//   - 공통 엔드포인트가 그룹마다 '전체'(상세코드 null) 행을 UNION 으로 끼워넣으므로,
//     보고 폼에서는 의미 없는 '전체' 항목을 toOptions 에서 제외한다.
// ───────────────────────────────────────────────────────────
const severityOptions = ref([
  { code: '100', label: '경미' },
  { code: '200', label: '중대' },
  { code: '300', label: '치명' },
])

// SYS062 코드 동적 로딩. 실패해도 정적 fallback 으로 폼은 동작.
const loadCodeOptions = async () => {
  try {
    const res = await api.get('/comApi/baseinfo/syst-info-lists', {
      params: { systCodeList: ['SYS062'] },
    })
    const list = res?.data?.systInfoList || []
    const toOptions = (groupCd) =>
      list
        // 상세코드(systValDCd) 가 null 인 행은 공통 '전체' sentinel → 보고 폼에서는 제외.
        .filter((it) => it.systValCd === groupCd && it.systValDCd != null)
        .sort((a, b) => (a.sortIdx ?? 0) - (b.sortIdx ?? 0))
        .map((it) => ({ code: it.systValDCd, label: it.systValDNm }))

    const severities = toOptions('SYS062')
    if (severities.length) severityOptions.value = severities
  } catch (err) {
    // 코드 조회 실패는 치명적이지 않음 — 정적 fallback 유지(폼 진행 가능).
    console.warn('[NearMissReport] 코드 조회 실패, 정적 옵션 사용:', err?.message)
  }
}

// ───────────────────────────────────────────────────────────
// 입력 상태
// ───────────────────────────────────────────────────────────
const occurDate = ref('') // 'YYYY-MM-DD' (DateStepperField) — 진입 시 오늘 기본값(onMounted)
const occurTime = ref('') // 'HH:MM' 24h (TimeStepperField) — 진입 시 현재시각 기본값(onMounted)
const locationDesc = ref('')
const description = ref('')
const potentialSeverityCd = ref('')
const immediateActionDesc = ref('')

const isSubmitting = ref(false)

// ───────────────────────────────────────────────────────────
// 사진 단일 첨부 (Risk_01 패턴)
// ───────────────────────────────────────────────────────────
const fileInputRef = ref(null)
const previewImage = ref(null) // { file, url }

const triggerFileInput = () => {
  if (fileInputRef.value) fileInputRef.value.click()
}

const onFileSelected = (evt) => {
  const input = evt?.target
  const file = input?.files?.[0]
  if (!file) return
  // 이미지 검증 + 미리보기 (TODO(developer): 용량/형식 제약 보강)
  if (typeof file.type !== 'string' || !file.type.startsWith('image/')) {
    showAlert('이미지 파일만 첨부할 수 있어요.')
    if (input) input.value = ''
    return
  }
  if (previewImage.value?.url) {
    URL.revokeObjectURL(previewImage.value.url)
  }
  previewImage.value = { file, url: URL.createObjectURL(file) }
  if (input) input.value = '' // 같은 파일 재선택 허용
}

const removePreview = () => {
  if (previewImage.value?.url) {
    URL.revokeObjectURL(previewImage.value.url)
  }
  previewImage.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}

// ───────────────────────────────────────────────────────────
// 이벤트
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

const onSubmit = async () => {
  // 골격 허용 최소 validation (필수: 발생일시/경위)
  if (!occurDate.value || !occurTime.value) {
    showAlert('발생일시를 입력해주세요.')
    return
  }
  if (!description.value.trim()) {
    showAlert('무슨 일이 있었는지 적어주세요.')
    return
  }

  if (isSubmitting.value) return
  isSubmitting.value = true

  try {
    const formData = new FormData()
    // 일자('YYYY-MM-DD') + 시각('HH:MM') → 서버 포맷('YYYY-MM-DD HH:mm')
    formData.append('occurDtime', `${occurDate.value} ${occurTime.value}`)
    if (locationDesc.value.trim()) formData.append('locationDesc', locationDesc.value.trim())
    formData.append('description', description.value.trim())
    if (potentialSeverityCd.value) formData.append('potentialSeverityCd', potentialSeverityCd.value)
    if (immediateActionDesc.value.trim()) {
      formData.append('immediateActionDesc', immediateActionDesc.value.trim())
    }
    // 단일 사진(선택) — 파트명 'item' (Risk_01 동일). 식별자(gv_*)는 axios 인터셉터가 자동 주입.
    if (previewImage.value?.file) {
      const fileName = buildFileName('nearmiss', previewImage.value.file.name)
      formData.append('item', previewImage.value.file, fileName)
    }

    await api.post('/appApi/nearmiss/report', formData, { timeout: 60 * 1000 })

    await showAlert('보고했어요. 관리자에게 전달됩니다')
    router.back()
  } catch (err) {
    console.error('[NearMissReport] 보고 실패:', err?.message)
    showAlert(
      err?.response?.data?.message || '보고를 등록하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isSubmitting.value = false
  }
}

// 파일명 생성(ChkLst/Risk_01 동일 방식): prefix_타임스탬프_원본명(안전화).
const buildFileName = (prefix, originalName = 'photo.jpg') => {
  const ts = new Date().toISOString().replace(/[:.]/g, '')
  const safe = String(originalName).replace(/[^\w.-]+/g, '_')
  return `${prefix}_${ts}_${safe}`
}

// ───────────────────────────────────────────────────────────
// 라이프사이클
// ───────────────────────────────────────────────────────────
onMounted(() => {
  // 발생일시 기본값 = 오늘 일자 + 현재시각(분까지). 로컬 타임존 기준.
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  occurDate.value = `${yyyy}-${mm}-${dd}`
  occurTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  loadCodeOptions()
})

onBeforeUnmount(() => {
  if (previewImage.value?.url) {
    URL.revokeObjectURL(previewImage.value.url)
  }
})
</script>

<style scoped>
.near-miss-report {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
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
.nmr-hd {
  /* min-height(고정 height 아님) + max(safe-area, 12px):
     앱 webview 는 Flutter SafeArea 로 이미 status bar 아래로 inset 되어
     env(safe-area-inset-top) 이 0 으로 잡힌다. 그 경우 헤더가 상단에 바짝 붙어
     제목이 잘려 보이므로 최소 12px 의 상단 여백을 보장한다. */
  min-height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
  padding-top: max(env(safe-area-inset-top), 12px);
}
.nmr-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-family: inherit;
}
.nmr-hd__title {
  margin: 0;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
}
.nmr-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.nmr-body {
  flex: 1;
  padding: 12px 16px 24px;
  overflow-y: auto;
}

.nmr-field {
  margin-bottom: 18px;
}
.nmr-label {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.nmr-req {
  color: var(--color-danger);
  margin-left: 2px;
}
.nmr-optional {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-tertiary);
}

/* 라디오 그룹 */
.nmr-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.nmr-radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  background: var(--color-surface);
}
.nmr-radio input {
  accent-color: var(--color-primary);
}
.nmr-radio--on {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-tint);
  font-weight: 600;
}

/* 입력 */
.nmr-input,
.nmr-select,
.nmr-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  font-family: inherit;
  box-sizing: border-box;
}
.nmr-textarea {
  resize: vertical;
  line-height: 1.5;
}
/* 발생일시 — 일자/시각 2열 */
.nmr-dtime {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 8px;
}
.nmr-input:focus,
.nmr-select:focus,
.nmr-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 사진 */
.nmr-photo {
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}
.nmr-photo__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}
.nmr-photo__preview {
  position: relative;
  width: 100%;
}
.nmr-photo__preview img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}
.nmr-photo__remove {
  position: absolute;
  top: 8px;
  right: 8px;
  border: 0;
  border-radius: var(--radius-sm);
  padding: 4px 10px;
  font-size: 12px;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.6);
  cursor: pointer;
  font-family: inherit;
}
.nmr-photo__input {
  position: absolute;
  left: -9999px;
  width: 1px;
  height: 1px;
  opacity: 0;
}

/* 푸터 */
.nmr-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
}
.nmr-submit {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.nmr-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.nmr-sprite {
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
