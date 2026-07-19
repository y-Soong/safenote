<template>
  <div class="risk-detail">
    <div class="table-box overflow-x-auto rounded-md border border-slate-300"
         style="--box-h: 62vh; --box-sticky-top: 1px; --box-ox: auto">
      <table class="data-grid w-full text-sm text-left">
        <thead>
          <tr>
            <th style="width: 12%">공정/작업</th>
            <th>유해위험요인</th>
            <th style="width: 9%">상태</th>
            <th style="width: 8%">초기 위험도</th>
            <th>현재 안전조치</th>
            <th style="width: 8%">초기 첨부</th>
            <th style="width: 8%">재평가 위험도</th>
            <th>개선대책</th>
          </tr>
        </thead>
        <tbody>
          <template v-if="!rows.length">
            <tr><td colspan="8" class="edu-grid-empty">표시할 데이터가 없습니다.</td></tr>
          </template>
          <template v-else>
            <template v-for="row in rows" :key="row.detailId">
              <tr>
                <td>{{ row.processNm }}</td>
                <td>
                  <div class="cell-strong">{{ row.hazardNm }}</div>
                  <div class="cell-sub">{{ row.assessmentDesc }}</div>
                </td>
                <td style="text-align: center">{{ row.assessmentStatusNm }}</td>
                <td style="text-align: center">{{ riskLvLabel(row.initRiskLv, row.initLikelihood, row.initSeverity) }}</td>
                <td>{{ row.initDesc }}</td>
                <td style="text-align: center">
                  <img
                    v-if="row.initFileMgmtCd"
                    class="thumb"
                    :src="fileUrl(row.initFileMgmtCd)"
                    alt="초기평가 첨부"
                    @click="fnOpenImage(row.initFileMgmtCd)"
                  />
                  <span v-else class="cell-sub">-</span>
                </td>
                <td style="text-align: center">{{ riskLvLabel(row.revalRiskLv, row.revalLikelihood, row.revalSeverity) }}</td>
                <td>
                  <ul v-if="(row.improves || []).length" class="improve-list">
                    <li v-for="imp in row.improves" :key="imp.improveId">
                      <span class="cell-sub">{{ fmtYmd(imp.improveDate) }}</span>
                      {{ imp.improveDesc }}
                      <img
                        v-if="imp.fileMgmtCd"
                        class="thumb thumb--sm"
                        :src="fileUrl(imp.fileMgmtCd)"
                        alt="개선 첨부"
                        @click="fnOpenImage(imp.fileMgmtCd)"
                      />
                    </li>
                  </ul>
                  <span v-else class="cell-sub">-</span>
                </td>
              </tr>
            </template>
          </template>
        </tbody>
      </table>
    </div>

    <!-- 이미지 라이트박스(경량) -->
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

// =========================== Ref ===========================
const rows = ref([]); // GET /webApi/subcon03/snapshot-risk-detail (평가행 + improves 자식)
const viewerSrc = ref(""); // 라이트박스 src

// 첨부 blob objectURL 캐시(fileMgmtCd → objectURL). 인증 헤더가 필요해 axios blob 로 로드한다.
const blobUrls = ref({});

// 표시 라벨(순수 UI)
const fmtYmd = (v) => (v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : v || "-");
const riskLvLabel = (lv, l, s) => (lv ? `${lv}${l != null && s != null ? ` (${l}×${s})` : ""}` : "-");

// 첨부 URL — 캐시에 로드된 objectURL 을 돌려준다(없으면 빈 값).
const fileUrl = (fileMgmtCd) => blobUrls.value[fileMgmtCd] || "";

// 썸네일 클릭 시 원본을 라이트박스로 확대.
const fnOpenImage = (fileMgmtCd) => {
  const url = blobUrls.value[fileMgmtCd];
  if (url) viewerSrc.value = url;
};

// 캐시 초기화(objectURL 누수 방지).
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

// 첨부 1건을 blob 으로 로드해 objectURL 캐시에 넣는다(실패는 조용히 무시 — 썸네일만 비움).
const loadBlob = async (fileMgmtCd) => {
  if (!fileMgmtCd || blobUrls.value[fileMgmtCd]) return;
  try {
    const response = await axios.get("/webApi/subcon03/snapshot-file", {
      params: { snapshotId: props.snapshotId, fileMgmtCd },
      responseType: "blob",
    });
    blobUrls.value = { ...blobUrls.value, [fileMgmtCd]: URL.createObjectURL(response.data) };
  } catch (e) {
    /* 첨부 로드 실패는 비치명 — 썸네일 미표시 */
  }
};

// =========================== Methods ===========================
const fnLoad = async () => {
  rows.value = [];
  clearBlobs();
  if (props.snapshotId == null) return;

  try {
    const response = await axios.get("/webApi/subcon03/snapshot-risk-detail", {
      params: { snapshotId: props.snapshotId },
    });
    if (response.status === 200) {
      rows.value = response.data?.rows || [];

      // 화면에 걸린 첨부 코드(초기/재평가/개선항목)를 모두 blob 으로 선로드.
      const codes = new Set();
      rows.value.forEach((r) => {
        if (r.initFileMgmtCd) codes.add(r.initFileMgmtCd);
        if (r.revalFileMgmtCd) codes.add(r.revalFileMgmtCd);
        (r.improves || []).forEach((imp) => {
          if (imp.fileMgmtCd) codes.add(imp.fileMgmtCd);
        });
      });
      codes.forEach((cd) => loadBlob(cd));
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// =========================== Life Cycle ===========================
onMounted(fnLoad);
watch(() => props.snapshotId, fnLoad);
onBeforeUnmount(clearBlobs);
</script>

<style scoped>
.cell-strong { font-weight: 600; }
.cell-sub { color: var(--color-text-muted, #6b7280); font-size: var(--btn-font-sm, 12px); }
.improve-list { margin: 0; padding-left: var(--space-md, 1rem); }
.thumb {
  width: 40px; height: 40px; object-fit: cover;
  border-radius: var(--btn-radius, 8px); cursor: pointer;
}
.thumb--sm { width: 28px; height: 28px; vertical-align: middle; margin-left: var(--space-sm, 0.5rem); }
.image-viewer {
  position: fixed; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.7); z-index: 1000; cursor: zoom-out;
}
.image-viewer img { max-width: 90vw; max-height: 90vh; }
</style>
