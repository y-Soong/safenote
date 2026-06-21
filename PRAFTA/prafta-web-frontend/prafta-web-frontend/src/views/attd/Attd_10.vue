<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title || '요청 승인 관리'"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 탭 (재기획서 §5.3) -->
    <div class="ra-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="ra-tab"
        :class="{ active: activeTab === t.key }"
        @click="fnSelectTab(t.key)"
      >
        {{ t.label }}
        <span v-if="tabCount(t.key) > 0" class="ra-tab-badge">
          {{ tabCount(t.key) }}
        </span>
      </button>
    </div>

    <div class="viewBody ra-body">
      <!-- 연차 탭: 2분할 (접수함 / 상세) -->
      <template v-if="activeTab === 'leave'">
        <!-- 접수함 -->
        <section class="ra-inbox">
          <div class="ra-inbox__head">
            내 결재 대기 ({{ approvalList.length }})
          </div>
          <div class="ra-list">
            <div v-if="approvalList.length === 0" class="ra-empty">
              대기 중인 연차 결재가 없습니다.
            </div>
            <div
              v-for="row in approvalList"
              :key="row.reqId + '-' + row.approvalStep"
              class="ra-row"
              :class="{ selected: selected && selected.reqId === row.reqId }"
              @click="fnSelect(row)"
            >
              <div class="ra-row__main">
                <span class="ra-row__name">{{ row.requesterUserNm }}</span>
                <span class="ra-row__dept">{{ row.nodeNm || "-" }}</span>
                <span v-if="row.selfYn === 'Y'" class="ra-chip self">본인</span>
              </div>
              <div class="ra-row__sub">
                {{ fmtDate(row.workYmd) }} · {{ row.unitNm || row.leaveType }} ·
                {{ Number(row.leaveDays) }}일
              </div>
            </div>
          </div>
        </section>

        <!-- 상세 패널 (§5.8.4) -->
        <section class="ra-detail">
          <div v-if="!selected" class="ra-detail__empty">
            좌측에서 결재 건을 선택하세요.
          </div>
          <template v-else>
            <div class="ra-sec">
              <div class="ra-sec__title">요청 정보</div>
              <dl class="ra-meta">
                <dt>요청번호</dt>
                <dd>{{ selected.reqId }}</dd>
                <dt>요청자</dt>
                <dd>
                  {{ selected.requesterUserNm }} ({{
                    selected.requesterUserCd
                  }})
                  <span v-if="selected.selfYn === 'Y'" class="ra-chip self"
                    >본인 신청</span
                  >
                </dd>
                <dt>소속</dt>
                <dd>{{ selected.nodeNm || "-" }}</dd>
                <dt>대상일자</dt>
                <dd>{{ fmtDate(selected.workYmd) }}</dd>
                <dt>요청일시</dt>
                <dd>{{ (selected.reqDate || "").replace("T", " ") }}</dd>
                <dt>결재 단계</dt>
                <dd>{{ selected.approvalStep }}단계</dd>
              </dl>
            </div>

            <div class="ra-sec">
              <div class="ra-sec__title">연차 내용</div>
              <dl class="ra-meta">
                <dt>연차 타입</dt>
                <dd>{{ leaveTypeLabel(selected) }}</dd>
                <dt>사용 단위</dt>
                <dd>{{ selected.unitNm || "-" }}</dd>
                <dt>사용 일수</dt>
                <dd>{{ Number(selected.leaveDays) }}일</dd>
                <dt v-if="selected.startTime">시간대</dt>
                <dd v-if="selected.startTime">
                  {{ fmtTime(selected.startTime) }} ~
                  {{ fmtTime(selected.endTime) }} ({{
                    selected.leaveMinutes
                  }}분)
                </dd>
                <dt>사유</dt>
                <dd>{{ selected.reqReason || "-" }}</dd>
              </dl>
            </div>

            <!-- 관리자 결정 (§5.8.4 — 연차는 라디오 2개) -->
            <div class="ra-sec ra-decide">
              <div class="ra-sec__title">결재 처리</div>
              <label class="ra-radio">
                <input type="radio" v-model="decision" value="approve" />
                요청대로 승인
              </label>
              <label class="ra-radio">
                <input type="radio" v-model="decision" value="reject" />
                반려
              </label>
              <textarea
                v-if="decision === 'reject'"
                v-model="rejectReason"
                rows="2"
                maxlength="500"
                placeholder="반려 사유 (필수)"
              />
              <div class="ra-decide__actions">
                <button
                  class="btn-process"
                  :disabled="processing"
                  @click="fnProcess"
                >
                  처리하기
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>

      <!-- 근태 보정 / 초과근무: 통합 대기요청 접수함 + 인라인 반려 -->
      <template
        v-else-if="activeTab === 'correction' || activeTab === 'overtime'"
      >
        <!-- 접수함 -->
        <section class="ra-inbox">
          <div class="ra-inbox__head">대기 요청 ({{ reqList.length }})</div>
          <div class="ra-list">
            <div v-if="reqList.length === 0" class="ra-empty">
              대기 중인 {{ tabLabel(activeTab) }} 요청이 없습니다.
            </div>
            <div
              v-for="row in reqList"
              :key="row.reqId"
              class="ra-row"
              :class="{
                selected: reqSelected && reqSelected.reqId === row.reqId,
              }"
              @click="fnSelectReq(row)"
            >
              <div class="ra-row__main">
                <span class="ra-row__name">{{ row.userNm || row.userCd }}</span>
                <span class="ra-row__dept">{{ row.nodeNm || "-" }}</span>
                <span class="ra-chip type">{{ reqTypeNm(row.reqType) }}</span>
              </div>
              <div class="ra-row__sub">
                {{ fmtDate(row.workYmd || row.startDate) }} ·
                {{ fmtTime(row.startTime)
                }}<template v-if="row.endTime">
                  ~ {{ fmtTime(row.endTime) }}</template
                >
              </div>
            </div>
          </div>
        </section>

        <!-- 상세 + 반려 처리 -->
        <section class="ra-detail">
          <div v-if="!reqSelected" class="ra-detail__empty">
            좌측에서 요청 건을 선택하세요.
          </div>
          <template v-else>
            <div class="ra-sec">
              <div class="ra-sec__title">요청 정보</div>
              <dl class="ra-meta">
                <dt>요청번호</dt>
                <dd>{{ reqSelected.reqId }}</dd>
                <dt>요청자</dt>
                <dd>
                  {{ reqSelected.userNm || "-" }} ({{ reqSelected.userCd }})
                </dd>
                <dt>소속</dt>
                <dd>{{ reqSelected.nodeNm || "-" }}</dd>
                <dt>요청유형</dt>
                <dd>{{ reqTypeNm(reqSelected.reqType) }}</dd>
                <dt v-if="reqSelected.workYmd">대상일자</dt>
                <dd v-if="reqSelected.workYmd">
                  {{ fmtDate(reqSelected.workYmd) }}
                  <template v-if="reqSelected.workSeq">
                    · {{ reqSelected.workSeq }}차</template
                  >
                </dd>
                <dt v-if="reqSelected.startDate">시작</dt>
                <dd v-if="reqSelected.startDate">
                  {{ fmtDate(reqSelected.startDate) }}
                  {{ fmtTime(reqSelected.startTime) }}
                </dd>
                <dt v-if="reqSelected.endDate">종료</dt>
                <dd v-if="reqSelected.endDate">
                  {{ fmtDate(reqSelected.endDate) }}
                  {{ fmtTime(reqSelected.endTime) }}
                </dd>
                <dt>요청일시</dt>
                <dd>{{ (reqSelected.reqDate || "").replace("T", " ") }}</dd>
                <dt>사유</dt>
                <dd>{{ reqSelected.reqReason || "-" }}</dd>
              </dl>
            </div>

            <!-- 결재 처리 — 요청대로 승인 / 반려 (연차 탭과 동일 패턴). 편의상 본 화면에서도 승인 가능. -->
            <div class="ra-sec ra-decide">
              <div class="ra-sec__title">결재 처리</div>
              <label class="ra-radio">
                <input type="radio" v-model="reqDecision" value="approve" />
                요청대로 승인
              </label>
              <label class="ra-radio">
                <input type="radio" v-model="reqDecision" value="reject" />
                반려
              </label>
              <p v-if="reqDecision === 'approve'" class="ra-decide__note">
                {{ approveScreenNm(activeTab) }}와 동일하게
                처리됩니다(근태/초과근무 기록 반영 + 요청 승인).
              </p>
              <textarea
                v-if="reqDecision === 'reject'"
                v-model="reqRejectReason"
                rows="2"
                maxlength="500"
                placeholder="반려 사유 (필수)"
              />
              <div class="ra-decide__actions">
                <button
                  class="btn-process"
                  :class="{ 'btn-reject': reqDecision === 'reject' }"
                  :disabled="reqProcessing"
                  @click="fnProcessReq"
                >
                  처리하기
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineOptions,
} from "vue";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

