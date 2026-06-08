<!--
  NoticeLoginPopup.vue — 로그인(앱 진입) 공지 팝업 (모바일 앱, 신규)
  - 작업 ID: prafta-app-023-3 (분해: .claude/requests/app_requests/prafta-app-023-tasks.md)
  - UI 명세: UI-app-023-2
  - 정책 출처: prafta-047 §6(팝업 판정), §6-6(고정만 SNOOZED), §10(일용직 SNOOZE 미제공)
  - 미러링 원천: web Notice01ServiceImpl.selectPopupNotices / ackConfirm / ackSnooze / read
  - 다건은 백엔드 정렬(PIN_YN DESC, PIN_ORDER ASC, INSERT_DATE DESC) 순서 그대로 스택 표시.
    프론트 재정렬 금지(백엔드 순서 신뢰).
  - 디자인 토큰: MainView .home-view 세트를 본 팝업 루트에 1회 재선언(자식 scoped 상속용).
  - 팝업 높이 바운딩: 루트 max-height 직접 부여 + 본문만 overflow-y(메모리 popup_layout_containment 준수).
  - planner 라운드: template + scoped style 완성, script 는 props/emits/ref 선언 + TODO 만.
  - developer 라운드: /popup 결과 주입은 부모(MainView)가 props 로 전달.
    ack-confirm / ack-snooze / read / file-download-token 호출 + 인덱스 진행 로직.
-->
<template>
  <div v-if="open && currentItem" class="notice-popup" role="dialog" aria-modal="true">
    <!-- 딤 배경 (닫기 트리거는 명시 버튼으로만 — 실수 닫힘 방지) -->
    <div class="notice-popup__dim" aria-hidden="true"></div>

    <!-- 팝업 카드 -->
    <div class="notice-popup__card">
      <!-- 헤더: 타이틀 + 다건 페이저 -->
      <header class="notice-popup__head">
        <p class="notice-popup__heading">공지사항</p>
        <span v-if="items.length > 1" class="notice-popup__pager">
          {{ currentIndex + 1 }} / {{ items.length }}
        </span>
      </header>

      <!-- 제목 (고정 배지) -->
      <div class="notice-popup__title-row">
        <span v-if="currentItem.pinYn === 'Y'" class="notice-popup__imp">중요</span>
        <h2 class="notice-popup__title">{{ currentItem.title }}</h2>
      </div>

      <!-- 본문 (longtext — 이 영역만 스크롤) -->
      <div class="notice-popup__body">
        <p class="notice-popup__content">{{ currentItem.content }}</p>
      </div>

      <!-- 첨부 (있을 때만) -->
      <ul
        v-if="currentItem.fileList && currentItem.fileList.length > 0"
        class="notice-popup__files"
      >
        <li v-for="file in currentItem.fileList" :key="file.fileMgmtCd" class="notice-popup__file">
          <button
            type="button"
            class="notice-popup__file-btn"
            :disabled="isDownloading"
            @click="onDownloadFile(currentItem.noticeId, file.fileMgmtCd)"
          >
            <svg class="icon" width="16" height="16" aria-hidden="true">
              <use href="#i-notice-clip" />
            </svg>
            <span class="notice-popup__file-name">{{ file.fileNm }}</span>
          </button>
        </li>
      </ul>

      <!-- 푸터 액션 — 1줄 우측정렬, 버튼은 글자 크기에 맞춤 -->
      <footer class="notice-popup__footer">
        <!-- 한시숨김: 고정(PIN_YN='Y') + 정규직(!isDaily) 일 때만 노출 -->
        <button
          v-if="canSnooze"
          type="button"
          class="notice-popup__btn notice-popup__btn--ghost"
          @click="onSnooze(currentItem.noticeId)"
        >
          오늘 그만 보기
        </button>
        <button
          type="button"
          class="notice-popup__btn notice-popup__btn--ghost"
          @click="onClose(currentItem.noticeId)"
        >
          닫기
        </button>
        <button
          type="button"
          class="notice-popup__btn notice-popup__btn--primary"
          @click="onConfirm(currentItem.noticeId)"
        >
          {{ isLastItem ? '확인' : '다음 ▶' }}
        </button>
      </footer>
    </div>

    <!-- 인라인 SVG 스프라이트 (클립 아이콘) -->
    <svg width="0" height="0" class="notice-popup__sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-notice-clip"
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
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'

import api from '@/api/axios'
import { resolveBaseURL } from '@/api/baseUrl'

const { proxy } = getCurrentInstance() || { proxy: null }

// 공통 alert 폴백(앱 전역 $alert 우선)
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const props = defineProps({
  // 팝업 표시 여부 (v-model:open)
  open: {
    type: Boolean,
    default: false,
  },
  // 팝업 공지 list (/appApi/notice01/popup 응답 — 백엔드 정렬 순서 그대로)
  // 각 항목: { noticeId, title, content, pinYn, popupFromYmd, popupToYmd, insertDate, fileCnt, isDaily, fileList:[{fileMgmtCd, fileNm}] }
  items: {
    type: Array,
    default: () => [],
  },
})

// open(v-model) + 전체 닫힘/액션 결과를 부모(MainView)로 전달
const emit = defineEmits(['update:open', 'confirm', 'snooze', 'read', 'closed'])

// ───────────────────────────────────────────────────────────
// 다건 순차 표시 상태 (백엔드 순서 신뢰 — 프론트 재정렬 금지)
// ───────────────────────────────────────────────────────────
const currentIndex = ref(0)
const isDownloading = ref(false)

