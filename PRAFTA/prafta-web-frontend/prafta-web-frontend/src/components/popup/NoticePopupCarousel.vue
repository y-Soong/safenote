<template>
  <Transition name="fade">
    <div v-show="hasNotices" class="modal-overlay notice-popup-overlay">
      <div class="notice-popup-card">
        <!-- 캐러셀 헤더: 제목 + 네비게이션 -->
        <div class="notice-popup-head">
          <h3 class="notice-popup-title">{{ currentItem.title }}</h3>
          <div v-if="isMulti" class="notice-popup-nav">
            <button class="nav-btn" aria-label="이전" @click="fnPrev">‹</button>
            <span class="nav-indicator">{{ currentIndex + 1 }} / {{ noticeList.length }}</span>
            <button class="nav-btn" aria-label="다음" @click="fnNext">›</button>
          </div>
        </div>

        <p class="notice-popup-date">{{ fnFormatDate(currentItem.insertDate) }}</p>

        <!-- 본문 (공지 내용만 스크롤 — 카드 크기는 고정) -->
        <div class="notice-popup-body">
          <p class="notice-popup-content">{{ currentItem.content }}</p>
        </div>

        <!-- 첨부 (본문 스크롤 밖 고정 영역 — 항상 노출/다운로드 가능) -->
        <div
          v-if="currentItem.fileList && currentItem.fileList.length > 0"
          class="notice-popup-files"
        >
          <div class="files-label">📎 첨부파일 {{ currentItem.fileList.length }}건</div>
          <ul class="file-list">
            <li
              v-for="(f, i) in currentItem.fileList"
              :key="i"
              class="file-item"
            >
              <span class="file-name" :title="f.fileNm">{{ f.fileNm }}</span>
              <button
                class="file-dl-btn"
                type="button"
                title="다운로드"
                @click="fnDownloadFile(f)"
              >
                ⬇
              </button>
            </li>
          </ul>
        </div>

        <!-- 인디케이터 점 -->
        <div v-if="isMulti" class="notice-popup-dots">
          <span
            v-for="(item, i) in noticeList"
            :key="i"
            class="dot"
            :class="{ 'dot--active': i === currentIndex }"
            @click="fnGoTo(i)"
          ></span>
        </div>

        <!-- 하단 버튼 (§6-6 분기) -->
        <div class="notice-popup-actions">
          <!-- 정규직 + 고정 공지: [일주일간 보지 않기] -->
          <button
            v-if="showSnooze"
            class="btn-secondary"
            @click="fnSnooze"
          >
            일주일간 보지 않기
          </button>
          <!-- 확인(CONFIRMED): 정규+비고정, 일용직(고정/비고정 모두) -->
          <button
            v-if="showConfirm"
            class="btn-primary"
            @click="fnConfirm"
          >
            확인
          </button>
          <!-- 닫기: 항상 (이력 없이 다음 로그인 재노출) -->
          <button class="btn-secondary" @click="fnClose">닫기</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  onMounted,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveBaseURL } from "@/api/baseUrl";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { ymdCompactToDash } from "@/utils/noticeDate";