defineOptions({ name: "Attd_10" });
const props = defineProps({
  title: String,
  buttons: Object,
});
const { proxy } = getCurrentInstance();

const localButtons = ref({ ...props.buttons });
const tabs = [
  { key: "correction", label: "근태 보정" },
  { key: "overtime", label: "초과근무 상신" },
  { key: "leave", label: "연차 상신" },
];
const activeTab = ref("leave");
const approvalList = ref([]);
const selected = ref(null);
const decision = ref("approve");
const rejectReason = ref("");
const processing = ref(false);

// 근태 보정 / 초과근무 통합 대기요청 접수함
const reqList = ref([]);
const reqSelected = ref(null);
const reqDecision = ref("approve"); // approve | reject
const reqRejectReason = ref("");
const reqProcessing = ref(false);

// 탭별 대기 건수 (배지용) — 활성 탭과 무관하게 유지
const leaveCount = ref(0);
const correctionCount = ref(0);
const overtimeCount = ref(0);

const tabLabel = (key) => tabs.find((t) => t.key === key)?.label ?? "";

// 탭별 대기 건수 (배지용). 0이면 배지 미표시.
const tabCount = (key) => {
  if (key === "leave") return leaveCount.value;
  if (key === "correction") return correctionCount.value;
  if (key === "overtime") return overtimeCount.value;
  return 0;
};

