<!--
  AdminTbmMaterialFormView.vue — 관리자 TBM 교육자료 등록/수정 폼
  - 작업 ID: 001-P5-T-F13 (분해: 001-phase5-admin-tbm-plan.md §2-6, §3-J T-A10 등록/수정, §7 T8 업로드)
  - 진입: 자료 리스트 "자료 등록"(신규) / 자료 상세 "수정" → /AdminTbmMaterialForm?mtrlCd=...
          mtrlCd 있으면 수정 모드, 없으면 등록 모드. (라우트 등록은 developer)
  - 백엔드:
      GET  /appApi/admin/tbm/edu-materials/{mtrlCd}     (수정 모드 초기 로드)
      POST /appApi/admin/tbm/edu-materials              (등록, + 항목 파일 업로드 T8)
      PUT  /appApi/admin/tbm/edu-materials/{mtrlCd}     (수정)
      GET  /appApi/admin/tbm/site-options               (스코프 사업장 셀렉트 — 개설폼과 동일 소스)
  - 필드: 제목 / 설명 / 타입(MTRL_TYPE COM003) / 스코프(회사공통 vs 사업장) / 사용여부
          + 항목(items) 다중(타입 SYS018 + 파일/URL + 설명 + 순서) — AdminTbmMaterialItemRow.
  - 업로드(T8): 파일 선택 UI + 파일명 표시 + 항목 로컬 state 까지만. 실제 업로드/저장은 developer.
  - 디자인 토큰: AdminLauncherView/TbmHubView 세트를 .admin-tbm-material-form-view 루트에 1회 선언.
  - C1: 스코프/권한은 서버만 신뢰. 클라이언트 역할 분기 없음.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + v-model + 단순 검증/항목 토글까지.
      ⚠️ API 호출(조회/저장/업로드)/라우팅은 developer(R5) — TODO(developer) 참조.
