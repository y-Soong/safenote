<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @keydown.ctrl.a.stop
      @keydown.meta.a.stop
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ master.title || "TBM 교육자료 상세" }}</span>
          <div class="header-actions">
            <button
              v-if="props.onEdit"
              type="button"
              class="btn btn-second btn-sm"
              @click="fnEdit"
            >
              수정
            </button>
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
        </div>

        <!-- 🔹 Content -->
        <div class="detail-wrapper">
          <!-- 메타 정보 -->
          <div class="detail-meta">
            <span class="meta-item">
              스코프:
              <span
                class="scope-badge"
                :class="
                  master.isCommonContent === 'Y'
                    ? 'scope-badge-common'
                    : 'scope-badge-site'
                "
              >
                {{ master.isCommonContent === "Y" ? "회사공통" : "사업장" }}
              </span>
            </span>
            <span class="meta-item">카테고리: {{ categoryNm }}</span>
            <span class="meta-item">등록자: {{ master.insertNm }}</span>
            <span class="meta-item">등록일: {{ master.insertDate }}</span>
          </div>
          <p v-if="master.contents" class="detail-desc">
            {{ master.contents }}
          </p>

          <!-- 세부항목(미디어) -->
          <div class="detail-section">
            <div class="detail-section-title">세부항목</div>
            <template v-if="itemList.length === 0">
              <div class="detail-empty">등록된 세부 항목이 없습니다.</div>
            </template>
            <template v-else>
              <div
                v-for="(item, idx) in itemList"
                :key="item.mtrlItemCd || idx"
                class="media-card"
              >
                <div class="media-card-head">
                  <span class="media-idx">{{ idx + 1 }}.</span>
                  <span class="media-type">{{
                    mediaTypeNm(item.mtrlItemType)
                  }}</span>
                  <span v-if="item.durationSec" class="media-duration">
                    ({{ formatDuration(item.durationSec) }})
                  </span>
                </div>

                <!-- 미디어 미리보기 -->
                <div class="media-preview">
                  <!-- 동영상 -->
                  <video
                    v-if="item.mtrlItemType === '02' && fileUrl(item)"
                    :src="fileUrl(item)"
                    controls
                    class="media-video"
                  ></video>
                  <!-- 유튜브 -->
                  <iframe
                    v-else-if="
                      item.mtrlItemType === '03' && youtubeEmbed(item.url)
                    "
                    :src="youtubeEmbed(item.url)"
                    class="media-youtube"
                    frameborder="0"
                    allowfullscreen
                  ></iframe>
                  <!-- 이미지 -->
                  <img
                    v-else-if="item.mtrlItemType === '01' && fileUrl(item)"
                    :src="fileUrl(item)"
                    class="media-image"
                    alt="이미지"
                  />
                  <!-- PDF -->
                  <div v-else-if="item.mtrlItemType === '04' && fileUrl(item)">
                    <iframe
                      :src="fileUrl(item)"
                      class="media-pdf"
                      frameborder="0"
                    ></iframe>
                    <a
                      :href="fileUrl(item)"
                      target="_blank"
                      rel="noopener"
                      class="media-link"
                      >새 탭에서 PDF 열기</a
                    >
                  </div>
                  <div v-else class="media-na">
                    미리보기를 제공할 수 없습니다.
                  </div>
                </div>

                <div v-if="item.mtrlDesc" class="media-desc">
                  {{ item.mtrlDesc }}
                </div>
              </div>
            </template>
          </div>

          <!-- 사용한 TBM 이력 -->
          <div class="detail-section">
            <div class="detail-section-title">이 자료를 사용한 TBM 이력</div>
            <template v-if="usedSessionList.length === 0">
              <div class="detail-empty">사용된 TBM 이력이 없습니다.</div>
            </template>
            <template v-else>
              <ul class="usage-list">
                <li
                  v-for="(sess, idx) in usedSessionList"
                  :key="sess.sessionCd || idx"
                  class="usage-item"
                >
                  <span class="usage-title">{{ sess.title }}</span>
                  <span class="usage-status">{{
                    statusNm(sess.statusCd)
                  }}</span>
                  <span class="usage-date">{{
                    sess.openedAt || sess.endedAt || "-"
                  }}</span>
                </li>
              </ul>
            </template>
          </div>
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
/* eslint-disable */
import {
  ref,
  reactive,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

// ================ Instance ================
const { proxy } = getCurrentInstance();

// ================ Props / Emits ================
const props = defineProps({
  mtrlCd_p: String,
  onEdit: Function,
});
const emit = defineEmits(["close"]);

// ================ Refs ================
const modalRef = ref(null);
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "";

const master = reactive({
  mtrlCd: "",
  title: "",
  contents: "",
  mtrlType: "",
  siteCd: "",
  isCommonContent: "",
  insertNm: "",
  insertDate: "",
});
const itemList = ref([]);
const usedSessionList = ref([]);
const baseCodeArr = ref({});

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Computed ================
const categoryNm = computed(() => {
  const list = baseCodeArr.value?.COM003 || [];
  const found = list.find((o) => o.baimValDCd === master.mtrlType);
  return found ? found.baimValDNm : master.mtrlType || "-";
});

// ================ Life Cycle ================
onMounted(async () => {
  await fnGetBaseinfoList();
  await fnSearch();
});

// ================ API ================
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM003"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      baseCodeArr.value = grouped;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm01/tbm-edu-detail", {
      params: { mtrlCd: props.mtrlCd_p },
    });

    if (response.status === 200) {
      const data = response.data || {};
      const info = data.tbmEduInfo || {};
      master.mtrlCd = info.mtrlCd || "";
      master.title = info.title || "";
      master.contents = info.contents || "";
      master.mtrlType = info.mtrlType || "";
      master.siteCd = info.siteCd || "";
      master.isCommonContent = info.isCommonContent || "";
      master.insertNm = info.insertNm || "";
      master.insertDate = info.insertDate || "";

      itemList.value = data.tbmEduItemInfoList || [];
      usedSessionList.value = data.usedSessionList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// ================ User Functions ================
const fnEdit = () => {
  if (typeof props.onEdit === "function") {
    emit("close");
    props.onEdit();
  }
};

/** 서버 파일 URL — 서명 URL 전환: 서버가 발급한 서명 절대 URL(item.fileUrl)을 그대로 사용.
 *  (기존 baseUrl + filePath + fileMgmtCd 수동 조립 제거.) 파일 없으면 빈 문자열. */
const fileUrl = (item) => item?.fileUrl || "";

/** 유튜브 URL -> embed URL 변환 (watch?v= / youtu.be/) */
const youtubeEmbed = (url) => {
  if (!url) return "";
  let videoId = "";
  const watchMatch = url.match(/[?&]v=([^&]+)/);
  const shortMatch = url.match(/youtu\.be\/([^?&]+)/);
  if (watchMatch) videoId = watchMatch[1];
  else if (shortMatch) videoId = shortMatch[1];
  if (!videoId) return "";
  return "https://www.youtube.com/embed/" + videoId;
};

const mediaTypeNm = (type) => {
  if (type === "01") return "이미지";
  if (type === "02") return "동영상";
  if (type === "03") return "YouTube";
  if (type === "04") return "PDF";
  return type || "-";
};

const formatDuration = (sec) => {
  const s = parseInt(sec, 10);
  if (isNaN(s) || s <= 0) return "";
  const m = Math.floor(s / 60);
  const r = s % 60;
  return m + "분 " + r + "초";
};

const statusNm = (statusCd) => {
  switch (statusCd) {
    case "DRAFT":
      return "작성중";
    case "OPENED":
      return "개설";
    case "IN_PROGRESS":
      return "진행중";
    case "COMPLETED":
      return "종료";
    case "CANCELLED":
      return "취소";
    default:
      return statusCd || "-";
  }
};
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.detail-wrapper {
  padding: 1.2rem;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}

.meta-item {
  font-size: var(--btn-font);
}

.detail-desc {
  margin: 0.75rem 0;
  color: var(--color-text-muted);
  white-space: pre-wrap;
}

.detail-section {
  margin-top: 1.25rem;
}

.detail-section-title {
  font-weight: 600;
  color: var(--color-text-strong);
  margin-bottom: 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid var(--color-border);
}

.detail-empty {
  padding: 1rem;
  text-align: center;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-radius: var(--btn-radius);
}

.media-card {
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  padding: 0.75rem;
  margin-bottom: 0.75rem;
  background: var(--color-surface);
}

.media-card-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.media-idx {
  font-weight: 600;
  color: var(--color-text-strong);
}

.media-type {
  color: var(--color-primary);
  font-size: var(--btn-font-sm);
  font-weight: 600;
}

.media-duration {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}

.media-preview {
  margin: 0.5rem 0;
}

.media-video,
.media-youtube,
.media-pdf {
  width: 100%;
  max-width: 640px;
  height: 360px;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
}

.media-image {
  max-width: 100%;
  max-height: 360px;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
}

.media-link {
  display: inline-block;
  margin-top: 0.5rem;
  color: var(--color-primary);
}

.media-na {
  padding: 1rem;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-radius: var(--btn-radius);
}

.media-desc {
  color: var(--color-text);
  font-size: var(--btn-font);
}

.usage-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.usage-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--color-border);
}

.usage-title {
  flex: 1;
  color: var(--color-text-strong);
}

.usage-status {
  font-size: var(--btn-font-sm);
  color: var(--color-primary);
}

.usage-date {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.scope-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
}

.scope-badge-common {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.scope-badge-site {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}
</style>