// REQ_TYPE(SYS032) 표시명 — 본 화면에서 다루는 값만.
const reqTypeNm = (t) =>
  ({ "01": "근태 생성", "02": "근태 수정", "03": "초과근무 생성" }[t] || t || "-");

// 승인을 처리하는 원 화면명 (반려 안내용).
const approveScreenNm = (tab) =>
  tab === "overtime" ? "초과근무 관리" : "근태 관리";

// 탭 → reqTypeGroup 매핑 (백엔드 /reqinbox/pending).
const reqTypeGroupOf = (tab) =>
  tab === "overtime" ? "overtime" : "correction";

// 표시용 날짜 포맷은 dateFormat 단일 출처에 위임(점). 빈값/형식불충분은 "-".
const fmtDate = (ymd) => {
  if (!ymd || ymd.length < 8) return ymd || "-";
  return formatYmdDot(ymd);
};
const fmtTime = (hhmm) => {
  if (!hhmm || hhmm.length < 4) return hhmm || "";
  return `${hhmm.slice(0, 2)}:${hhmm.slice(2, 4)}`;
};

// 연차 타입 표시 — 연차명(연차번호) 형태. 일부 누락 시 가능한 값만, 모두 없으면 leaveCd → "-".
const leaveTypeLabel = (row) => {
  if (!row) return "-";
  const nm = row.leaveNm;
  const no = row.leaveNo;
  if (nm && no) return `${nm}(${no})`;
  return nm || no || row.leaveCd || "-";
};