-->
<template>
  <div class="admin-tbm-material-form-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-mtrlf-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">{{ isEdit ? '교육자료 수정' : '교육자료 등록' }}</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main class="admin-tbm-material-form-body">
      <!-- 수정 모드 로딩 -->
      <p v-if="isLoading" class="admin-tbm-state">불러오는 중…</p>

      <!-- 수정 모드 로드 실패 -->
      <div v-else-if="loadError" class="admin-tbm-state">
        <p class="admin-tbm-state__msg">자료를 불러오지 못했어요.</p>
        <button type="button" class="admin-tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <form v-else class="mtrl-form" @submit.prevent="onSubmit">
        <!-- 제목 -->
        <div class="mtrl-form__field">
          <label class="mtrl-form__label" for="mtrl-title">제목</label>
          <input
            id="mtrl-title"
            v-model.trim="form.title"
            class="mtrl-form__input"
            type="text"
            maxlength="200"
            placeholder="자료 제목을 입력하세요"
          />
        </div>

        <!-- 타입(MTRL_TYPE COM003) -->
        <div class="mtrl-form__field">
          <label class="mtrl-form__label" for="mtrl-type">타입</label>
          <select id="mtrl-type" v-model="form.mtrlType" class="mtrl-form__select">
            <option value="">선택</option>
            <option v-for="t in typeOptions" :key="t.code" :value="t.code">{{ t.name }}</option>
          </select>
        </div>

        <!-- 스코프(회사공통 vs 사업장) -->
        <div class="mtrl-form__field">
          <span class="mtrl-form__label">공개 범위</span>
          <div class="mtrl-form__radios">
            <label class="radio-item" :class="{ 'is-checked': form.scope === 'COMMON' }">
              <input type="radio" name="mtrlScope" value="COMMON" v-model="form.scope" />
              <span>회사 공통</span>
            </label>
            <label class="radio-item" :class="{ 'is-checked': form.scope === 'SITE' }">
              <input type="radio" name="mtrlScope" value="SITE" v-model="form.scope" />
              <span>사업장 전용</span>
            </label>
          </div>
        </div>

        <!-- 사업장 셀렉트(스코프=SITE 시) -->
        <div v-if="form.scope === 'SITE'" class="mtrl-form__field">
          <label class="mtrl-form__label" for="mtrl-site">사업장</label>
          <select id="mtrl-site" v-model="form.siteCd" class="mtrl-form__select">
            <option value="">선택</option>
            <option v-for="opt in siteOptions" :key="opt.siteCd" :value="opt.siteCd">
              {{ opt.siteNm }}
            </option>
          </select>
        </div>

        <!-- 설명(CONTENTS) -->
        <div class="mtrl-form__field">
          <label class="mtrl-form__label" for="mtrl-desc">설명</label>
          <textarea
            id="mtrl-desc"
            v-model="form.contents"
            class="mtrl-form__textarea"
            rows="3"
            maxlength="500"
            placeholder="자료 설명 (선택, 500자 이내)"
          ></textarea>
        </div>

        <!-- 사용여부 -->
        <div class="mtrl-form__field">
          <label class="radio-item" :class="{ 'is-checked': form.useYn === 'Y' }">
            <input type="checkbox" :checked="form.useYn === 'Y'" @change="onToggleUse" />
            <span>이 자료를 사용함</span>
          </label>
        </div>

        <!-- 항목(items) 다중 -->
        <div class="mtrl-form__field">
          <div class="mtrl-form__field-head">
            <span class="mtrl-form__label">자료 항목 ({{ items.length }})</span>
            <button type="button" class="mtrl-form__add" @click="onAddItem">항목 추가</button>
          </div>

          <p v-if="!items.length" class="mtrl-form__empty">
            추가된 항목이 없어요. "항목 추가"로 이미지/동영상/PDF/URL을 등록하세요.
          </p>

          <div v-else class="mtrl-form__items">
            <AdminTbmMaterialItemRow
              v-for="(it, i) in items"
              :key="it._key"
              v-model="items[i]"
              :index="i"
              :is-last="i === items.length - 1"
              @file="onItemFile"
              @remove="onRemoveItem"
              @move-up="onMoveUp"
              @move-down="onMoveDown"
            />
          </div>
        </div>

        <!-- 액션 (F-10 규약: 왼쪽=진행/확정, 오른쪽=이탈) -->
        <div class="mtrl-form__actions">
          <button type="submit" class="btn btn--primary" :disabled="submitting">
            {{ isEdit ? '수정 저장' : '등록' }}
          </button>
          <button type="button" class="btn btn--ghost" :disabled="submitting" @click="onCancel">
            취소
          </button>
        </div>
      </form>
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-mtrlf-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import AdminTbmMaterialItemRow from './components/AdminTbmMaterialItemRow.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 모드/상태 ─────────────────────────────────────────────────────
const mtrlCd = computed(() => route.query.mtrlCd || '')
const isEdit = computed(() => !!mtrlCd.value)

const isLoading = ref(false) // 수정 모드 초기 로드
const loadError = ref(false)
const submitting = ref(false)

// 타입 옵션(MTRL_TYPE = COM003 "TBM교육타입") — 서버 옵션 endpoint(options:[{code,name}]).
const typeOptions = ref([]) // [{ code, name }]

// 사업장 옵션(스코프=SITE) — 개설폼과 동일 소스(site-options).
const siteOptions = ref([])

// ── 폼 상태(developer: 초기값/리셋/수정 로드 매핑 보완) ──────────────
const form = reactive({
  title: '',
  mtrlType: '',
  scope: 'COMMON', // COMMON | SITE (SITE_CD NULL=공통 / 값=사업장)
  siteCd: '',
  contents: '',
  useYn: 'Y',
})

// 항목 로컬 state. _key 는 v-for 안정 키(로컬 전용, 저장 payload 에서 제외).
//   { _key, mtrlItemType, url, fileName, mtrlDesc, sortIdx, file?(선택 원본 — 업로드 시 developer 사용) }
const items = ref([])
let itemSeq = 0
const newItem = () => ({
  _key: `it-${++itemSeq}`,
  mtrlItemType: '01',
  url: '',
  fileName: '',
  mtrlDesc: '',
  sortIdx: items.value.length,
})

