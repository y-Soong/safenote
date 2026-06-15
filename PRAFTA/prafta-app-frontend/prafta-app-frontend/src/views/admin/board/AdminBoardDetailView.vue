<!--
  AdminBoardDetailView.vue — 관리자 안전자료실(Archive) 상세 (읽기 전용) [prafta-app-025 J1-8]
  - 진입: /AdminBoardDetail?noticeId=A20260614001 (AdminBoardView 행 선택).
  - 동작: 제목/자료타입/등록자/일시/본문 + 첨부 다운로드. ★편집/삭제 없음(웹 위임).
  - 백엔드:
      GET /appApi/notice02/archive-info?noticeId=                       (B3 상세 + 첨부 list)
      GET /appApi/notice02/file-download-token?noticeId=&fileMgmtCd=     (B6 다운로드 단기 토큰, scope=ARCHIVE_FILE_DL)
      → 스트림은 웹 @NoAuth /webApi/notice02/file-download?token= 재사용(Flutter 셸이 가로채 외부 브라우저 위임).
  - C1: 자료실=회사 전체 공통. noticeId 는 서버가 CMPNY_CD + NOTICE_TYPE='ARCHIVE' + DEL_YN='N' 스코프로만 조회(타사 차단).
  - 참조 패턴: views/notice/NoticeDetailView.vue(헤더 뒤로 + 메타 + 본문 pre-wrap + 첨부 토큰→웹 스트림).
  - planner 라운드 범위: template + scoped style 완성, script 는 import/ref 선언 + computed(라우트 파싱)만.
  - developer 라운드 범위(TODO):
      (1) GET archive-info → archive/fileList 바인딩 + 로딩/빈/404 처리
      (2) onDownloadFile: file-download-token 발급 → resolveBaseURL()+/webApi/notice02/file-download?token= 로 이동
      (3) noticeId 누락/404 → 빈 상태, 뒤로 라우팅
  - 디자인 토큰: NoticeDetailView 동형 inline scoped 토큰(루트 1회 선언).
-->
<template>
  <div class="board-detail">
    <!-- 헤더 -->
    <header class="bd-hd">
      <button type="button" class="bd-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-bd-chev-left" />
        </svg>
      </button>
      <h1 class="bd-hd__title">자료 상세</h1>
      <span class="bd-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤) -->
    <main class="bd-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="bd-loading" aria-live="polite">불러오는 중...</div>

      <template v-else-if="archive">
        <!-- 자료타입 + 제목 -->
        <div class="bd-title-row">
          <span v-if="archive.archiveTypeNm" class="bd-type">{{ archive.archiveTypeNm }}</span>
          <h2 class="bd-title">{{ archive.title }}</h2>
        </div>

        <!-- 등록자 / 일시 -->
        <p class="bd-meta">
          <span v-if="archive.insertUserNm">{{ archive.insertUserNm }} · </span>{{ archive.insertDate }}
        </p>

        <!-- 본문 (white-space 보존) -->
        <div class="bd-content">{{ archive.content }}</div>

        <!-- 첨부 -->
        <section v-if="fileList.length > 0" class="bd-files">
          <p class="bd-files__label">첨부파일</p>
          <ul class="bd-files__list">
            <li v-for="file in fileList" :key="file.fileMgmtCd" class="bd-file">
              <button
                type="button"
                class="bd-file__btn"
                :disabled="isDownloading"
                @click="onDownloadFile(archive.noticeId, file.fileMgmtCd)"
              >
                <svg class="icon" width="16" height="16" aria-hidden="true">
                  <use href="#i-bd-clip" />
                </svg>
                <span class="bd-file__name">{{ file.fileNm }}</span>
                <svg class="icon bd-file__dl" width="16" height="16" aria-hidden="true">
                  <use href="#i-bd-download" />
                </svg>
              </button>
            </li>
          </ul>
        </section>
      </template>

      <!-- 빈/권한없음 -->
      <div v-else class="bd-empty">자료를 찾을 수 없어요</div>
    </main>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="bd-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-bd-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-bd-clip" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
        </symbol>
        <symbol id="i-bd-download" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="7 10 12 15 17 10" />
          <line x1="12" y1="15" x2="12" y2="3" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import api from '@/api/axios'