const fnSelectTab = (key) => {
  activeTab.value = key;
  fnLoad();
};

// 탭 전환/내부 재조회 진입점 — 활성 탭 데이터 로드.
const fnLoad = async () => {
  if (activeTab.value === "leave") return fnLoadApprovals();
  if (activeTab.value === "correction" || activeTab.value === "overtime") {
    return fnLoadReqInbox();
  }
};

// 조회 버튼 진입점 — 활성 탭 목록 + 전 탭(근태보정/초과근무/연차) 카운트 배지를 함께 갱신.
const fnSearch = async () => {
  await Promise.all([fnLoad(), fnLoadCounts()]);
};

const fnLoadApprovals = async () => {
  try {
    const r = await axios.get("/webApi/leaveflow/my-approvals", {});
    approvalList.value = r.data?.approvalList ?? [];
    // 선택 유지 또는 해제
    if (
      selected.value &&
      !approvalList.value.some((x) => x.reqId === selected.value.reqId)
    ) {
      selected.value = null;
    }
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "결재 목록 조회 오류."));
  }
};

const fnLoadReqInbox = async () => {
  try {
    const r = await axios.get("/webApi/reqinbox/pending", {
      params: { reqTypeGroup: reqTypeGroupOf(activeTab.value) },
    });
    reqList.value = r.data?.pendingList ?? [];
    if (
      reqSelected.value &&
      !reqList.value.some((x) => x.reqId === reqSelected.value.reqId)
    ) {
      reqSelected.value = null;
    }
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "대기 요청 조회 오류."));
  }
};

// 탭 배지용 대기 건수 로드 — 활성 탭과 무관하게 연차/근태보정/초과근무 3종 모두 갱신.
// 배지 카운트는 보조 정보이므로 실패해도 화면 흐름은 막지 않는다.
const fnLoadCounts = async () => {
  try {
    const [leaveRes, corrRes, otRes] = await Promise.all([
      axios.get("/webApi/leaveflow/my-approvals"),
      axios.get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "correction" },
      }),
      axios.get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "overtime" },
      }),
    ]);
    leaveCount.value = (leaveRes.data?.approvalList ?? []).length;
    correctionCount.value = (corrRes.data?.pendingList ?? []).length;
    overtimeCount.value = (otRes.data?.pendingList ?? []).length;
  } catch (e) {
    console.warn("[Attd_10] 탭 카운트 로드 실패", e);
  }
};

const fnSelectReq = (row) => {
  reqSelected.value = row;
  reqDecision.value = "approve";
  reqRejectReason.value = "";
};

// 근태보정/초과근무 처리 진입점 — 라디오 선택에 따라 승인 또는 반려로 분기.
const fnProcessReq = () => {
  if (reqDecision.value === "reject") return fnRejectReq();
  return fnApproveReq();
};

