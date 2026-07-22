<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @create="fnOpenReqPop"
    />

    <div class="viewBody">
      <!-- 연동 중인 회사 (ACCEPTED) -->
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-text">연동 중인 회사</span>
          </div>
          <div class="custom-btn-area">
            <button class="btn btn-custom" @click="fnCopyMyCmpnyCd">
              내 회사코드 복사
            </button>
          </div>
        </div>
        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 30vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th>회사코드</th>
                <th>회사명</th>
                <th>연동일시</th>
                <th class="event_cell" style="text-align: center; width: 140px">
                  관리
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!acceptedList.length">
                <tr>
                  <td colspan="5" class="edu-grid-empty">
                    연동 중인 회사가 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in acceptedList" :key="row.relationId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.otherCmpnyCd }}</td>
                  <td>{{ row.otherCmpnyNm }}</td>
                  <td>{{ row.processDtime }}</td>
                  <td style="text-align: center">
                    <button
                      v-if="canTerminate"
                      class="btn btn-sm btn-primary"
                      @click="fnOpenTerminatePop(row)"
                    >
                      해지
                    </button>
                    <button class="btn btn-sm" @click="fnOpenHistPop(row)">
                      이력
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 요청 내역 (보낸/받은 통합 — 상태 라벨에 방향 반영) -->
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-text">요청 내역</span>
            <span v-if="receivedPendingCnt > 0" class="pending-count"
              >수락 대기 {{ receivedPendingCnt }}건</span
            >
          </div>
        </div>
        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 38vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 4%">
                  No
                </th>
                <th>상대 회사코드</th>
                <th>회사명</th>
                <th>상태</th>
                <th>요청일시</th>
                <th>처리일시</th>
                <th>코멘트</th>
                <th class="event_cell" style="text-align: center; width: 180px">
                  관리
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!requestList.length">
                <tr>
                  <td colspan="8" class="edu-grid-empty">
                    요청 내역이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(row, idx) in requestList" :key="row.relationId">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ row.otherCmpnyCd }}</td>
                  <td>{{ row.otherCmpnyNm }}</td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="statusClass(row.status)"
                      >{{ statusLabel(row) }}</span
                    >
                  </td>
                  <td>{{ row.insertDate }}</td>
                  <td>{{ row.processDtime }}</td>
                  <td class="comment-cell">{{ row.processComment }}</td>
                  <td style="text-align: center">
                    <template v-if="row.status === 'REQUESTED' && row.direction === 'SENT'">
                      <button
                        v-if="canProcess"
                        class="btn btn-sm btn-primary"
                        @click="fnCancel(row)"
                      >
                        취소
                      </button>
                    </template>
                    <template v-else-if="row.status === 'REQUESTED' && row.direction === 'RECEIVED'">
                      <button
                        v-if="canProcess"
                        class="btn btn-sm btn-primary"
                        @click="fnAccept(row)"
                      >
                        수락
                      </button>
                      <button
                        v-if="canProcess"
                        class="btn btn-sm"
                        @click="fnOpenRejectPop(row)"
                      >
                        거부
                      </button>
                    </template>
                    <button class="btn btn-sm" @click="fnOpenHistPop(row)">
                      이력
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
import { ref, computed, defineProps, onMounted, getCurrentInstance, defineOptions } from "vue";
import { useModal } from "@/utils/useModal";
import { resolveApiErrorMessage } from "@/utils/apiError";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CmpnyRelationReqPop from "@/views/subcon/popup/CmpnyRelationReqPop.vue";
import CmpnyRelationRejectPop from "@/views/subcon/popup/CmpnyRelationRejectPop.vue";
import CmpnyRelationTerminatePop from "@/views/subcon/popup/CmpnyRelationTerminatePop.vue";
import CmpnyRelationHistPop from "@/views/subcon/popup/CmpnyRelationHistPop.vue";

// =========================== Define ===========================
defineOptions({ name: "Subcon_01" });
const props = defineProps({ title: String, buttons: Object });

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const relationList = ref([]); // GET /webApi/subcon01/relation-lists 원본

// 2분류 — 연동중(ACCEPTED)은 상단 목록, 나머지는 하단 요청 내역 통합 목록.
// 보낸/받은 구분은 탭이 아닌 상태 라벨(statusLabel)에 방향을 녹여 표현.
const acceptedList = computed(() => relationList.value.filter((r) => r.status === "ACCEPTED"));
// 정렬: 내가 응답해야 하는 건(받은 요청중)을 최상단에, 그다음 보낸 요청중, 나머지는 최신순.
const requestList = computed(() =>
  relationList.value
    .filter((r) => r.status !== "ACCEPTED")
    .slice()
    .sort((a, b) => {
      const priority = (r) =>
        r.status !== "REQUESTED" ? 2 : r.direction === "RECEIVED" ? 0 : 1;
      const diff = priority(a) - priority(b);
      if (diff !== 0) return diff;
      return (b.insertDate || "").localeCompare(a.insertDate || "");
    })
);
const receivedPendingCnt = computed(
  () => relationList.value.filter((r) => r.direction === "RECEIVED" && r.status === "REQUESTED").length
);

