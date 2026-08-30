<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-normal tev-modal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- Title (드래그 영역) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>TBM 증빙자료 출력</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="viewBody tev-body">
          <!-- ── 조회 조건: 년도 / 반기 / 사업장 ── -->
          <div class="tev-cond">
            <div class="tev-cond__item">
              <label>년도</label>
              <select v-model="year">
                <option v-for="y in yearOptions" :key="y" :value="y">
                  {{ y }}년
                </option>
              </select>
            </div>
            <div class="tev-cond__item">
              <label>반기</label>
              <select v-model="half">
                <option value="H1">상반기 (1/1 ~ 6/30)</option>
                <option value="H2">하반기 (7/1 ~ 12/31)</option>
              </select>
            </div>
            <div class="tev-cond__item">
              <label>사업장</label>
              <select v-model="siteCd">
                <option value="">전체</option>
                <option v-for="s in siteOptions" :key="s.siteCd" :value="s.siteCd">
                  {{ s.siteNm }}
                </option>
              </select>
            </div>
            <button class="btn btn-primary btn-sm" :disabled="loading" @click="fnSearch">
              조회
            </button>
          </div>

          <p class="tev-note">
            연동(공유) 세션의 자사 근로자 참석분은 사업장 선택과 무관하게 포함됩니다.
            근로자별 이수현황은 세션 인정시간 기준으로 합산됩니다.
          </p>

          <!-- ── 결과: 세션 목록(건별 일지 체크 선택 — 기본 전체) ── -->
          <div v-if="loading" class="tev-state">조회 중…</div>
          <template v-else-if="searched">
            <div class="tev-summary">
              기간 <b>{{ fromDate }}</b> ~ <b>{{ toDate }}</b> · TBM
              <b>{{ sessions.length }}</b>건 · 교육일지 첨부
              <b>{{ checkedCount }}</b>건 선택
            </div>
            <div class="tev-table-wrap">
              <table class="tev-table">
                <thead>
                  <tr>
                    <th style="width: 34px">
                      <input
                        type="checkbox"
                        :checked="allChecked"
                        @change="fnToggleAll($event.target.checked)"
                        title="교육일지(건별) 전체 선택"
                      />
                    </th>
                    <th style="width: 90px">실시 일자</th>
                    <th>교육 제목</th>
                    <th style="width: 150px">개설사·사업장</th>
                    <th style="width: 76px">인정(분)</th>
                    <th style="width: 84px">참여/이수</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="sessions.length === 0">
                    <td colspan="6" class="tev-empty">
                      해당 반기에 종료된 TBM이 없습니다.
                    </td>
                  </tr>
                  <tr v-for="s in sessions" :key="s.sessionCd">
                    <td class="t-center">
                      <input type="checkbox" v-model="s._checked" />
                    </td>
                    <td class="t-center">{{ (s.endedAt || "").slice(0, 10) }}</td>
                    <td>{{ s.title }}</td>
                    <td>
                      <template v-if="s.sharedYn === 'Y'">
                        <span class="tev-badge">연동</span>
                        {{ s.hostCmpnyNm }} · {{ s.siteNm }}
                      </template>
                      <template v-else>{{ s.siteNm }}</template>
                    </td>
                    <td class="t-center">{{ s.eduMinutes }}</td>
                    <td class="t-center">
                      {{ s.attendanceCount }} / {{ s.completedCount }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
          <div v-else class="tev-state">조건 선택 후 [조회]를 누르세요.</div>

          <!-- 다운로드 진행률 -->
          <div v-if="building" class="tev-progress">
            {{ progressText }}
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-secondary" :disabled="building" @click="$emit('close')">
              닫기
            </button>
            <button
              class="btn btn-primary"
              :disabled="!searched || building || sessions.length === 0"
              @click="fnDownload"
            >
              {{ building ? "생성 중…" : "엑셀 다운로드" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { buildTbmEvidenceExcel } from "@/utils/tbmEvidenceExcel";

defineOptions({ name: "TbmEvidencePop" });
defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ── 조회 조건 ─────────────────────────────────────────────
const now = new Date();
const currentYear = now.getFullYear();
const yearOptions = Array.from({ length: 4 }, (_, i) => String(currentYear - i));
const year = ref(String(currentYear));
// 기본 반기 = 오늘 기준(1~6월 상반기 / 7~12월 하반기)
const half = ref(now.getMonth() + 1 <= 6 ? "H1" : "H2");
const siteCd = ref("");
const siteOptions = ref([]);

// ── 결과 상태 ─────────────────────────────────────────────
const loading = ref(false);
const searched = ref(false);
const sessions = ref([]); // 각 행에 _checked(건별 일지 첨부 여부 — 기본 전체 체크)
const fromDate = ref("");
const toDate = ref("");
const cmpnyNm = ref(""); // 서버 응답(TB_CMPNY) — 웹 세션에 회사명 미보유

const checkedCount = computed(() => sessions.value.filter((s) => s._checked).length);
const allChecked = computed(
  () => sessions.value.length > 0 && checkedCount.value === sessions.value.length,
);
const fnToggleAll = (checked) => {
  sessions.value.forEach((s) => (s._checked = checked));
};

// ── 다운로드 진행 상태 ────────────────────────────────────
const building = ref(false);
const progressText = ref("");

// 자사 사업장 옵션 로드(공용 EP — 빈 필터 = 전체).
const fnLoadSites = async () => {
  try {
    const r = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: "",
        siteNm: "",
      },
    });
    siteOptions.value = r.data?.siteInfoResultList ?? [];
  } catch (e) {
    console.warn("[TbmEvidence] 사업장 목록 로드 실패", e);
  }
};