// 근태보정/초과근무 요청을 "요청대로 승인" — 근무 관리(Attd_07)와 동일한 승인 엔드포인트를
// 호출해 근태/초과근무 기록을 반영하고 요청 상태를 승인('02')으로 전이한다.
// (역할은 근무 관리 화면과 중복되지만, 편의상 본 화면에서도 처리 가능하도록 제공.)
const fnApproveReq = async () => {
  const r = reqSelected.value;
  if (!r) return;
  const ok = await proxy.$confirm("승인 처리하시겠습니까?");
  if (!ok) return;

  const isOvertime = activeTab.value === "overtime";
  let url;
  let payload;
  if (isOvertime) {
    // 초과근무 승인: 요청 구간 그대로 OT 등록 + 연결 요청 승인 마감.
    url = "/webApi/attd07/update-user-overtime-requests";
    payload = {
      reqId: r.reqId,
      userCd: r.userCd,
      siteCd: r.siteCd,
      nodeCd: r.nodeCd || "",
      workYmd: r.workYmd,
      reqReason: r.reqReason || "",
      overtimes: [
        {
          // prafta-043: 초과근무 유형(otType) 전면 파기 — payload 에서 제거.
          startDate: r.startDate || r.workYmd,
          startTime: r.startTime,
          endDate: r.endDate || r.workYmd,
          endTime: r.endTime,
        },
      ],
    };
  } else {
    // 근태 보정 승인: 요청자가 적은 출퇴근 값을 그대로 근태 기록에 반영 + 요청 승인 마감.
    url = "/webApi/attd07/update-user-attd-requests";
    payload = {
      reqId: r.reqId,
      siteCd: r.siteCd,
      userCd: r.userCd,
      workYmd: r.workYmd,
      workSeq: String(r.workSeq),
      nodeCd: r.nodeCd,
      checkInDate: r.startDate || r.workYmd,
      checkInTime: r.startTime || "",
      checkInMethod: "02",
      checkOutDate: r.endDate || r.workYmd,
      checkOutTime: r.endTime || "",
      checkOutMethod: "02",
      processComment: "요청 승인 관리 승인",
    };
  }

  reqProcessing.value = true;
  try {
    await axios.post(url, payload);
    await proxy.$alert("승인되었습니다.");
    reqSelected.value = null;
    await fnLoadReqInbox();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(
      resolveApiErrorMessage(e, "승인 처리 중 오류가 발생했습니다.")
    );
  } finally {
    reqProcessing.value = false;
  }
};

const fnRejectReq = async () => {
  if (!reqSelected.value) return;
  if (!(reqRejectReason.value || "").trim()) {
    return proxy.$alert("반려 사유를 입력해주세요.");
  }
  const ok = await proxy.$confirm("반려 처리하시겠습니까?");
  if (!ok) return;

  const r = reqSelected.value;
  const isOvertime = activeTab.value === "overtime";
  // 반려 엔드포인트는 서버 보관 REQ 와 키필드 일치를 요구한다(변조 방지).
  const url = isOvertime
    ? "/webApi/attd07/reject-user-overtime-requests"
    : "/webApi/attd07/reject-user-attd-requests";
  const payload = isOvertime
    ? {
        reqId: r.reqId,
        siteCd: r.siteCd,
        userCd: r.userCd,
        rejectReason: reqRejectReason.value,
      }
    : {
        reqId: r.reqId,
        siteCd: r.siteCd,
        userCd: r.userCd,
        workYmd: r.workYmd,
        workSeq: String(r.workSeq),
        nodeCd: r.nodeCd,
        rejectReason: reqRejectReason.value,
      };

  reqProcessing.value = true;
  try {
    await axios.post(url, payload);
    await proxy.$alert("반려되었습니다.");
    reqSelected.value = null;
    await fnLoadReqInbox();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "반려 처리 중 오류가 발생했습니다."));
  } finally {
    reqProcessing.value = false;
  }
};

const fnSelect = (row) => {
  selected.value = row;
  decision.value = "approve";
  rejectReason.value = "";
};

const fnProcess = async () => {
  if (!selected.value) return;
  const payload = {
    reqId: selected.value.reqId,
    approvalStep: selected.value.approvalStep,
    comment: decision.value === "reject" ? rejectReason.value : "",
  };

  if (decision.value === "reject") {
    if (!(rejectReason.value || "").trim()) {
      return proxy.$alert("반려 사유를 입력해주세요.");
    }
  }

  const ok = await proxy.$confirm(
    decision.value === "approve" ? "승인 처리하시겠습니까?" : "반려 처리하시겠습니까?"
  );
  if (!ok) return;

  processing.value = true;
  try {
    const url =
      decision.value === "approve"
        ? "/webApi/leaveflow/approve"
        : "/webApi/leaveflow/reject";
    await axios.post(url, payload);
    await proxy.$alert("처리되었습니다.");
    selected.value = null;
    await fnLoad();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "결재 처리 중 오류가 발생했습니다."));
  } finally {
    processing.value = false;
  }
};

