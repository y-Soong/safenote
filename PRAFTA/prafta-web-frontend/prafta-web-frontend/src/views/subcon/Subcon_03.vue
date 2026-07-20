<template>
  <div class="viewComm subcon03-container">
    <!-- 보낸/받은 탭 (Attd_01 표준 — 화면 최상단 밑줄형 탭바) -->
    <div class="subcon03-tab-bar">
      <button
        type="button"
        :class="['subcon03-tab-btn', { active: activeTab === 'sent' }]"
        @click="activeTab = 'sent'"
      >
        보낸 요청
      </button>
      <button
        type="button"
        :class="['subcon03-tab-btn', { active: activeTab === 'received' }]"
        @click="activeTab = 'received'"
      >
        받은 요청
        <span v-if="receivedPendingCnt > 0" class="tab-badge">{{
          receivedPendingCnt
        }}</span>
      </button>
    </div>

    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenCreatePop"
    />

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 66vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <!-- 보낸 요청 -->
          <table
            v-show="activeTab === 'sent'"
            class="data-grid w-full table-fixed text-sm text-left"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th>제공 회사</th>
                <th style="width: 7%">유형</th>
                <th>대상 사업장</th>
                <th>기간</th>
                <th style="width: 6%">마감만</th>
                <th style="width: 8%">상태</th>
                <th>요청일시</th>
                <th style="width: 6%">버전</th>
                <th class="event_cell" style="text-align: center; width: 90px">
                  관리
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!sentList.length">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    보낸 요청이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sentList" :key="row.shareReqId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.otherCmpnyNm }}</td>
                  <td style="text-align: center">
                    {{ dataTypeLabel(row.dataType) }}
                  </td>
                  <td>{{ row.siteNm }}</td>
                  <td>{{ periodLabel(row) }}</td>
                  <td style="text-align: center">
                    {{ row.closedOnlyYn === "Y" ? "Y" : "N" }}
                  </td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="statusClass(row.status)"
                      >{{ statusLabel(row.status) }}</span
                    >
                  </td>
                  <td>{{ row.insertDate }}</td>
                  <td style="text-align: center">
                    {{ row.snapshotVersion ? "v" + row.snapshotVersion : "-" }}
                  </td>
                  <td style="text-align: center">
                    <button
                      v-if="canProcess && row.status === 'REQUESTED'"
                      class="btn btn-sm"
                      @click="fnCancel(row)"
                    >
                      취소
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>

          <!-- 받은 요청 -->
          <table
            v-show="activeTab === 'received'"
            class="data-grid w-full table-fixed text-sm text-left"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th>요청 회사</th>
                <th style="width: 7%">유형</th>
                <th>대상 사업장</th>
                <th>기간</th>
                <th style="width: 6%">마감만</th>
                <th>제공 목적</th>
                <th style="width: 8%">상태</th>
                <th>요청일시</th>
                <th class="event_cell" style="text-align: center; width: 130px">
                  관리
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!receivedList.length">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    받은 요청이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in receivedList" :key="row.shareReqId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.otherCmpnyNm }}</td>
                  <td style="text-align: center">
                    {{ dataTypeLabel(row.dataType) }}
                  </td>
                  <td>{{ row.siteNm }}</td>
                  <td>{{ periodLabel(row) }}</td>
                  <td style="text-align: center">
                    {{ row.closedOnlyYn === "Y" ? "Y" : "N" }}
                  </td>
                  <td class="purpose-cell">{{ row.purpose }}</td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="statusClass(row.status)"
                      >{{ statusLabel(row.status) }}</span
                    >
                  </td>
                  <td>{{ row.insertDate }}</td>
                  <td style="text-align: center">
                    <button
                      v-if="canProcess && row.status === 'REQUESTED'"
                      class="btn btn-sm btn-primary"
                      @click="fnOpenApprovePop(row)"
                    >
                      승인
                    </button>
                    <button
                      v-if="canProcess && row.status === 'REQUESTED'"
                      class="btn btn-sm"
                      @click="fnOpenRejectPop(row)"
                    >
                      거부
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import ShareReqCreatePop from "@/views/subcon/popup/ShareReqCreatePop.vue";
import ShareReqApprovePop from "@/views/subcon/popup/ShareReqApprovePop.vue";
import ShareReqRejectPop from "@/views/subcon/popup/ShareReqRejectPop.vue";