import { resolveBaseURL } from '@/api/baseUrl'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 진입 query (목록 행 → noticeId)
const noticeId = computed(() => route.query.noticeId || '')

// ── 상태 ─────────────────────────────────────────────────────────────
const isLoading = ref(true)
const isDownloading = ref(false)

// archive: { noticeId, archiveTypeNm, title, content, insertUserNm, insertDate, ... } (archive-info 응답 archiveInfo)
const archive = ref(null)
// fileList: [{ fileMgmtCd, fileNm, ... }] (archive-info 응답 fileList)
const archiveFileList = ref([])
const fileList = computed(() => archiveFileList.value || [])

// ── 핸들러 ────────────────────────────────────────────────────────────
const onBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/AdminBoard')
}

// 첨부 다운로드 — 토큰 발급 후 웹 @NoAuth 스트림 URL 로 이동(Flutter 셸이 외부 브라우저 위임).
const onDownloadFile = async (id, fileMgmtCd) => {
  if (isDownloading.value) return
  if (!id || !fileMgmtCd) return
  isDownloading.value = true
  try {
    // 단기 토큰(scope=ARCHIVE_FILE_DL) 발급 → 웹 @NoAuth 스트림 URL 로 이동(Flutter 셸이 외부 브라우저 위임).
    const { data } = await api.get('/appApi/notice02/file-download-token', {
      params: { noticeId: id, fileMgmtCd },
    })
    const token = data?.token
    if (!token) {
      await showAlert('첨부파일을 내려받지 못했어요.')
      return
    }
    const url = `${resolveBaseURL()}/webApi/notice02/file-download?token=${encodeURIComponent(token)}`
    window.location.href = url
  } catch (e) {
    await showAlert(e?.response?.data?.message || '첨부파일을 내려받지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isDownloading.value = false
  }
}

// ── 상세 조회 — GET /appApi/notice02/archive-info?noticeId= ─────────────
const loadDetail = async () => {
  if (!noticeId.value) {
    archive.value = null
    isLoading.value = false
    return
  }
  isLoading.value = true
  try {
    // GET /appApi/notice02/archive-info → 마스터 + 첨부 list. 서버가 CMPNY+ARCHIVE+DEL_YN='N' 스코프로만 조회(타사 차단).
    const { data } = await api.get('/appApi/notice02/archive-info', {
      params: { noticeId: noticeId.value },
    })
    archive.value = data?.archiveInfo || null
    archiveFileList.value = Array.isArray(data?.fileList) ? data.fileList : []
  } catch (e) {
    const status = e?.response?.status
    if (status === 404) {
      archive.value = null
      return
    }
    console.error('[AdminBoardDetailView] 상세 조회 실패:', e?.message)
    await showAlert(e?.response?.data?.message || '자료를 불러오지 못했어요.')
    archive.value = null
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.board-detail {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;

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
.bd-hd {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  flex-shrink: 0;
}
.bd-hd__back {
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
.bd-hd__title {
  flex: 1;
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
}
.bd-hd__spacer {
  width: 44px;
  flex-shrink: 0;
}

/* 본문 */
.bd-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.bd-loading,
.bd-empty {
  padding: 48px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.bd-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.bd-type {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}
.bd-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}
.bd-meta {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.bd-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.bd-files {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 0.5px solid var(--color-border);
}
.bd-files__label {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.bd-files__list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.bd-file {
  margin: 0;
}
.bd-file__btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  color: var(--color-primary);
  font-size: 14px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.bd-file__btn:disabled {
  color: var(--color-text-tertiary);
  cursor: default;
}
.bd-file__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bd-file__dl {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
