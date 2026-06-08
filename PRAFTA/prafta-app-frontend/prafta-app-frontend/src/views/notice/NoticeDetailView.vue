<!--
  NoticeDetailView.vue — 공지 상세 (모바일 앱, 신규, 읽기 전용)
  - 작업 ID: prafta-app-023-4 (분해: .claude/requests/app_requests/prafta-app-023-tasks.md)
  - UI 명세: UI-app-023-3
  - 정책 출처: prafta-047 §7(열람/뱃지 소멸), §6(대상 재검증=백엔드 fail-closed)
  - 미러링 원천: web Notice01ServiceImpl.selectNoticeInfo (앱은 진입 전 대상 재검증 추가 — 0.3)
  - 진입: /NoticeDetail?noticeId=N20260608001  (카드/팝업/전체목록에서 라우팅)
  - 편집/삭제 없음(D2 — 앱은 읽기/ACK 전용).
  - 참조: NearMissManageDetail(헤더 뒤로 + dl 읽기본문 + 첨부 + 푸터 없음).
  - 디자인 토큰: MainView .home-view 세트를 .notice-detail 루트에 1회 재선언.
  - planner 라운드: template + scoped style 완성, script 는 선언 + TODO 만.
  - developer 라운드: GET /notice-info, 첨부 토큰 다운로드, 403/뒤로 라우팅, noticeId 라우트 파싱.
-->
<template>
  <div class="notice-detail">
    <!-- 헤더 -->
    <header class="nd-hd">
      <button type="button" class="nd-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-nd-chev-left" />
        </svg>
      </button>
      <h1 class="nd-hd__title">공지 상세</h1>
      <span class="nd-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤) -->
    <main class="nd-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="nd-loading" aria-live="polite">불러오는 중...</div>

      <template v-else-if="notice">
        <!-- 제목 + 고정 배지 -->
        <div class="nd-title-row">
          <span v-if="notice.pinYn === 'Y'" class="nd-imp">중요</span>
          <h2 class="nd-title">{{ notice.title }}</h2>
        </div>

        <!-- 작성자 / 일시 -->
        <p class="nd-meta">
          <span v-if="notice.insertUserNm">{{ notice.insertUserNm }} · </span>
          {{ notice.insertDate }}
        </p>

        <!-- 본문 (white-space 보존) -->
        <div class="nd-content">{{ notice.content }}</div>

        <!-- 첨부 -->
        <section v-if="fileList.length > 0" class="nd-files">
          <p class="nd-files__label">첨부파일</p>
          <ul class="nd-files__list">
            <li v-for="file in fileList" :key="file.fileMgmtCd" class="nd-file">
              <button
                type="button"
                class="nd-file__btn"
                :disabled="isDownloading"
                @click="onDownloadFile(notice.noticeId, file.fileMgmtCd)"
              >
                <svg class="icon" width="16" height="16" aria-hidden="true">
                  <use href="#i-nd-clip" />
                </svg>
                <span class="nd-file__name">{{ file.fileNm }}</span>
                <svg class="icon nd-file__dl" width="16" height="16" aria-hidden="true">
                  <use href="#i-nd-download" />
                </svg>
              </button>
            </li>
          </ul>
        </section>
      </template>

      <!-- 빈/권한없음 -->
      <div v-else class="nd-empty">공지를 찾을 수 없어요</div>
    </main>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="nd-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-nd-chev-left"
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
          id="i-nd-clip"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path
            d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"
          />
        </symbol>
        <symbol
          id="i-nd-download"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
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

// 진입 query (카드/팝업/목록 → noticeId)
const noticeId = computed(() => route.query.noticeId || '')

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)
const isDownloading = ref(false)

// notice: { noticeId, title, content, pinYn, insertUserNm, insertDate, ... } (notice-info 응답 noticeInfo)
const notice = ref(null)
// fileList: [{ fileMgmtCd, fileNm, ... }] (notice-info 응답 fileList)
const noticeFileList = ref([])

