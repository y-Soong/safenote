<!--
  AdminBoardFormView.vue — 관리자 안전자료실(Archive) 등록 폼 [prafta-app-025 J1-8]
  - 진입: /AdminBoardForm (AdminBoardView 헤더 "등록").
  - 동작: 자료타입(필수·"전체" 없음)/제목(필수)/내용/비밀번호(필수)/비번확인/첨부(다건 업로드) → 저장 → 목록 복귀.
  - 백엔드:
      GET  /appApi/notice02/archive-types                     (B1 자료타입 드롭다운, COM008 USE_YN='Y')
      POST /appApi/notice02/upload-file  (multipart 'file')   (B4 첨부 선업로드 → { fileMgmtCd, fileNm })
      POST /appApi/notice02/save-archive { archiveTypeCd, title, content, editPwd, fileList:[{fileMgmtCd,sortIdx}] }  (B5 등록)
  - C1: 등록 권한(master/hr/safe)·자료타입 필수·비번 BCrypt·NOTICE_TYPE='ARCHIVE'/TARGET_SCOPE='ALL' 등은 서버가 최종 강제.
        권한 없으면 서버 403(ARCHIVE_403_002). 자료타입 빈값/제목/비번 누락은 프론트+서버 이중 차단.
        비번 확인(editPwdConfirm)은 프론트 UX 전용(서버는 단일 editPwd 만 수신).
  - 참조 패턴: 웹 views/notice/popup/ArchiveCreatePop.vue(폼 구성/검증/첨부 선업로드) → 모바일 단일 컬럼 변환.
  - PII: 자료 본문에 PII 없음. 첨부 확장자 화이트리스트는 서버(FileService)가 최종.
  - planner 라운드 범위: template + scoped style 완성, script 는 import/ref 선언 + UI(파일 선택 트리거/비번 일치 computed)만.
  - developer 라운드 범위(TODO):
      (1) GET archive-types → archiveTypeOptions ("전체" 없음, placeholder만)
      (2) onSave: 프론트 검증 → 파일 B4 선업로드(순서=sortIdx) → B5 save → 성공 $alert + /AdminBoard 복귀
      (3) 403(권한)/400(검증·확장자) 에러코드별 안내
      (4) 취소/뒤로 → router 복귀
  - 디자인 토큰: AdminLauncherView 동형 inline scoped 토큰(루트 1회 선언). 하드코딩/!important 금지.
