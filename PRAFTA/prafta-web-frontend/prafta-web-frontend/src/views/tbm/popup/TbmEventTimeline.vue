<template>
  <!-- 임베디드 모드: 출결 상세 그리드 안에서 펼쳐지는 타임라인 -->
  <div v-if="props.embedded" class="timeline-embedded">
    <div class="timeline-head">
      <span class="timeline-title">
        이벤트 타임라인 - {{ props.userNm_p }}
      </span>
      <span class="timeline-count">총 {{ totalCount }}건</span>
    </div>
    <template v-if="!eventList || eventList.length === 0">
      <div class="timeline-empty">이벤트가 없습니다.</div>
    </template>
    <ul v-else class="timeline-list">
      <li v-for="ev in eventList" :key="ev.eventNo" class="timeline-item">
        <span class="ev-time">{{ ev.serverReceivedAt || ev.eventTime }}</span>
        <span class="ev-type" :class="evTypeClass(ev.eventTypeCd)">
          {{ ev.eventTypeNm || ev.eventTypeCd }}
        </span>
        <span v-if="ev.eventData" class="ev-data">{{
          formatData(ev.eventData)
        }}</span>
      </li>
    </ul>
    <div v-if="totalCount > pageSize" class="timeline-more">
      <button
        class="btn btn-second btn-xs"
        :disabled="page <= 1"
        @click="fnGoPage(page - 1)"
      >
        이전
      </button>
      <span class="pager-info">{{ page }} / {{ totalPages }}</span>
      <button
        class="btn btn-second btn-xs"
        :disabled="page >= totalPages"
        @click="fnGoPage(page + 1)"
      >
        다음
      </button>
    </div>
  </div>

  <!-- 독립 모달 모드 -->
  <Transition v-else name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>이벤트 타임라인 - {{ props.userNm_p }}</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
        <div class="timeline-body">
          <template v-if="!eventList || eventList.length === 0">
            <div class="timeline-empty">이벤트가 없습니다.</div>
          </template>
          <ul v-else class="timeline-list">
            <li v-for="ev in eventList" :key="ev.eventNo" class="timeline-item">
              <span class="ev-time">{{
                ev.serverReceivedAt || ev.eventTime
              }}</span>
              <span class="ev-type" :class="evTypeClass(ev.eventTypeCd)">
                {{ ev.eventTypeNm || ev.eventTypeCd }}
              </span>
              <span v-if="ev.eventData" class="ev-data">{{
                formatData(ev.eventData)
              }}</span>
            </li>
          </ul>
        </div>
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const { proxy } = getCurrentInstance();

const props = defineProps({
  attendanceCd_p: String,
  userNm_p: String,
  embedded: { type: Boolean, default: false },
});
defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 4,
});

const eventList = ref([]);
const totalCount = ref(0);
const page = ref(1);
const pageSize = ref(100);

const totalPages = computed(() => {
  const pages = Math.ceil(totalCount.value / pageSize.value);
  return pages < 1 ? 1 : pages;
});

onMounted(async () => {
  await fnSearch();
});

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm04/attendance-events", {
      params: {
        attendanceCd: props.attendanceCd_p,
        page: page.value,
        pageSize: pageSize.value,
      },
    });

    if (response.status === 200) {
      const data = response.data || {};
      eventList.value = data.eventList || [];
      totalCount.value = data.totalCount || 0;
    }
  } catch (err) {
    eventList.value = [];
    totalCount.value = 0;
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
  fnSearch();
};

// EVENT_DATA(JSON)를 사람이 읽을 짧은 문자열로 변환
const formatData = (raw) => {
  if (raw == null) return "";
  try {
    const obj = typeof raw === "string" ? JSON.parse(raw) : raw;
    if (obj && typeof obj === "object") {
      return Object.entries(obj)
        .map(([k, v]) => k + ": " + v)
        .join(", ");
    }
    return String(raw);
  } catch (e) {
    return String(raw);
  }
};

const evTypeClass = (code) => {
  switch (code) {
    case "NETWORK_LOST":
    case "BACKGROUND_OUT":
    case "FORCED_END":
      return "ev-warn";
    case "ENTER":
    case "START":
    case "END":
    case "SIGNATURE_STARTED":
      return "ev-info";
    default:
      return "";
  }
};
</script>

<style scoped>
/* 기본 .modal-content 의 20px 패딩 제거 → 헤더/본문/푸터가 박스 끝에 밀착. */
.modal-content {
  padding: 0;
}

.timeline-embedded {
  padding: 0.25rem 0;
}

.timeline-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.timeline-title {
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: var(--btn-font-sm);
}

.timeline-count {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.timeline-body {
  padding: 1rem;
  max-height: 60vh;
  overflow-y: auto;
}

.timeline-empty {
  padding: 1rem;
  text-align: center;
  color: var(--color-text-muted);
}

.timeline-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.35rem 0;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
}

.ev-time {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  min-width: 150px;
}

.ev-type {
  font-size: var(--btn-font-sm);
  font-weight: 600;
  padding: 1px 6px;
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.ev-info {
  background: var(--color-bg);
  color: var(--color-text-strong);
  border-color: var(--color-border-strong);
}

.ev-warn {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-text);
}

.ev-data {
  font-size: var(--btn-font-sm);
  color: var(--color-text);
}

.timeline-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.pager-info {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.btn-xs {
  height: var(--btn-height-sm);
  padding: 0 0.5rem;
  font-size: var(--btn-font-sm);
}
</style>