// 메뉴 버튼권한 → 액션 노출 (§5 매핑: 수락/거부/취소=save, 해지=delete)
const canProcess = computed(() => localButtons.value?.save === "Y");
const canTerminate = computed(() => localButtons.value?.delete === "Y");

// 상태 라벨 — REQUESTED는 방향에 따라 "요청중"(보냄)/"수락 대기"(받음)로 분리, 그 외는 상태만으로 확정.
const statusLabel = (row) => {
  if (row.status === "REQUESTED") {
    return row.direction === "RECEIVED" ? "수락 대기" : "요청중";
  }
  return { ACCEPTED: "연동중", REJECTED: "거부됨", CANCELLED: "취소됨", TERMINATED: "해지됨" }[row.status] || row.status;
};
const statusClass = (s) => ({ REQUESTED: "is-requested", ACCEPTED: "is-accepted" }[s] || "is-closed");

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// 전이 액션(수락/취소) 연타 방지 플래그.
const processing = ref(false);

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnSearch();
});

// =========================== Methods ===========================
// 목록 조회 — GET /webApi/subcon01/relation-lists (회사 스코프는 서버 JWT 클레임).
//   조회 중 목록 비움(User_06 fnSearch 패턴). 배지/3분류는 computed 가 자동 갱신.
const fnSearch = async () => {
  relationList.value = [];

  try {
    const response = await axios.get("/webApi/subcon01/relation-lists");

    if (response.status === 200) {
      relationList.value = response.data?.relations || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 내 회사코드 복사 — 상대사에게 연동 요청용으로 알려줄 때 사용. 회사코드는 로그인 시 세션에 적재된 값.
const fnCopyMyCmpnyCd = async () => {
  const cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");

  if (!cmpnyCd) {
    await proxy.$alert("회사코드를 확인할 수 없습니다. 다시 로그인해 주세요.");
    return;
  }

  try {
    await navigator.clipboard.writeText(cmpnyCd);
    await proxy.$alert("회사코드가 복사되었습니다.");
  } catch (err) {
    await proxy.$alert(
      "회사코드 복사에 실패했습니다: " + (err?.message || "알 수 없는 오류")
    );
  }
};

// 연동 요청 팝업 — 저장 성공 시 목록 재조회.
const fnOpenReqPop = () => {
  openPop(CmpnyRelationReqPop, {
    onSaved: fnSearch,
  });
};

// 수락 — POST /webApi/subcon01/relation-accept { relationId }. 확인 후 진행, 성공 시 재조회.
const fnAccept = async (row) => {
  const ok = await proxy.$confirm(`'${row.otherCmpnyNm}'의 연동 요청을 수락하시겠습니까?`);
  if (!ok) return;

  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon01/relation-accept", {
      relationId: row.relationId,
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

// 거부 팝업 — 사유 입력 후 저장 성공 시 재조회.
const fnOpenRejectPop = (row) => {
  openPop(CmpnyRelationRejectPop, {
    relationId: row.relationId,
    onSaved: fnSearch,
  });
};

// 취소 — POST /webApi/subcon01/relation-cancel { relationId }. 확인 후 진행, 성공 시 재조회.
const fnCancel = async (row) => {
  const ok = await proxy.$confirm("연동 요청을 취소하시겠습니까?");
  if (!ok) return;

  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon01/relation-cancel", {
      relationId: row.relationId,
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

// 해지 팝업 — 영향 요약(terminate-summary)은 팝업이 조회. 성공 시 재조회.
const fnOpenTerminatePop = (row) => {
  openPop(CmpnyRelationTerminatePop, {
    relationId: row.relationId,
    otherCmpnyNm: row.otherCmpnyNm,
    onSaved: fnSearch,
  });
};

// 이력 팝업 — 상대사 행위자는 서버가 "상대사 처리"로 마스킹하여 내려준다.
const fnOpenHistPop = (row) => {
  openPop(CmpnyRelationHistPop, {
    relationId: row.relationId,
    otherCmpnyNm: row.otherCmpnyNm,
  });
};
</script>

<style scoped>
/* 소제목 우측 — 수락 대기 건수(플랫폼 화면 p03-count 패턴 참고) */
.pending-count {
  margin-left: 0.5rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  font-size: 0.78rem;
  font-weight: 600;
}

/* 상태 배지 (User_06 status-badge 패턴) */
.status-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  line-height: 1.4;
}
.status-badge.is-requested {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-accepted {
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
}
.status-badge.is-closed {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-muted, #4b5563);
}

/* 코멘트 셀 — 길면 줄바꿈 */
.comment-cell {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