// 반기 세션 목록 조회 — 기본 전체 체크(건당 증빙 필수 전제).
const fnSearch = async () => {
  loading.value = true;
  try {
    const r = await axios.get("/webApi/tbm04/evidence-sessions", {
      params: { year: year.value, half: half.value, siteCd: siteCd.value },
    });
    sessions.value = (r.data?.sessionList ?? []).map((s) => ({ ...s, _checked: true }));
    fromDate.value = r.data?.fromDate ?? "";
    toDate.value = r.data?.toDate ?? "";
    cmpnyNm.value = r.data?.cmpnyNm ?? "";
    searched.value = true;
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  } finally {
    loading.value = false;
  }
};

// 엑셀 생성 — 근로자 집계 1회 + 체크 세션 상세를 50건 청크로 수집(진행률 표시) 후
// 클라이언트(exceljs)에서 통합 워크북 생성(서버는 JSON 만 공급 — 부하 회피 확정안).
const DETAIL_CHUNK = 50;
const fnDownload = async () => {
  if (building.value) return;
  building.value = true;
  progressText.value = "근로자별 이수현황 집계 중…";
  try {
    const workerRes = await axios.get("/webApi/tbm04/evidence-worker-summary", {
      params: { year: year.value, half: half.value, siteCd: siteCd.value },
    });
    const workers = workerRes.data?.workerList ?? [];

    // 체크된 세션의 건별 상세를 청크 수집.
    const checkedCds = sessions.value.filter((s) => s._checked).map((s) => s.sessionCd);
    const details = { sessionList: [], attendeeList: [], riskList: [], mtrlList: [] };
    for (let i = 0; i < checkedCds.length; i += DETAIL_CHUNK) {
      progressText.value = `교육일지 생성 중… (${Math.min(i + DETAIL_CHUNK, checkedCds.length)}/${checkedCds.length})`;
      const chunk = checkedCds.slice(i, i + DETAIL_CHUNK);
      const r = await axios.post("/webApi/tbm04/evidence-session-details", {
        sessionCds: chunk,
      });
      details.sessionList.push(...(r.data?.sessionList ?? []));
      details.attendeeList.push(...(r.data?.attendeeList ?? []));
      details.riskList.push(...(r.data?.riskList ?? []));
      details.mtrlList.push(...(r.data?.mtrlList ?? []));
    }

    progressText.value = "엑셀 파일 생성 중…";
    const halfLabel = half.value === "H1" ? "상반기" : "하반기";
    const siteNm =
      siteCd.value === ""
        ? "전체"
        : (siteOptions.value.find((s) => s.siteCd === siteCd.value)?.siteNm ?? siteCd.value);
    const cmpnyCd = sessionStorage.getItem("gv_cmpnyCd") || "";

    await buildTbmEvidenceExcel({
      cmpnyLabel: cmpnyNm.value ? `${cmpnyNm.value} (${cmpnyCd})` : cmpnyCd,
      siteLabel: siteNm,
      periodLabel: `${year.value}년 ${halfLabel}`,
      fromDate: fromDate.value,
      toDate: toDate.value,
      sessions: sessions.value,
      workers,
      details,
      fileName: `TBM_안전교육_증빙_${year.value}_${halfLabel}.xlsx`,
    });
  } catch (err) {
    console.error("[TbmEvidence] 증빙 생성 실패", err);
    await proxy.$alert(
      resolveApiErrorMessage(err, "증빙자료 생성 중 오류가 발생했습니다."),
    );
  } finally {
    building.value = false;
    progressText.value = "";
  }
};