// ── 항목 제어(UI 토글 — 허용 범위) ────────────────────────────────
const onAddItem = () => {
  items.value.push(newItem())
}
const onRemoveItem = (i) => {
  items.value.splice(i, 1)
}
const onMoveUp = (i) => {
  if (i <= 0) return
  const arr = items.value
  ;[arr[i - 1], arr[i]] = [arr[i], arr[i - 1]]
}
const onMoveDown = (i) => {
  const arr = items.value
  if (i >= arr.length - 1) return
  ;[arr[i + 1], arr[i]] = [arr[i], arr[i + 1]]
}
// 항목 파일 선택 원본 보관(업로드는 developer). 항목 객체에 file 참조만 부착.
const onItemFile = ({ index, file }) => {
  if (items.value[index]) items.value[index].file = file
}

const onToggleUse = (e) => {
  form.useYn = e.target.checked ? 'Y' : 'N'
}

// ── 검증(클라이언트 1차 — 서버가 최종 권위) ──────────────────────
const validate = () => {
  if (!form.title) {
    showAlert('제목을 입력해 주세요.')
    return false
  }
  if (!form.mtrlType) {
    showAlert('타입을 선택해 주세요.')
    return false
  }
  if (form.scope === 'SITE' && !form.siteCd) {
    showAlert('사업장을 선택해 주세요.')
    return false
  }
  if (!items.value.length) {
    showAlert('자료 항목을 1개 이상 추가해 주세요.')
    return false
  }
  // 항목별 최소 검증: URL형은 url, 파일형은 파일명(신규) 또는 기존 파일코드.
  for (const it of items.value) {
    if (it.mtrlItemType === '03') {
      const u = (it.url || '').trim()
      if (!u) {
        showAlert('URL 항목의 주소를 입력해 주세요.')
        return false
      }
      // http(s) 스킴만 허용(서버 권위 검증과 동일). javascript:/data:/file: 등 차단.
      if (!/^https?:\/\//i.test(u) || u.length > 1000) {
        showAlert('URL은 http:// 또는 https:// 로 시작하는 1000자 이내 주소만 입력할 수 있습니다.')
        return false
      }
    } else if (!it.fileName && !it.fileMgmtCd) {
      showAlert('파일 항목의 파일을 선택해 주세요.')
      return false
    }
  }
  return true
}

// 저장 FormData 구성(멀티파트 A안 — 백엔드 @RequestPart("data") JSON + @RequestPart("files") 파일들).
//   식별자(회사)는 서버가 토큰에서 채운다. 신규 파일은 files 배열에 fileIndex 순서대로 append 하고,
//   해당 항목 JSON 에는 fileIndex 를 기록한다. 기존 파일 유지 항목은 fileMgmtCd 보존(fileIndex=null).
const buildFormData = () => {
  const files = [] // 신규 업로드 파일(File) — fileIndex 순서대로 push
  const items_ = items.value.map((it, i) => {
    const isFileType = it.mtrlItemType !== '03'
    let fileIndex = null
    let fileMgmtCd = isFileType ? it.fileMgmtCd || null : null
    // 파일형이고 신규 파일이 선택된 경우: files 에 push 하고 인덱스 기록(기존 코드는 무시).
    if (isFileType && it.file) {
      fileIndex = files.length
      files.push(it.file)
      fileMgmtCd = null
    }
    return {
      mtrlItemType: it.mtrlItemType,
      url: it.mtrlItemType === '03' ? it.url || null : null,
      mtrlDesc: it.mtrlDesc || null,
      sortIdx: i,
      fileMgmtCd,
      fileIndex,
    }
  })

  const payload = {
    title: form.title,
    mtrlType: form.mtrlType,
    siteCd: form.scope === 'SITE' ? form.siteCd : null, // null = 회사공통
    contents: form.contents || null,
    useYn: form.useYn,
    items: items_,
  }

  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  files.forEach((f) => formData.append('files', f))
  return formData
}

// ── 조회(수정 모드 초기 로드) ──────────────────────────────────────
const loadSiteOptions = async () => {
  try {
    const { data } = await api.get('/appApi/admin/tbm/site-options')
    siteOptions.value = Array.isArray(data?.sites) ? data.sites : []
  } catch (e) {
    console.error('[AdminTbmMaterialFormView] 사업장 옵션 조회 실패:', e?.message)
  }
}

// MTRL_TYPE 코드 옵션 조회 — GET /appApi/admin/tbm/material-type-options (options:[{code,name}]).
const loadTypeOptions = async () => {
  try {
    const { data } = await api.get('/appApi/admin/tbm/material-type-options')
    typeOptions.value = Array.isArray(data?.options) ? data.options : []
  } catch (e) {
    console.error('[AdminTbmMaterialFormView] 타입 옵션 조회 실패:', e?.message)
  }
}

