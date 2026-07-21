<template>
  <div class="nearmiss-detail">
    <template v-if="!rows.length">
      <div class="edu-grid-empty">표시할 데이터가 없습니다.</div>
    </template>
    <template v-else>
      <article v-for="row in rows" :key="row.detailId" class="nm-card">
        <header class="nm-card__head">
          <span class="nm-card__when">{{ row.occurDtime }}</span>
          <span class="status-badge">{{ row.reportStatusNm }}</span>
        </header>
        <dl class="nm-card__body">
          <div>
            <dt>장소</dt>
            <dd>{{ row.locationDesc }}</dd>
          </div>
          <div>
            <dt>공정/작업</dt>
            <dd>{{ row.processNm || "-" }}</dd>
          </div>
          <div>
            <dt>내용</dt>
            <dd>{{ row.description }}</dd>
          </div>
          <div>
            <dt>잠재 중대성</dt>
            <dd>{{ row.potentialSeverityNm || "-" }}</dd>
          </div>
          <div>
            <dt>즉시 조치</dt>
            <dd>{{ row.immediateActionDesc || "-" }}</dd>
          </div>
          <div>
            <dt>임시 조치</dt>
            <dd>{{ row.adminTempActionDesc || "-" }}</dd>
          </div>
          <div>
            <dt>원인</dt>
            <dd>{{ row.causeDesc || "-" }}</dd>
          </div>
          <div>
            <dt>재발방지</dt>
            <dd>{{ row.preventionDesc || "-" }}</dd>
          </div>
          <div>
            <dt>제보자</dt>
            <dd>{{ row.affilCmpnyNm }} · {{ row.reporterNm || "-" }}</dd>
          </div>
        </dl>
        <div v-if="row.fileMgmtCd" class="nm-card__photo">
          <img
            :src="fileUrl(row.fileMgmtCd)"
            alt="아차사고 사진"
            @click="fnOpenImage(row.fileMgmtCd)"
          />
        </div>
      </article>
    </template>

    <div v-if="viewerSrc" class="image-viewer" @click="viewerSrc = ''">
      <img :src="viewerSrc" alt="첨부 원본" />
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, watch, onMounted, onBeforeUnmount, defineProps, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ snapshotId: [Number, String] });
const { proxy } = getCurrentInstance();

const rows = ref([]); // GET /webApi/subcon03/snapshot-nearmiss-detail
const viewerSrc = ref("");

// 첨부 blob objectURL 캐시(fileMgmtCd → objectURL).
const blobUrls = ref({});

const fileUrl = (fileMgmtCd) => blobUrls.value[fileMgmtCd] || "";
const fnOpenImage = (fileMgmtCd) => {
  const url = blobUrls.value[fileMgmtCd];
  if (url) viewerSrc.value = url;
};

const clearBlobs = () => {
  Object.values(blobUrls.value).forEach((u) => {
    try {
      URL.revokeObjectURL(u);
    } catch (e) {
      /* noop */
    }
  });
  blobUrls.value = {};
};

const loadBlob = async (fileMgmtCd) => {
  if (!fileMgmtCd || blobUrls.value[fileMgmtCd]) return;
  try {
    const response = await axios.get("/webApi/subcon03/snapshot-file", {
      params: { snapshotId: props.snapshotId, fileMgmtCd },
      responseType: "blob",
    });
    blobUrls.value = { ...blobUrls.value, [fileMgmtCd]: URL.createObjectURL(response.data) };
  } catch (e) {
    /* 첨부 로드 실패는 비치명 */
  }
};

const fnLoad = async () => {
  rows.value = [];
  clearBlobs();
  if (props.snapshotId == null) return;

  try {
    const response = await axios.get("/webApi/subcon03/snapshot-nearmiss-detail", {
      params: { snapshotId: props.snapshotId },
    });
    if (response.status === 200) {
      rows.value = response.data?.rows || [];
      rows.value.forEach((r) => {
        if (r.fileMgmtCd) loadBlob(r.fileMgmtCd);
      });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

onMounted(fnLoad);
watch(() => props.snapshotId, fnLoad);
onBeforeUnmount(clearBlobs);
</script>

<style scoped>
.nm-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 8px);
  padding: var(--space-md, 0.75rem);
  margin-bottom: var(--space-md, 0.75rem);
}
.nm-card__head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-sm, 0.5rem);
}
.nm-card__when { font-weight: 600; }
.status-badge {
  display: inline-block; padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary-bg, #dcfce7); color: var(--color-primary, #16a34a);
  font-size: var(--btn-font-sm, 11px);
}
.nm-card__body { margin: 0; }
.nm-card__body > div { display: flex; gap: var(--space-sm, 0.5rem); padding: 0.15rem 0; }
.nm-card__body dt { width: 90px; flex-shrink: 0; color: var(--color-text-muted, #6b7280); }
.nm-card__body dd { margin: 0; }
.nm-card__photo { margin-top: var(--space-sm, 0.5rem); }
.nm-card__photo img {
  max-width: 160px; max-height: 160px; object-fit: cover;
  border-radius: var(--btn-radius, 8px); cursor: pointer;
}
.image-viewer {
  position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.7); z-index: 1000; cursor: zoom-out;
}
.image-viewer img { max-width: 90vw; max-height: 90vh; }
</style>