onMounted(fnLoadSites);
</script>

<style scoped>
.tev-modal {
  width: min(760px, 94vw);
}

.tev-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* 조회 조건: 필터 바 (연한 패널) */
.tev-cond {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
  padding: 0.75rem 0.875rem;
  background: var(--color-bg, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
}
.tev-cond__item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.tev-cond__item:nth-of-type(3) {
  flex: 1 1 160px;
}
.tev-cond__item label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted, #4b5563);
}
.tev-cond__item select {
  height: 34px;
  min-width: 140px;
  width: 100%;
  padding: 0 0.5rem;
  background: var(--card-bg, #ffffff);
  border: 1px solid var(--color-border-strong, #d1d5db);
  border-radius: var(--input-radius, 8px);
  font-size: 0.8125rem;
  color: var(--color-text-strong, #111827);
}
.tev-cond__item select:focus {
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}
.tev-cond > .btn {
  height: 34px;
  min-height: 34px;
  padding: 0 1rem;
}

.tev-note {
  margin: 0;
  font-size: 0.6875rem;
  line-height: 1.5;
  color: var(--color-text-muted, #4b5563);
  word-break: keep-all;
  padding-left: 0.25rem;
}

.tev-state {
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
  border: 1px dashed var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
}

.tev-summary {
  font-size: 0.8125rem;
  color: var(--color-text, #374151);
  margin-bottom: -0.375rem;
}
.tev-summary b {
  color: var(--color-text-strong, #111827);
}

.tev-table-wrap {
  overflow-x: auto;
  max-height: 40vh;
  overflow-y: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 8px);
}
.tev-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}
.tev-table th {
  position: sticky;
  top: 0;
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #4b5563);
  font-weight: 600;
  text-align: center;
  padding: 0.375rem 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  white-space: nowrap;
}
.tev-table td {
  padding: 0.375rem 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
  word-break: keep-all;
}
.tev-table tbody tr:last-child td {
  border-bottom: none;
}
.t-center {
  text-align: center;
}
.tev-empty {
  text-align: center;
  color: var(--color-text-muted, #4b5563);
  padding: 0.75rem;
}

/* 연동(공유) 세션 배지 */
.tev-badge {
  display: inline-block;
  padding: 0.05rem 0.4rem;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 600;
  background: #e0e7ff;
  color: #3730a3;
  margin-right: 0.25rem;
}

.tev-progress {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  text-align: center;
}
</style>
