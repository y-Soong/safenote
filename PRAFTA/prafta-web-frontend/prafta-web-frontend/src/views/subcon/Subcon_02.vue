<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenProposePop"
    />

    <div class="viewBody">
      <!-- 제공한/받은 연동 탭 (Attd_01 밑줄형 표준 — Subcon_01 승계) -->
      <div class="subcon02-tab-bar">
        <button :class="['subcon02-tab-btn', { active: activeTab === 'sent' }]" @click="activeTab = 'sent'">
          제공한 연동
        </button>
        <button :class="['subcon02-tab-btn', { active: activeTab === 'received' }]" @click="activeTab = 'received'">
          받은 연동
          <span v-if="receivedPendingCnt > 0" class="tab-badge">{{ receivedPendingCnt }}</span>
        </button>
      </div>

      <div class="table-wrapper subtitle-pane">
        <div class="table-box overflow-x-auto rounded-md border border-slate-300"
             style="--box-h: 66vh; --box-sticky-top: 1px; --box-ox: auto">
          <!-- 제공한 연동 -->
          <table v-show="activeTab === 'sent'" class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">No</th>
                <th>대상 회사코드</th>
                <th>회사명</th>
                <th>내 사업장</th>
                <th>상태</th>
                <th style="width: 90px">점검연동</th>
                <th>제안일시</th>
                <th>처리일시</th>
                <th>코멘트</th>
                <th class="event_cell" style="text-align: center; width: 200px">관리</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!sentList.length">
                <tr><td colspan="10" class="edu-grid-empty">제공한 연동이 없습니다.</td></tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in sentList" :key="row.linkId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.otherCmpnyCd }}</td>
                  <td>{{ row.otherCmpnyNm }}</td>
                  <td>{{ row.srcSiteNm }}</td>
                  <td style="text-align: center">
                    <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
                  </td>
                  <td style="text-align: center">
                    <span class="status-badge" :class="chkptLinkClass(row.chkptLinkStatus)">
                      {{ chkptLinkLabel(row.chkptLinkStatus) }}
                    </span>
                  </td>
                  <td>{{ row.insertDate }}</td>
                  <td>{{ row.processDtime }}</td>
                  <td class="comment-cell">{{ row.processComment }}</td>
                  <td style="text-align: center">
                    <button v-if="canProcess && row.status === 'PROPOSED'" class="btn btn-sm btn-primary" @click="fnCancel(row)">취소</button>
                    <button
                      v-if="canProcess && row.status === 'ACTIVE' && row.chkptLinkStatus !== 'ACTIVE'"
                      class="btn btn-sm btn-primary"
                      @click="fnOpenChkptLinkEnablePop(row)"
                    >
                      점검 연동
                    </button>
                    <button
                      v-if="canTerminate && row.status === 'ACTIVE' && row.chkptLinkStatus === 'ACTIVE'"
                      class="btn btn-sm"
                      @click="fnOpenChkptLinkDisablePop(row)"
                    >
                      점검 해제
                    </button>
                    <button v-if="canTerminate && row.status === 'ACTIVE'" class="btn btn-sm" @click="fnOpenTerminatePop(row)">해지</button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>

          <!-- 받은 연동 -->
          <table v-show="activeTab === 'received'" class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">No</th>
                <th>제공 회사코드</th>
                <th>회사명</th>
                <th>제공 사업장</th>
                <th>내 미러 사업장</th>
                <th>상태</th>
                <th style="width: 90px">점검연동</th>
                <th>제안일시</th>
                <th class="event_cell" style="text-align: center; width: 200px">관리</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!receivedList.length">
                <tr><td colspan="9" class="edu-grid-empty">받은 연동이 없습니다.</td></tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in receivedList" :key="row.linkId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.otherCmpnyCd }}</td>
                  <td>{{ row.otherCmpnyNm }}</td>
                  <td>{{ row.srcSiteNm }}</td>
                  <td>{{ row.dstSiteNm }}</td>
                  <td style="text-align: center">
                    <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
                  </td>
                  <td style="text-align: center">
                    <span class="status-badge" :class="chkptLinkClass(row.chkptLinkStatus)">
                      {{ chkptLinkLabel(row.chkptLinkStatus) }}
                    </span>
                  </td>
                  <td>{{ row.insertDate }}</td>
                  <td style="text-align: center">
                    <button v-if="canProcess && row.status === 'PROPOSED'" class="btn btn-sm btn-primary" @click="fnAccept(row)">수락</button>
                    <button v-if="canProcess && row.status === 'PROPOSED'" class="btn btn-sm" @click="fnOpenRejectPop(row)">거부</button>
                    <!-- 점검 연동 '실행'은 제공측 전용이라 받은 탭에는 없다. 해제는 양측 가능. -->
                    <button
                      v-if="canTerminate && row.status === 'ACTIVE' && row.chkptLinkStatus === 'ACTIVE'"
                      class="btn btn-sm"
                      @click="fnOpenChkptLinkDisablePop(row)"
                    >
                      점검 해제
                    </button>
                    <button v-if="canTerminate && row.status === 'ACTIVE'" class="btn btn-sm" @click="fnOpenTerminatePop(row)">해지</button>
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
import { ref, computed, defineProps, onMounted, getCurrentInstance, defineOptions } from "vue";
import { useModal } from "@/utils/useModal";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import SiteLinkProposePop from "@/components/popup/SiteLinkProposePop.vue";
import SiteLinkRejectPop from "@/components/popup/SiteLinkRejectPop.vue";
import SiteLinkTerminatePop from "@/components/popup/SiteLinkTerminatePop.vue";
import ChkptLinkEnablePop from "@/components/popup/ChkptLinkEnablePop.vue";
import ChkptLinkDisablePop from "@/components/popup/ChkptLinkDisablePop.vue";

