<template>
  <div class="viewComm">
    <!-- 탭 (재기획서 §5.3) — Attd_01 형태: 탭바를 화면명(ViewHeader) 위에 둔다. -->
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

    <ViewHeader
      class="commViewHeader"
      :title="props.title || '요청 승인 관리'"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewBody ra-body">
      <!-- 연차 탭: 2분할 (접수함 / 상세) -->
      <template v-if="activeTab === 'leave'">
        <!-- 접수함: 좌측 컬럼을 두 소섹션으로 분리(B안) — 기존 결재 대기(위, 무수정) + 신규 연차 변경 대기(아래, 신규) -->
        <section class="ra-inbox">
          <!-- 기존 "내 결재 대기" 블록 — wrapper 만 추가, 내부 마크업/로직 100% 동일 -->
          <div class="ra-inbox__block">
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
          </div>

          <!-- 신규: 연차 변경(이동/삭제) 요청 대기 — 별도 배열(leavechangeList), 위 블록과 데이터 미병합 -->
          <div class="ra-inbox__block ra-inbox__block--leavechange">
            <div class="ra-inbox__head">
              연차 변경 요청 대기 ({{ leavechangeList.length }})
            </div>
            <div class="ra-list ra-list--compact">
              <div v-if="leavechangeList.length === 0" class="ra-empty">
                확인 대기 중인 연차 변경 요청이 없습니다.
              </div>
              <div
                v-for="row in leavechangeList"
                :key="row.changeReqId"
                class="ra-row"
                @click="fnSelectLeaveChange(row)"
              >
                <div class="ra-row__main">
                  <span class="ra-row__name">{{ row.targetUserNm }}</span>
                  <span class="ra-chip type">{{ leaveChangeTypeNm(row.reqType) }}</span>
                  <span class="ra-chip self">{{ leaveChangeInitiatorNm(row.initiatorType) }}</span>
                </div>
                <div class="ra-row__sub">
                  {{ fmtDate(row.targetStartDate) }}
                  <template v-if="row.reqType === 'MOVE'">
                    → {{ fmtDate(row.moveTargetDate) }}
                  </template>
                  · {{ leaveChangeStatusNm(row.reqStatus) }}
                </div>
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

    <LeaveChangeConfirmPop
      v-if="showLeaveChangePop"
      :change-req-id="selectedLeaveChangeReqId"
      @close="showLeaveChangePop = false"
      @confirmed="fnAfterLeaveChangeConfirmed"
    />
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
import LeaveChangeConfirmPop from "./popup/LeaveChangeConfirmPop.vue";
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

// "연차 상신" 탭 내 "연차 변경 요청 대기" 소섹션(B안) — approvalList 와 별개 배열, 데이터 병합 없음.
//   상세/확인/반려는 기존 LeaveChangeConfirmPop 재사용, 중복 컴포넌트 금지.
const leavechangeList = ref([]);
const leavechangeCount = ref(0);
const showLeaveChangePop = ref(false);
const selectedLeaveChangeReqId = ref("");

// 연차 변경 요청 코드 → 라벨 매핑(TB_LEAVE_CHANGE_REQUEST 전용 — SYS032 와 무관, 재사용 금지)
const LEAVE_CHANGE_TYPE_NM = { MOVE: "이동", DELETE: "삭제" };
const LEAVE_CHANGE_INITIATOR_NM = { ADMIN: "관리자발의", WORKER: "근로자발의" };
const LEAVE_CHANGE_STATUS_NM = {
  REQUESTED: "요청(응답대기)",
  AGREED: "동의(확인대기)",
  REJECTED: "거부",
  CONFIRMED: "확정",
  CLOSED: "종료",
};
const leaveChangeTypeNm = (t) => LEAVE_CHANGE_TYPE_NM[t] || t || "-";
const leaveChangeInitiatorNm = (t) => LEAVE_CHANGE_INITIATOR_NM[t] || t || "-";
const leaveChangeStatusNm = (s) => LEAVE_CHANGE_STATUS_NM[s] || s || "-";

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
  if (activeTab.value === "leave") {
    // B안: 결재 대기 + 연차 변경 대기 두 소섹션을 동시 로드(데이터 병합 없음, 별개 배열 유지)
    return Promise.all([fnLoadApprovals(), fnLoadLeaveChanges()]);
  }
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