onMounted(() => {
  fnLoad();
  fnLoadCounts();
});
</script>

<style scoped>
.ra-tabs {
  display: flex;
  gap: 0.25rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  padding: 0 0.5rem;
}
.ra-tab {
  border: none;
  background: transparent;
  padding: 0.6rem 0.9rem;
  font-size: 0.9rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  border-bottom: 2px solid transparent;
}
.ra-tab.active {
  color: var(--color-primary, #30796a);
  border-bottom-color: var(--color-primary, #30796a);
  font-weight: 600;
}
.ra-tab-badge {
  display: inline-block;
  min-width: 1.2rem;
  padding: 0 0.3rem;
  margin-left: 0.2rem;
  border-radius: 0.6rem;
  background: var(--color-primary, #30796a);
  color: #fff;
  font-size: 0.72rem;
}
.ra-body {
  display: flex;
  gap: 1rem;
  padding: 0.75rem;
  align-items: stretch;
}
.ra-inbox {
  flex: 0 0 58%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  display: flex;
  flex-direction: column;
  min-height: 360px;
}
.ra-detail {
  flex: 1;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  padding: 0.75rem;
  background: var(--color-surface, #fff);
}
.ra-inbox__head {
  padding: 0.6rem 0.75rem;
  font-weight: 600;
  font-size: 0.9rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.ra-list {
  flex: 1;
  overflow-y: auto;
}
.ra-row {
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  border-left: 4px solid transparent;
  cursor: pointer;
}
.ra-row:hover {
  background: var(--color-surface-hover, #f9fafb);
}
.ra-row.selected {
  border-left-color: var(--color-primary, #30796a);
  background: var(--color-primary-tint, #dcfce7);
}
.ra-row__main {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.ra-row__name {
  font-size: 0.9rem;
  color: var(--color-text, #111827);
}
.ra-row__dept {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
}
.ra-row__sub {
  font-size: 0.8rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 0.2rem;
}
.ra-chip.self {
  font-size: 0.7rem;
  background: var(--color-amber-tint, #fef3c7);
  color: var(--color-amber, #b45309);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
}
.ra-chip.type {
  font-size: 0.7rem;
  background: var(--color-primary-tint, #dcfce7);
  color: var(--color-primary, #30796a);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
}
.ra-empty,
.ra-detail__empty {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.85rem;
  text-align: center;
  padding: 2rem 0;
}
.ra-sec {
  margin-bottom: 0.9rem;
}
.ra-sec__title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text, #111827);
  margin-bottom: 0.4rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
}
.ra-meta {
  display: grid;
  grid-template-columns: 5.5rem 1fr;
  gap: 0.3rem 0.5rem;
  font-size: 0.84rem;
  margin: 0;
}
.ra-meta dt {
  color: var(--color-text-muted, #6b7280);
}
.ra-meta dd {
  margin: 0;
  color: var(--color-text, #111827);
}
.ra-decide {
  border-top: 1px solid var(--color-border, #e5e7eb);
  padding-top: 0.6rem;
}
.ra-radio {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.3rem;
  cursor: pointer;
}
.ra-decide textarea {
  width: 100%;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.35rem;
  padding: 0.35rem 0.5rem;
  font-size: 0.85rem;
  margin: 0.3rem 0;
}
.ra-decide__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.4rem;
}
.btn-process {
  background: var(--color-primary, #30796a);
  color: #fff;
  border: none;
  border-radius: 0.35rem;
  padding: 0.45rem 1.1rem;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-process:disabled {
  opacity: 0.6;
  cursor: default;
}
.btn-reject {
  background: var(--color-danger, #dc2626);
}
.ra-decide__note {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0 0 0.4rem;
  line-height: 1.4;
}
</style>