const currentItem = computed(() => props.items[currentIndex.value] || null)
const isLastItem = computed(() => currentIndex.value >= props.items.length - 1)

// 한시숨김 노출 조건: 고정공지(PIN_YN='Y') + 정규직(isDaily=false)만.
// (일용직/비고정은 서버 ackSnooze 가 NOTICE_400_004 로 거부 — 정책 §6-6/§10)
const canSnooze = computed(
  () => !!currentItem.value && currentItem.value.pinYn === 'Y' && !currentItem.value.isDaily,
)

// ───────────────────────────────────────────────────────────
// 액션 핸들러 (UI 진행만 — API 호출 본문은 developer)
// ───────────────────────────────────────────────────────────

// 확인(CONFIRMED) → 다음 건으로, 마지막이면 종료.
//   실제 ack-confirm 호출은 부모(MainView)가 'confirm' 이벤트로 수행(서버 ACK 실패가 진행을 막지 않도록 분리).
const onConfirm = (noticeId) => {
  emit('confirm', noticeId)
  advance()
}

// 한시숨김(SNOOZED, 오늘+7) → 다음 건으로. ack-snooze 호출/거부 message 노출은 부모가 담당.
const onSnooze = (noticeId) => {
  emit('snooze', noticeId)
  advance()
}

// 닫기(읽음만, ACK 미설정) → 다음 건으로. read 호출은 부모가 담당.
const onClose = (noticeId) => {
  emit('read', noticeId)
  advance()
}

// 첨부 다운로드 — 토큰 발급(앱 EP) 후 웹 @NoAuth 스트림 URL 로 이동.
//   GET /appApi/notice01/file-download-token { noticeId, fileMgmtCd } → { token, expiresInSec, fileNm }
//   받은 token 으로 {baseURL}/webApi/notice01/file-download?token=... 로 메인프레임 네비게이션.
//   Flutter 셸(web_app.dart shouldOverrideUrlLoading)이 '/file-download' URL 을 가로채
//   외부 브라우저(url_launcher)로 실제 다운로드를 위임한다(웹뷰 내 octet-stream 직접 로드/연결끊김 방지).
//   isDownloading 토글로 중복 클릭 방지.
const onDownloadFile = async (noticeId, fileMgmtCd) => {
  if (isDownloading.value) return
  if (!noticeId || !fileMgmtCd) return
  isDownloading.value = true
  try {
    const { data } = await api.get('/appApi/notice01/file-download-token', {
      params: { noticeId, fileMgmtCd },
    })
    const token = data?.token
    if (!token) {
      await showAlert('첨부파일을 내려받지 못했어요. 잠시 후 다시 시도해 주세요.')
      return
    }
    const url = `${resolveBaseURL()}/webApi/notice01/file-download?token=${encodeURIComponent(token)}`
    window.location.href = url
  } catch (e) {
    // 403(대상 외)/404(없는 첨부) 등은 서버 message 우선 노출.
    await showAlert(
      e?.response?.data?.message || '첨부파일을 내려받지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isDownloading.value = false
  }
}

// 다음 건으로 진행 (마지막이면 모달 닫고 인덱스 리셋)
const advance = () => {
  if (currentIndex.value < props.items.length - 1) {
    currentIndex.value += 1
    return
  }
  currentIndex.value = 0
  emit('update:open', false)
  emit('closed')
}
</script>

<style scoped>
/*
 * 디자인 토큰 — MainView .home-view 세트를 팝업 루트에 1회 재선언.
 * (팝업은 MainView 밖 오버레이로 렌더되므로 상속이 끊겨 자체 선언 필요)
 */
.notice-popup {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-warning-tint: #fffbeb;
  --color-warning-text-deep: #9a3412;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;

  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

.notice-popup__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

/* 팝업 카드 — 높이를 고정(70vh)해 내용량과 무관하게 팝업 크기 일정. 본문만 스크롤. */
.notice-popup__card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 360px;
  height: 70vh;
  max-height: 560px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  color: var(--color-text-primary);
}

.notice-popup__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 8px;
  flex-shrink: 0;
}
.notice-popup__heading {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.notice-popup__pager {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.notice-popup__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px 8px;
  flex-shrink: 0;
}
.notice-popup__imp {
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
.notice-popup__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

/* 본문 — 이 영역만 스크롤 (flex:1 1 auto; min-height:0; overflow-y:auto) */
.notice-popup__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 16px 12px;
  border-top: 0.5px solid var(--color-border-light);
}
.notice-popup__content {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.notice-popup__files {
  list-style: none;
  margin: 0;
  padding: 8px 16px;
  border-top: 0.5px solid var(--color-border-light);
  flex-shrink: 0;
}
.notice-popup__file {
  margin: 0;
}
.notice-popup__file-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 8px 0;
  background: transparent;
  border: 0;
  color: var(--color-primary);
  font-size: 13px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.notice-popup__file-btn:disabled {
  color: var(--color-text-tertiary);
  cursor: default;
}
.notice-popup__file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 푸터 — 버튼을 1줄로 우측정렬, 각 버튼은 글자 폭에 맞춤(좌우 패딩만). */
.notice-popup__footer {
  flex-shrink: 0;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 0.5px solid var(--color-border-light);
}
.notice-popup__btn {
  flex: 0 0 auto;
  width: auto;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
}
.notice-popup__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.notice-popup__btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 0.5px solid var(--color-border);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