// =========================== Define ===========================
defineOptions({ name: "Subcon_03" });
const props = defineProps({ title: String, buttons: Object });

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const activeTab = ref("sent");
const reqList = ref([]); // GET /webApi/subcon03/share-req-lists 원본

// direction 기준 2분류(전 상태 표시 = 목록이 곧 이력 — D9)
const sentList = computed(() =>
  reqList.value.filter((r) => r.direction === "SENT")
);
const receivedList = computed(() =>
  reqList.value.filter((r) => r.direction === "RECEIVED")
);
const receivedPendingCnt = computed(
  () => receivedList.value.filter((r) => r.status === "REQUESTED").length
);

// 메뉴 버튼권한 → 액션 노출 (승인/거부/취소 = save)
const canProcess = computed(() => localButtons.value?.save === "Y");

// 라벨 [SYS077 / SYS078]
const dataTypeLabel = (t) => ({ ATTD: "근태" })[t] || t;
const statusLabel = (s) =>
  ({
    REQUESTED: "요청중",
    APPROVED: "승인",
    REJECTED: "거부됨",
    CANCELLED: "취소됨",
  })[s] || s;
const statusClass = (s) =>
  ({ REQUESTED: "is-proposed", APPROVED: "is-active" })[s] || "is-closed";

// 기간 표시 (YYYYMMDD → YYYY-MM-DD)
const fmtYmd = (v) =>
  v && v.length === 8
    ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}`
    : v || "";
const periodLabel = (row) =>
  `${fmtYmd(row.periodStr)} ~ ${fmtYmd(row.periodEnd)}`;

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// 전이 액션(취소) 연타 방지 플래그(Subcon_02 패턴 승계).
const processing = ref(false);

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnSearch();
});

// =========================== Methods ===========================
// 목록 조회 — GET /webApi/subcon03/share-req-lists (회사 스코프는 서버 JWT 클레임).
//   조회 중 목록 비움. 배지/2분류는 computed 가 자동 갱신.
const fnSearch = async () => {
  reqList.value = [];

  try {
    const response = await axios.get("/webApi/subcon03/share-req-lists");

    if (response.status === 200) {
      reqList.value = response.data?.reqs || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 공유 요청 생성 팝업 — 저장 성공 시 목록 재조회.
const fnOpenCreatePop = () => {
  openPop(ShareReqCreatePop, {
    onSaved: fnSearch,
  });
};

// 승인 팝업 — 팝업이 approve-info(마감 상태·미마감 월·릴레이 후보) 조회 후 승인까지 처리.
const fnOpenApprovePop = (row) => {
  openPop(ShareReqApprovePop, {
    shareReqId: row.shareReqId,
    onSaved: fnSearch,
  });
};

// 거부 팝업 — 사유 입력(필수) 후 저장 성공 시 재조회.
const fnOpenRejectPop = (row) => {
  openPop(ShareReqRejectPop, {
    shareReqId: row.shareReqId,
    onSaved: fnSearch,
  });
};

// 취소 — POST /webApi/subcon03/share-req-cancel { shareReqId }. 요청측만 가능(서버 강제).
const fnCancel = async (row) => {
  const ok = await proxy.$confirm("요청을 취소하시겠습니까?");
  if (!ok) return;

  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon03/share-req-cancel", {
      shareReqId: row.shareReqId,
    });

    if (response.status === 200) {
      await proxy.$alert("처리되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(
      err,
      "취소 처리 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
  }
};
</script>

<style scoped>
/* Attd_01 표준 — 탭바를 화면 최상단(헤더 위)에 두는 컨테이너 구조 */
.subcon03-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 탭바 — Attd_01 밑줄형 표준(14px) */
.subcon03-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.subcon03-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.subcon03-tab-btn:hover {
  color: var(--color-text, #374151);
}
.subcon03-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}

.tab-badge {
  display: inline-block;
  min-width: 1.25rem;
  margin-left: 0.25rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  background: var(--color-primary, #16a34a);
  color: #fff;
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4rem;
  text-align: center;
}

/* 상태 배지 (Subcon_01/02 패턴 승계) */
.status-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-proposed {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-active {
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
}
.status-badge.is-closed {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}

.purpose-cell {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