const fileList = computed(() => noticeFileList.value || [])

// ───────────────────────────────────────────────────────────
// 핸들러
// ───────────────────────────────────────────────────────────

const onBack = () => {
  // 히스토리가 있으면 뒤로, 없으면(직접 진입) 메인으로 폴백.
  if (window.history.length > 1) router.back()
  else router.push('/MainView')
}

// 첨부 다운로드 — 토큰 발급 후 웹 @NoAuth 스트림 URL 로 메인프레임 이동.
//   Flutter 셸(web_app.dart shouldOverrideUrlLoading)이 '/file-download' URL 을 가로채
//   외부 브라우저(url_launcher)로 실제 다운로드를 위임한다.
const onDownloadFile = async (id, fileMgmtCd) => {
  if (isDownloading.value) return
  if (!id || !fileMgmtCd) return
  isDownloading.value = true
  try {
    const { data } = await api.get('/appApi/notice01/file-download-token', {
      params: { noticeId: id, fileMgmtCd },
    })
    const token = data?.token
    if (!token) {
      await showAlert('첨부파일을 내려받지 못했어요. 잠시 후 다시 시도해 주세요.')
      return
    }
    const url = `${resolveBaseURL()}/webApi/notice01/file-download?token=${encodeURIComponent(token)}`
    window.location.href = url
  } catch (e) {
    await showAlert(
      e?.response?.data?.message || '첨부파일을 내려받지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isDownloading.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 상세 조회 — GET /appApi/notice01/notice-info?noticeId=
//   이 호출만으로 read 처리(LAST_READ_DATE 갱신)되어 뱃지가 소멸한다(별도 read 불필요).
//   대상 아니면 서버가 403(NOTICE_403_003) → 안내 후 뒤로.
// ───────────────────────────────────────────────────────────
const loadDetail = async () => {
  if (!noticeId.value) {
    // noticeId 누락 진입 → 빈 상태 폴백(에러 alert 없이 "찾을 수 없어요" 노출).
    notice.value = null
    isLoading.value = false
    return
  }
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/notice01/notice-info', {
      params: { noticeId: noticeId.value },
    })
    notice.value = data?.noticeInfo || null
    noticeFileList.value = Array.isArray(data?.fileList) ? data.fileList : []
  } catch (e) {
    const status = e?.response?.status
    const errorCode = e?.response?.data?.errorCode
    // 403(대상 아님): 권한 없음 안내 후 뒤로.
    if (status === 403 || errorCode === 'NOTICE_403_003') {
      await showAlert('이 공지를 볼 수 있는 권한이 없어요.')
      onBack()
      return
    }
    if (status === 404) {
      // 빈 상태 노출(뒤로가지 않고 "찾을 수 없어요").
      notice.value = null
      return
    }
    console.error('[NoticeDetailView] 상세 조회 실패:', e?.message)
    await showAlert(e?.response?.data?.message || '공지를 불러오지 못했어요.')
    notice.value = null
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.notice-detail {
  --color-primary: #16a34a;
  --color-warning-tint: #fffbeb;
  --color-warning-text-deep: #9a3412;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
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
.nd-hd {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  flex-shrink: 0;
}
.nd-hd__back {
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
.nd-hd__title {
  flex: 1;
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
}
.nd-hd__spacer {
  width: 44px;
  flex-shrink: 0;
}

/* 본문 */
.nd-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.nd-loading,
.nd-empty {
  padding: 48px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.nd-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.nd-imp {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-warning-tint);
  color: var(--color-warning-text-deep);
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  flex-shrink: 0;
}
.nd-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}
.nd-meta {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.nd-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.nd-files {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 0.5px solid var(--color-border);
}
.nd-files__label {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.nd-files__list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.nd-file {
  margin: 0;
}
.nd-file__btn {
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
.nd-file__btn:disabled {
  color: var(--color-text-tertiary);
  cursor: default;
}
.nd-file__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nd-file__dl {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