const props = defineProps({
  // 부모(로그인/메인)가 미리 조회한 목록을 넘겨도 되고, 비우면 onMounted에서 조회
  notices: {
    type: Array,
    default: () => [],
  },
  onClose: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// 노출 공지 목록 (서버 popup 응답: noticeId/title/content/pinYn/insertDate/fileCnt/isDaily)
const noticeList = ref([]);
const currentIndex = ref(0);

const hasNotices = computed(() => noticeList.value.length > 0);
const isMulti = computed(() => noticeList.value.length > 1);
const currentItem = computed(() => noticeList.value[currentIndex.value] || {});

// 버튼 분기 (§6-6): 서버가 내려준 currentItem.pinYn / currentItem.isDaily 기준
const showSnooze = computed(
  () => currentItem.value.isDaily !== true && currentItem.value.pinYn === "Y"
);
const showConfirm = computed(() => {
  // 일용직: 항상 확인. 정규+비고정: 확인. 정규+고정: 확인 없음(스누즈만)
  if (currentItem.value.isDaily === true) return true;
  return currentItem.value.pinYn !== "Y";
});

// 등록일시 표시(insertDate 가 datetime 문자열이거나 YYYYMMDD 일 수 있어 방어적 처리)
const fnFormatDate = (value) => {
  if (!value) return "";
  const v = String(value).trim();
  // 'YYYY-MM-DD HH:mm:ss' 형태면 날짜 부분만
  if (v.includes("-")) return v.split(" ")[0];
  if (v.length >= 8) return ymdCompactToDash(v.slice(0, 8));
  return v;
};

onMounted(async () => {
  if (props.notices && props.notices.length) {
    noticeList.value = [...props.notices];
  } else {
    await fnLoadPopups();
  }
  if (noticeList.value.length > 0) {
    fnMarkRead();
  } else {
    // 노출할 공지 없음 → 즉시 닫기
    fnClose();
  }
});

// 팝업 대상 조회 (식별자/현재소속은 서버 JWT 도출, LIMIT 10·정렬 §5)
const fnLoadPopups = async () => {
  try {
    const response = await axios.post(
      "/webApi/notice01/popup",
      {},
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      noticeList.value = response.data?.popupList || [];
    }
  } catch (err) {
    // 팝업 조회 실패는 사용자 흐름을 막지 않도록 조용히 닫기(메인 진입 비차단)
    noticeList.value = [];
  }
};

// 캐러셀 이동 (UI)
const fnPrev = () => {
  currentIndex.value =
    (currentIndex.value - 1 + noticeList.value.length) % noticeList.value.length;
  fnMarkRead();
};
const fnNext = () => {
  currentIndex.value = (currentIndex.value + 1) % noticeList.value.length;
  fnMarkRead();
};
const fnGoTo = (i) => {
  currentIndex.value = i;
  fnMarkRead();
};

// 열람 시 LAST_READ_DATE 갱신 (§7 뱃지 소멸). 실패해도 흐름 비차단.
const fnMarkRead = async () => {
  const noticeId = currentItem.value?.noticeId;
  if (!noticeId) return;
  try {
    await axios.post(
      "/webApi/notice01/read",
      { noticeId },
      { headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    // 열람 갱신 실패는 무시(다음 열람 시 재시도)
  }
};

// [확인] → CONFIRMED
const fnConfirm = async () => {
  const noticeId = currentItem.value?.noticeId;
  if (!noticeId) return;
  try {
    await axios.post(
      "/webApi/notice01/ack-confirm",
      { noticeId },
      { headers: { "Content-Type": "application/json" } }
    );
    fnAdvanceAfterAck();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// [일주일간 보지 않기] → SNOOZED (정규직·고정 한정)
const fnSnooze = async () => {
  const noticeId = currentItem.value?.noticeId;
  if (!noticeId) return;
  try {
    await axios.post(
      "/webApi/notice01/ack-snooze",
      { noticeId },
      { headers: { "Content-Type": "application/json" } }
    );
    fnAdvanceAfterAck();
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ack 처리 후 현재 항목 제거 + 다음 노출 (UI 흐름)
const fnAdvanceAfterAck = () => {
  noticeList.value.splice(currentIndex.value, 1);
  if (noticeList.value.length === 0) {
    fnClose();
    return;
  }
  if (currentIndex.value >= noticeList.value.length) {
    currentIndex.value = noticeList.value.length - 1;
  }
  fnMarkRead();
};

// 첨부 다운로드 (단기 토큰 발급 → file-download?token= 로 브라우저 다운로드).
// 팝업 수신자는 노출 대상이므로 토큰 발급 시 서버 대상 재검증을 통과한다.
const fnDownloadFile = async (file) => {
  const noticeId = currentItem.value?.noticeId;
  if (!noticeId || !file?.fileMgmtCd) return;
  try {
    const response = await axios.get("/webApi/notice01/file-download-token", {
      params: { noticeId, fileMgmtCd: file.fileMgmtCd },
    });
    if (response.status === 200 && response.data?.token) {
      const base = String(resolveBaseURL() || "").replace(/\/$/, "");
      const url =
        `${base}/webApi/notice01/file-download?token=` +
        encodeURIComponent(response.data.token);
      // 토큰 쿼리 방식(서버 @NoAuth) → 브라우저 직접 다운로드
      window.open(url, "_blank");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "다운로드 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// [닫기] → 이력 없이 닫기 (다음 로그인 재노출)
const fnClose = () => {
  props.onClose?.();
  emit("close");
};
</script>

<style scoped>
.notice-popup-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
}

.notice-popup-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm, 0.75rem);
  /* 고정 크기: 내용 길이에 따라 커지지 않는다. 넘치는 본문은 .notice-popup-body 가 스크롤 */
  width: 600px;
  height: 620px;
  max-width: 92vw;
  max-height: 88vh;
  padding: var(--space-lg, 1.5rem);
  background: var(--card-bg, #ffffff);
  border: var(--card-border, 1px solid #e5e7eb);
  border-radius: var(--card-radius, 16px);
  box-shadow: var(--card-shadow, 0 1px 3px rgba(16, 24, 40, 0.1));
}

.notice-popup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm, 0.5rem);
  flex-shrink: 0;
}

.notice-popup-title {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}

.notice-popup-nav {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs, 0.375rem);
  flex-shrink: 0;
}

.nav-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
  font-size: var(--font-size-lg, 1.125rem);
  line-height: 1;
  cursor: pointer;
}

.nav-btn:hover {
  background: var(--color-bg, #f9fafb);
}

.nav-indicator {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #4b5563);
  min-width: 40px;
  text-align: center;
}

.notice-popup-date {
  margin: 0;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #4b5563);
  flex-shrink: 0;
}

.notice-popup-body {
  flex: 1 1 auto;
  /* flex 컬럼 안에서 자식 스크롤이 동작하려면 min-height:0 필수 */
  min-height: 0;
  overflow-y: auto;
  padding-right: var(--space-xs, 0.25rem);
}

.notice-popup-content {
  margin: 0;
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
  white-space: pre-wrap;
  line-height: 1.6;
}

/* 첨부 영역(본문 스크롤 밖 고정). 첨부가 많으면 목록 자체만 스크롤 */
.notice-popup-files {
  flex-shrink: 0;
  padding-top: var(--space-sm, 0.5rem);
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.files-label {
  margin-bottom: var(--space-xs, 0.375rem);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
}

.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs, 0.25rem);
  max-height: 96px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm, 0.5rem);
  padding: var(--space-xxs, 0.25rem) var(--space-sm, 0.5rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface-muted, #f9fafb);
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text, #374151);
}

.file-dl-btn {
  flex-shrink: 0;
  width: 28px;
  height: 24px;
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--btn-radius, 8px);
  background: var(--color-surface, #ffffff);
  color: var(--color-primary, #16a34a);
  font-size: var(--font-size-sm, 0.875rem);
  line-height: 1;
  cursor: pointer;
}

.file-dl-btn:hover {
  background: var(--color-primary-soft, #ecfdf3);
  border-color: var(--color-primary, #16a34a);
}

.notice-popup-dots {
  display: flex;
  justify-content: center;
  gap: var(--space-xs, 0.375rem);
  flex-shrink: 0;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-pill, 999px);
  background: var(--color-border-strong, #d1d5db);
  cursor: pointer;
}

.dot--active {
  background: var(--color-primary, #16a34a);
}

.notice-popup-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 0.5rem);
  flex-shrink: 0;
}

.btn-primary {
  height: var(--btn-height-lg, 32px);
  padding: 0 var(--btn-padding-lg, 13px);
  border: none;
  border-radius: var(--btn-radius-lg, 10px);
  background: var(--color-primary, #16a34a);
  color: #ffffff;
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}

.btn-primary:hover {
  background: var(--color-primary-hover, #15803d);
}

.btn-secondary {
  height: var(--btn-height-lg, 32px);
  padding: 0 var(--btn-padding-lg, 13px);
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--btn-radius-lg, 10px);
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
  font-size: var(--btn-font, 11px);
  cursor: pointer;
}

/* 모바일 대응 (~480px) */
@media (max-width: 480px) {
  .notice-popup-card {
    width: 92vw;
    padding: var(--space-md, 1rem);
  }

  .notice-popup-actions {
    flex-wrap: wrap;
  }
}
</style>