// "연차 상신" 탭 내 "연차 변경 요청 대기" 소섹션 목록 조회(B안) — 확인 대기(AGREED) 건만 노출.
//   사이트/부서 셀렉터가 없는 화면이므로 SITE_CD/NODE_CD 는 비워서 호출(master/hr 는 전사, 노드 관리자는 400 — 상세설명 4번).
//   fnLoadApprovals 와 독립적으로 실패를 격리해, 한쪽이 실패해도 다른 소섹션은 정상 표시되게 한다.
const fnLoadLeaveChanges = async () => {
  try {
    const r = await axios.get("/webApi/attd13/change-requests", {
      params: { REQ_STATUS: "AGREED" },
    });
    leavechangeList.value = r.data?.list ?? [];
    leavechangeCount.value = r.data?.totalCnt ?? leavechangeList.value.length;
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "연차 변경 요청 조회 오류."));
  }
};

// 소섹션 행 클릭 → 상세/확인/반려 팝업 오픈(UI 토글 — 로직 없음, 기존 LeaveChangeConfirmPop 재사용)
const fnSelectLeaveChange = (row) => {
  selectedLeaveChangeReqId.value = row.changeReqId;
  showLeaveChangePop.value = true;
};

// 팝업에서 확인/반려 완료 시 연차변경 소섹션·탭 배지만 재조회(approvalList 는 영향 없음)
const fnAfterLeaveChangeConfirmed = () => {
  showLeaveChangePop.value = false;
  fnLoadLeaveChanges();
  fnLoadCounts();
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

// 탭 배지용 대기 건수 로드 — 활성 탭과 무관하게 연차(결재+변경 합산)/근태보정/초과근무 갱신.
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
    const approvalCnt = (leaveRes.data?.approvalList ?? []).length;
    correctionCount.value = (corrRes.data?.pendingList ?? []).length;
    overtimeCount.value = (otRes.data?.pendingList ?? []).length;

    // 연차 변경 요청 대기 건수는 별도 try/catch 로 격리(노드 관리자 400 등 실패해도 위 3개 배지 갱신은 막지 않는다).
    try {
      const changeRes = await axios.get("/webApi/attd13/change-requests", {
        params: { REQ_STATUS: "AGREED" },
      });
      leavechangeCount.value =
        changeRes.data?.totalCnt ?? (changeRes.data?.list ?? []).length;
    } catch (e) {
      console.warn("[Attd_10] 연차 변경 요청 카운트 로드 실패", e);
    }

    // B안 배지 합산 결정(상세설명 5번 근거): 탭 배지는 "이 탭에 처리할 게 총 몇 건" 을 보여줘야 하므로
    //   결재 대기 + 연차 변경 대기를 합산한다. 소섹션별 개별 건수는 각 블록 헤더에서 별도 표시.
    leaveCount.value = approvalCnt + leavechangeCount.value;
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
/* 탭바 표준(Attd_01 .attd01-tab-bar/.attd01-tab-btn 스펙 준수 — 밑줄형 14px) */
.ra-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
.ra-tab {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.ra-tab:hover {
  color: var(--color-text, #374151);
}
.ra-tab.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
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
/* "연차 상신" 탭 ra-inbox 컬럼을 두 소섹션으로 세로 분할(B안).
   기존 블록(.ra-inbox__block 첫 번째)은 가변 높이 유지(기존 UX 동일),
   신규 연차변경 블록은 고정 높이 + 자체 스크롤로 보조 정보 성격을 시각적으로 구분한다. */
.ra-inbox__block {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.ra-inbox__block:first-child {
  flex: 1 1 auto;
}
.ra-inbox__block--leavechange {
  flex: 0 0 auto;
  border-top: 1px solid var(--color-border, #e5e7eb);
}
.ra-list--compact {
  max-height: 180px;
  overflow-y: auto;
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