// =========================== Define ===========================
defineOptions({ name: "Subcon_02" });
const props = defineProps({ title: String, buttons: Object });

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const activeTab = ref("sent");
const linkList = ref([]); // GET /webApi/subcon02/site-link-lists 원본

// direction 기준 2분류(전 상태 표시 = 목록이 곧 이력)
const sentList = computed(() => linkList.value.filter((r) => r.direction === "SENT"));
const receivedList = computed(() => linkList.value.filter((r) => r.direction === "RECEIVED"));
const receivedPendingCnt = computed(() => receivedList.value.filter((r) => r.status === "PROPOSED").length);

// 메뉴 버튼권한 → 액션 노출 (수락/거부/취소=save, 해지=delete — T1 매핑 승계)
const canProcess = computed(() => localButtons.value?.save === "Y");
const canTerminate = computed(() => localButtons.value?.delete === "Y");

// 상태 배지 라벨/클래스 [SYS079]
const statusLabel = (s) =>
  ({ PROPOSED: "제안중", ACTIVE: "연동중", REJECTED: "거부됨", CANCELLED: "취소됨", TERMINATED: "해지됨" }[s] || s);
const statusClass = (s) => ({ PROPOSED: "is-proposed", ACTIVE: "is-active" }[s] || "is-closed");

// 점검 구성 연동 상태 배지 (NONE/ACTIVE — SYS 코드 미사용, 화면 라벨 고정)
const chkptLinkLabel = (s) => (s === "ACTIVE" ? "연동중" : "미연동");
const chkptLinkClass = (s) => (s === "ACTIVE" ? "is-active" : "is-closed");

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// 전이 액션(수락/취소) 연타 방지 플래그(Subcon_01 패턴 승계).
const processing = ref(false);

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnSearch();
});

// =========================== Methods ===========================
// 목록 조회 — GET /webApi/subcon02/site-link-lists (회사 스코프는 서버 JWT 클레임).
//   조회 중 목록 비움. 배지/2분류는 computed 가 자동 갱신.
const fnSearch = async () => {
  linkList.value = [];

  try {
    const response = await axios.get("/webApi/subcon02/site-link-lists");

    if (response.status === 200) {
      linkList.value = response.data?.links || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 연동 제안 팝업 — 저장 성공 시 목록 재조회.
const fnOpenProposePop = () => {
  openPop(SiteLinkProposePop, {
    onSaved: fnSearch,
  });
};

// 수락 — POST /webApi/subcon02/site-link-accept { linkId }.
//   수락 시 사업장/근무타입이 우리 회사에 복제(미러)됨을 확인 후 진행.
const fnAccept = async (row) => {
  const ok = await proxy.$confirm(
    `'${row.srcSiteNm}' 연동을 수락하시겠습니까?\n사업장과 근무타입이 우리 회사에 복제됩니다.`
  );
  if (!ok) return;

  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon02/site-link-accept", {
      linkId: row.linkId,
    });

    if (response.status === 200) {
      await proxy.$alert("처리되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "수락 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
  }
};

// 거부 팝업 — 사유 입력(필수) 후 저장 성공 시 재조회.
const fnOpenRejectPop = (row) => {
  openPop(SiteLinkRejectPop, {
    linkId: row.linkId,
    onSaved: fnSearch,
  });
};

// 취소 — POST /webApi/subcon02/site-link-cancel { linkId }. 확인 후 진행, 성공 시 재조회.
const fnCancel = async (row) => {
  const ok = await proxy.$confirm("제안을 취소하시겠습니까?");
  if (!ok) return;

  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon02/site-link-cancel", {
      linkId: row.linkId,
    });

    if (response.status === 200) {
      await proxy.$alert("처리되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "취소 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
  }
};

// 해지 팝업 — 독립화 안내 표시 후 확정. 성공 시 재조회.
const fnOpenTerminatePop = (row) => {
  openPop(SiteLinkTerminatePop, {
    linkId: row.linkId,
    siteNm: row.dstSiteNm || row.srcSiteNm,
    onSaved: fnSearch,
  });
};

// 순회점검 구성 연동 실행 팝업 — 제공측(SENT) 전용. 복제/잠금/결과통합 안내 확인 후 실행.
const fnOpenChkptLinkEnablePop = (row) => {
  openPop(ChkptLinkEnablePop, {
    linkId: row.linkId,
    siteNm: row.srcSiteNm,
    onSaved: fnSearch,
  });
};

// 순회점검 구성 연동 해제 팝업 — 양측 가능. 독립화/결과통합 중단 안내 확인 후 해제.
const fnOpenChkptLinkDisablePop = (row) => {
  openPop(ChkptLinkDisablePop, {
    linkId: row.linkId,
    siteNm: row.dstSiteNm || row.srcSiteNm,
    onSaved: fnSearch,
  });
};
</script>

<style scoped>
/* 탭바 — Attd_01 밑줄형 표준(14px, Subcon_01 승계) */
.subcon02-tab-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.subcon02-tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.subcon02-tab-btn:hover {
  color: var(--color-text, #374151);
}
.subcon02-tab-btn.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}

/* 받은 연동 배지 */
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

/* 상태 배지 (Subcon_01 status-badge 패턴 승계) */
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

.comment-cell {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