// 수정 모드: 기존 자료 + 항목 로드 → 폼/items 매핑.
const loadDetail = async () => {
  if (!isEdit.value) return
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/edu-materials/${encodeURIComponent(mtrlCd.value)}`,
    )
    const m = data?.material
    if (!m) {
      loadError.value = true
      return
    }
    form.title = m.title || ''
    form.mtrlType = m.mtrlType || ''
    form.scope = m.siteCd ? 'SITE' : 'COMMON'
    form.siteCd = m.siteCd || ''
    form.contents = m.contents || ''
    form.useYn = m.useYn === 'N' ? 'N' : 'Y'
    items.value = (Array.isArray(data?.items) ? data.items : []).map((it) => ({
      _key: `it-${++itemSeq}`,
      mtrlItemType: it.mtrlItemType || '01',
      url: it.url || '',
      fileName: '', // 기존 파일은 코드로만 보유(파일명 표시는 fileMgmtCd 존재로 대체)
      fileMgmtCd: it.fileMgmtCd || null,
      mtrlDesc: it.mtrlDesc || '',
      sortIdx: it.sortIdx,
    }))
  } catch (e) {
    console.error('[AdminTbmMaterialFormView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

const onRetry = () => loadDetail()

// ── 저장 ──────────────────────────────────────────────────────────
const onSubmit = async () => {
  if (submitting.value) return
  if (!validate()) return
  const ok = await askConfirm(isEdit.value ? '수정 내용을 저장할까요?' : '교육자료를 등록할까요?')
  if (!ok) return
  submitting.value = true
  try {
    // 멀티파트(A안): data(JSON Blob) + files(신규 파일). Content-Type 은 axios 가 자동 설정.
    const formData = buildFormData()
    if (isEdit.value) {
      await api.put(
        `/appApi/admin/tbm/edu-materials/${encodeURIComponent(mtrlCd.value)}`,
        formData,
      )
    } else {
      await api.post('/appApi/admin/tbm/edu-materials', formData)
    }
    await showAlert(isEdit.value ? '수정되었어요.' : '등록되었어요.')
    router.back()
  } catch (e) {
    const msg = e?.response?.data?.message || '저장에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    submitting.value = false
  }
}

// 취소(미저장 안내)
const onCancel = async () => {
  const ok = await askConfirm('작성 중인 내용은 저장되지 않아요. 나가시겠어요?')
  if (!ok) return
  router.back()
}

const onBack = () => router.back()

onMounted(async () => {
  await loadTypeOptions()
  await loadSiteOptions()
  await loadDetail()
  // 등록 모드: 기본 항목 1개로 시작(선택). developer 가 정책에 맞게 조정 가능.
  if (!isEdit.value && !items.value.length) items.value.push(newItem())
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView/TbmHubView 세트) — 자식(ItemRow) scoped 가 상속 */
.admin-tbm-material-form-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
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
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.admin-tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-tbm-hd__back {
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
.admin-tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-hd__spacer {
  width: 36px;
}

/* 본문 */
.admin-tbm-material-form-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}

/* 폼 */
.mtrl-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding-bottom: var(--space-lg);
}
.mtrl-form__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.mtrl-form__field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mtrl-form__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.mtrl-form__input,
.mtrl-form__select,
.mtrl-form__textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0 var(--space-md);
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.mtrl-form__textarea {
  height: auto;
  padding: var(--space-md);
  resize: vertical;
  line-height: 1.5;
}
.mtrl-form__input:focus,
.mtrl-form__select:focus,
.mtrl-form__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 라디오/체크 */
.mtrl-form__radios {
  display: flex;
  gap: var(--space-sm);
}
.radio-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  background: var(--color-surface);
}
.radio-item.is-checked {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-weight: 600;
}

/* 항목 추가 버튼 + 리스트 */
.mtrl-form__add {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-full);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.mtrl-form__items {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.mtrl-form__empty {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 액션 */
.mtrl-form__actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
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
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

/* 상태 메시지 */
.admin-tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-state__msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-retry {
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 스프라이트 */
.admin-tbm-sprite {
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