-->
<template>
  <div class="board-form">
    <!-- 헤더 -->
    <header class="bf-hd">
      <button type="button" class="bf-hd__back" aria-label="뒤로" @click="onCancel">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-bf-chev-left" />
        </svg>
      </button>
      <h1 class="bf-hd__title">자료 등록</h1>
      <span class="bf-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 폼) -->
    <main class="bf-body">
      <!-- 자료타입 (필수, "전체" 없음 — placeholder만) -->
      <div class="bf-field">
        <label class="bf-label" for="bf-type">자료타입 <span class="bf-req">*</span></label>
        <select id="bf-type" v-model="form.archiveTypeCd" class="bf-select">
          <!-- "전체"(빈값) 옵션 없음. 안내 placeholder 만 두되 disabled+hidden 으로 빈값 재선택을 막는다
               (미선택 저장은 onSave + 서버 save-archive 가 이중 차단). -->
          <option value="" disabled hidden>자료타입을 선택해 주세요</option>
          <option v-for="t in archiveTypeOptions" :key="t.archiveTypeCd" :value="t.archiveTypeCd">
            {{ t.archiveTypeNm }}
          </option>
        </select>
      </div>

      <!-- 제목 (필수) -->
      <div class="bf-field">
        <label class="bf-label" for="bf-title">제목 <span class="bf-req">*</span></label>
        <input
          id="bf-title"
          v-model="form.title"
          type="text"
          class="bf-input"
          maxlength="200"
          placeholder="제목을 입력해 주세요"
        />
      </div>

      <!-- 내용 -->
      <div class="bf-field">
        <label class="bf-label" for="bf-content">내용</label>
        <textarea
          id="bf-content"
          v-model="form.content"
          class="bf-textarea"
          rows="6"
          placeholder="내용을 입력해 주세요"
        ></textarea>
      </div>

      <hr class="bf-divider" />

      <!-- 비밀번호 (필수, 저장 시 BCrypt — master 포함 필수) -->
      <div class="bf-field">
        <label class="bf-label" for="bf-pwd">비밀번호 <span class="bf-req">*</span></label>
        <input
          id="bf-pwd"
          v-model="form.editPwd"
          type="password"
          class="bf-input"
          maxlength="50"
          placeholder="수정 비밀번호 (저장 시 암호화)"
        />
      </div>
      <div class="bf-field">
        <label class="bf-label" for="bf-pwd2">비밀번호 확인 <span class="bf-req">*</span></label>
        <input
          id="bf-pwd2"
          v-model="form.editPwdConfirm"
          type="password"
          class="bf-input"
          maxlength="50"
          placeholder="비밀번호를 다시 입력해 주세요"
        />
        <p v-if="form.editPwdConfirm" class="bf-pwd-msg" :class="pwdMatched ? 'is-ok' : 'is-error'">
          {{ pwdMatched ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.' }}
        </p>
      </div>

      <hr class="bf-divider" />

      <!-- 첨부 (다건) -->
      <div class="bf-field">
        <label class="bf-label">첨부파일</label>
        <input
          ref="fileInputRef"
          type="file"
          multiple
          accept="image/*,video/*,audio/*,text/*,.csv,.tsv,.log,.md,.json,.xml,.rtf"
          class="bf-file-hidden"
          @change="onFileChange"
        />
        <button type="button" class="bf-file-btn" @click="triggerFileSelect">파일 선택</button>
        <ul v-if="files.length > 0" class="bf-file-list">
          <li v-for="(f, i) in files" :key="i" class="bf-file-item">
            <span class="bf-file-item__name">{{ f.name }}</span>
            <button type="button" class="bf-file-item__del" aria-label="삭제" @click="removeFile(i)">×</button>
          </li>
        </ul>
        <p class="bf-hint">
          텍스트/이미지/동영상/음성 파일만 첨부할 수 있습니다(실행/스크립트 형식 제외). 저장 시 함께 업로드됩니다.
        </p>
      </div>
    </main>

    <!-- 하단 액션 -->
    <footer class="bf-footer">
      <button type="button" class="bf-btn bf-btn--ghost" @click="onCancel">취소</button>
      <button type="button" class="bf-btn bf-btn--primary" :disabled="saving" @click="onSave">
        {{ saving ? '저장 중...' : '저장' }}
      </button>
    </footer>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="bf-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-bf-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 ─────────────────────────────────────────────────────────────
// 폼 (대상지정/팝업/고정 없음 — 자료실은 회사 전체 공통, 서버가 강제값 고정)
const form = reactive({
  archiveTypeCd: '',
  title: '',
  content: '',
  editPwd: '',
  editPwdConfirm: '',
})

// 선택한 첨부 파일(File 객체 배열 — 저장 시 B4 선업로드)
const files = ref([])
const fileInputRef = ref(null)

// 저장/업로드 진행 중 플래그(중복 제출 방지)
const saving = ref(false)

// 자료타입 드롭다운 옵션 (B1 응답 — [{ archiveTypeCd, archiveTypeNm }])
const archiveTypeOptions = ref([])

// 비밀번호/비밀번호 확인 일치 여부 (오타 방지 즉시 피드백 — 프론트 UX 전용)
const pwdMatched = computed(() => !!form.editPwd && form.editPwd === form.editPwdConfirm)

// ── 첨부 UI(바인딩만) ──────────────────────────────────────────────────
const triggerFileSelect = () => {
  fileInputRef.value?.click()
}
const onFileChange = (e) => {
  const picked = Array.from(e.target.files || [])
  files.value = [...files.value, ...picked]
  // 동일 파일 재선택 시에도 change 가 발생하도록 input 값 초기화
  if (e.target) e.target.value = ''
}
const removeFile = (i) => {
  files.value.splice(i, 1)
}

// ── 핸들러 ────────────────────────────────────────────────────────────
const onCancel = () => {
  if (window.history.length > 1) router.back()
  else router.push('/AdminBoard')
}

// 저장 — 프론트 검증 → 첨부 B4 선업로드(순서=sortIdx) → B5 save → 성공 복귀.
//   서버가 등록 권한(master/hr/safe)·자료타입 유효성·비번 BCrypt·강제값을 최종 처리(C1).
const onSave = async () => {
  // 프론트 필수값 검증(서버도 재검증)
  if (!form.archiveTypeCd) return showAlert('자료타입을 선택해 주세요.')
  if (!form.title) return showAlert('제목을 입력해 주세요.')
  if (!form.editPwd) return showAlert('비밀번호를 입력해 주세요.')
  if (!form.editPwdConfirm) return showAlert('비밀번호 확인을 입력해 주세요.')
  if (form.editPwd !== form.editPwdConfirm) return showAlert('비밀번호가 일치하지 않습니다.')

  if (saving.value) return
  saving.value = true
  try {
    // 1) 첨부 선업로드: files 선택 순서를 sortIdx(1-base)로 사용. 각 파일 multipart 업로드 → fileMgmtCd 수집.
    //    확장자 화이트리스트/path 방어/등록 권한(403)은 서버 upload-file 이 최종 검증.
    const filePayload = []
    for (let i = 0; i < files.value.length; i++) {
      const fd = new FormData()
      fd.append('file', files.value[i])
      const { data } = await api.post('/appApi/notice02/upload-file', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      if (data?.fileMgmtCd) filePayload.push({ fileMgmtCd: data.fileMgmtCd, sortIdx: i + 1 })
    }

    // 2) 등록: NOTICE_TYPE='ARCHIVE'/POPUP_YN='N'/PIN_YN='N'/TARGET_SCOPE='ALL'/채번/비번 BCrypt 는 서버 강제.
    //    등록 권한(master/hr/safe)은 서버가 최종 판정(C1) — 권한 없으면 403(ARCHIVE_403_002).
    await api.post(
      '/appApi/notice02/save-archive',
      {
        archiveTypeCd: form.archiveTypeCd,
        title: form.title,
        content: form.content,
        editPwd: form.editPwd,
        fileList: filePayload,
      },
      { headers: { 'Content-Type': 'application/json' } },
    )

    // 3) 성공 → 안내 후 목록으로 복귀(등록 폼은 히스토리에 남기지 않도록 replace).
    await showAlert('저장되었습니다.')
    router.replace('/AdminBoard')
  } catch (e) {
    // 403(ARCHIVE_403_002 권한없음) / 400(검증·확장자 차단) 등 메시지 노출
    await showAlert(e?.response?.data?.message || '저장 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    saving.value = false
  }
}

// ── 자료타입 조회 — GET /appApi/notice02/archive-types ──────────────────
//   COM008(USE_YN='Y') 목록. 빈 목록이면 선택 가능한 자료타입이 없으므로 저장 시 서버가 차단(ARCHIVE_400_002).
const loadTypes = async () => {
  try {
    const { data } = await api.get('/appApi/notice02/archive-types')
    archiveTypeOptions.value = Array.isArray(data?.typeList)
      ? data.typeList.map((t) => ({ archiveTypeCd: t.archiveTypeCd, archiveTypeNm: t.archiveTypeNm }))
      : []
  } catch (e) {
    console.warn('[AdminBoardFormView] archive-types 조회 실패:', e?.message)
    archiveTypeOptions.value = []
    await showAlert('자료타입을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

onMounted(() => {
  loadTypes()
})
</script>

<style scoped>
.board-form {
  --color-primary: #16a34a;
  --color-primary-hover: #15803d;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-strong: #d1d5db;
  --color-surface: #ffffff;
  --color-surface-muted: #f3f4f6;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-full: 9999px;

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
.bf-hd {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  flex-shrink: 0;
}
.bf-hd__back {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  cursor: pointer;
}
.bf-hd__title {
  flex: 1;
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
}
.bf-hd__spacer {
  width: 44px;
  flex-shrink: 0;
}

/* 본문 폼 */
.bf-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bf-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bf-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.bf-req {
  color: var(--color-danger);
}

.bf-input,
.bf-select,
.bf-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  font-family: inherit;
}
.bf-textarea {
  resize: vertical;
  min-height: 120px;
  line-height: 1.6;
}

.bf-divider {
  border: none;
  border-top: 0.5px solid var(--color-border);
  margin: 0;
}

/* 비밀번호 일치 메시지 */
.bf-pwd-msg {
  margin: 0;
  font-size: 12px;
}
.bf-pwd-msg.is-ok {
  color: var(--color-primary);
}
.bf-pwd-msg.is-error {
  color: var(--color-danger);
}

/* 첨부 */
.bf-file-hidden {
  display: none;
}
.bf-file-btn {
  align-self: flex-start;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}
.bf-file-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.bf-file-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface-muted);
  font-size: 12px;
  max-width: 100%;
}
.bf-file-item__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bf-file-item__del {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}
.bf-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

/* 하단 액션 */
.bf-footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border);
  flex-shrink: 0;
}
.bf-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
.bf-btn--ghost {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-primary);
}
.bf-btn--primary {
  border: none;
  background: var(--color-primary);
  color: #ffffff;
}
.bf-btn--primary:disabled {
  opacity: 0.6;
  cursor: default;
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
