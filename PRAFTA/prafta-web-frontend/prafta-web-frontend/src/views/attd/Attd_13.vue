<!--
  Attd_13.vue — 관리자 연차 변경/삭제 동의 관리 (prafta-com-008-C-3)
  유형: frontend-screen (웹 관리자)
  연결 작업: PRAFTA-{C-4-web}
  참조 패턴: views/notice/Notice_02.vue (ViewHeader + viewSearch + viewBody + data-grid + 팝업),
            views/attd/Attd_12.vue (모니터링성 조회 화면)
  역할 분담: 본 화면(planner 골격) = 레이아웃/검색바/그리드/팝업 토글 구조.
            developer = 조회/발의/확인 API 호출 + store/router + 정렬/리사이즈 로직.
  ※ 비즈니스 로직(API/계산/라우팅)은 developer 가 채운다. 골격은 template + style + 상태 선언만.
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 검색바: 사업장 / 소속부서(하위부서 포함) / 사용자명 / 요청상태 -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <!-- TODO(developer): 사업장 목록 바인딩(세션 사업장 스코프). 단일 사업장이면 고정 표시. -->
        <select v-model="siteCd" name="combo">
          <option value="">전체</option>
          <option v-for="s in siteList" :key="s.siteCd" :value="s.siteCd">
            {{ s.siteNm }}
          </option>
        </select>
      </div>

      <div>
        <label>소속부서</label>
        <!-- TODO(developer): 노드 트리/셀렉트 + 하위부서 포함 토글(INC_SUB, prafta-028 패턴). -->
        <select v-model="nodeCd" name="combo">
          <option value="">전체</option>
          <option v-for="n in nodeList" :key="n.nodeCd" :value="n.nodeCd">
            {{ n.nodeNm }}
          </option>
        </select>
      </div>

      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="includeSubNode" />
          하위부서 포함
        </label>
      </div>

      <div>
        <label>사용자명</label>
        <input
          id="changeReqUserKeyword"
          type="text"
          v-model="userKeyword"
          placeholder="사용자명"
        />
      </div>

      <div>
        <label>요청상태</label>
        <!-- 상태머신: REQUESTED / AGREED / REJECTED / CONFIRMED / CLOSED (SYS 신규) -->
        <select v-model="reqStatus" name="combo">
          <option value="">전체</option>
          <option value="REQUESTED">요청(응답대기)</option>
          <option value="AGREED">동의(확인대기)</option>
          <option value="REJECTED">거부</option>
          <option value="CONFIRMED">확정</option>
          <option value="CLOSED">종료</option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-text">연차 변경/삭제 요청 목록</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 60vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="text-align: center; width: 3%">No</th>
                <th>사용자</th>
                <th>대상 연차일</th>
                <!-- G1: 시간차/반차 구분 없이 "무슨 연차인지"를 목록에서 바로 식별 -->
                <th>사용단위</th>
                <th>요청유형</th>
                <th>이동대상일</th>
                <th>발의주체</th>
                <th>상태</th>
                <th>근로자응답</th>
                <th style="text-align: center; width: 8%">처리</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!changeReqList || changeReqList.length === 0">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    조회된 요청이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(item, idx) in changeReqList"
                  :key="item.changeReqId"
                  style="cursor: pointer"
                  @dblclick="fnOpenConfirmPop(item)"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ item.userNm }}</td>
                  <td style="text-align: center">{{ item.targetStartDate }}</td>
                  <td style="text-align: center">
                    {{ item.unitLabel || "-" }}
                  </td>
                  <td style="text-align: center">{{ item.reqTypeNm }}</td>
                  <!-- 위치선택 확장(2026-08-18): 지정 파트/시각 병기(그리드 폭 고려 축약 라벨 —
                       전체 라벨은 확인 팝업에서 표기). 미지정이면 종전 표시 그대로 -->
                  <td style="text-align: center">
                    {{ item.moveTargetDate || "-" }}<template v-if="item.moveTargetPos"> · {{ item.moveTargetPos }}</template>
                  </td>
                  <td style="text-align: center">{{ item.initiatorTypeNm }}</td>
                  <td style="text-align: center">{{ item.reqStatusNm }}</td>
                  <td style="text-align: center">
                    {{ item.workerResponseNm || "-" }}
                  </td>
                  <td style="text-align: center">
                    <!-- AGREED(동의·확인대기) 상태에서만 확인 버튼 활성 -->
                    <button
                      type="button"
                      class="btn-confirm"
                      :disabled="item.reqStatus !== 'AGREED'"
                      @click.stop="fnOpenConfirmPop(item)"
                    >
                      확인
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 관리자 최종 확인 팝업 (근로자 동의 후 실제 반영) -->
    <LeaveChangeConfirmPop
      v-if="showConfirmPop"
      :change-req-id="selectedChangeReqId"
      @close="showConfirmPop = false"
      @confirmed="fnAfterConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, getCurrentInstance, onMounted } from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import LeaveChangeConfirmPop from "./popup/LeaveChangeConfirmPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

const props = defineProps({
  title: { type: String, default: "연차 변경 동의 관리" },
  buttons: Object,
});

const { proxy } = getCurrentInstance();

// ── 검색 조건 ────────────────────────────────────────────────────────────
const siteCd = ref("");
const nodeCd = ref("");
const includeSubNode = ref(true);
const userKeyword = ref("");
const reqStatus = ref("");

// ── 코드/목록 ────────────────────────────────────────────────────────────
const siteList = ref([]);
const nodeList = ref([]);
const changeReqList = ref([]);

// ── 팝업 토글 ────────────────────────────────────────────────────────────
// 발의(LeaveChangeRequestPop) 동선은 Attd_05 달력 셀로 일원화([부분휴가진입점-04] §5-1 확정)
//   — 본 화면은 동의 관리(목록·확인) 전용.
const showConfirmPop = ref(false);
const selectedChangeReqId = ref("");

// 헤더 버튼 — 권한 메뉴(tb_syst_auth_menu BTN_*)에서 주입된 props.buttons 사용(Attd_14 등과 동일 패턴).
const localButtons = ref({ ...props.buttons });

// 권한 스코프(D1+D3): master/hr 는 회사 전사(사업장/부서 자유), 그 외(노드 관리자)는 담당 부서 강제.
//   서버도 동일 정책으로 fail-closed 강제(canManageNodeExcludeSafe + 역할 기반 스코프, safe 제외).
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// 코드값 → 라벨 매핑 (그리드 표시용). 서버 row 는 코드값만 반환.
const REQ_TYPE_NM = { MOVE: "이동", DELETE: "삭제" };
const INITIATOR_TYPE_NM = { ADMIN: "관리자", WORKER: "근로자" };
const REQ_STATUS_NM = {
  REQUESTED: "요청(응답대기)",
  AGREED: "동의(확인대기)",
  REJECTED: "거부",
  CONFIRMED: "확정",
  CLOSED: "종료",
};
const WORKER_RESPONSE_NM = { PENDING: "대기", AGREE: "동의", REJECT: "거부" };

// 표시용 날짜 포맷은 dateFormat 단일 출처에 위임(점 구분 YYYY.MM.DD).
const fmtYmd = (ymd) => formatYmdDot(ymd);

// G1: 사용단위 라벨 — 시간차(SYS025 02:2시간 / 03:1시간 / 04:30분)는 '시간차 ' 접두
//   (AttdDayDetailPop 표기 관례와 동형). 단위 라벨이 없으면 빈 값 → 그리드에서 '-' 표시.
const HOURLY_UNITS = ["02", "03", "04"];
const unitLabelOf = (unitCode, unitNm) => {
  if (!unitNm) return "";
  // SYS025 단위명이 이미 '시간차(…)' 형태면 접두 생략(중복 표기 방지)
  return HOURLY_UNITS.includes(unitCode) && !unitNm.startsWith("시간차") ? `시간차 ${unitNm}` : unitNm;
};

// ── 위치선택 확장(2026-08-18): 이동 대상 위치(반차 파트/시간차 지정 시각) 병기 ──
//   그리드는 폭 제약으로 축약 라벨("시작 기준/종료 기준")을 쓰고,
//   전체 라벨("시작 기준(늦게 출근)")은 확인 팝업(LeaveChangeConfirmPop)에서 표기한다.
//   미지정(null)이면 빈 값 → 종전 표시 바이트 그대로(무회귀). 오전/오후 환산 금지(plan §4 사용자 확정).
const MOVE_HALF_PART_NM_SHORT = { START: "시작 기준", END: "종료 기준" };
const fmtHhmm = (hhmm) => {
  const v = String(hhmm ?? "");
  return v.length >= 4 ? `${v.slice(0, 2)}:${v.slice(2, 4)}` : "";
};
const hhmmToMin = (hhmm) => {
  const v = String(hhmm ?? "");
  if (v.length !== 4) return null;
  const h = parseInt(v.slice(0, 2), 10);
  const m = parseInt(v.slice(2, 4), 10);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  if (h < 0 || h > 23 || m < 0 || m > 59) return null;
  return h * 60 + m;
};
const moveTargetPosLabelOf = (halfPart, moveStartTime, leaveMinutes) => {
  if (halfPart) return MOVE_HALF_PART_NM_SHORT[halfPart] || "";
  const s = hhmmToMin(moveStartTime);
  if (s == null) return "";
  const dur = Number(leaveMinutes);
  if (!Number.isFinite(dur) || dur <= 0) return fmtHhmm(moveStartTime);
  // 시간차 종료는 시작+원 분량(leaveMinutes) 클라 파생(표시 전용). 자정 넘김은 익일 규약 — 모듈러 표기.
  const e = (s + dur) % 1440;
  const pad = (n) => String(n).padStart(2, "0");
  return `${fmtHhmm(moveStartTime)}~${pad(Math.floor(e / 60))}:${pad(e % 60)}`;
};

// 서버 row → 그리드 표시 객체로 보강(라벨/포맷)
const toRow = (r) => ({
  changeReqId: r.changeReqId,
  reqStatus: r.reqStatus,
  reqType: r.reqType,
  userNm: r.targetUserNm,
  targetStartDate: fmtYmd(r.targetStartDate),
  unitLabel: unitLabelOf(r.useUnitType, r.unitNm),
  reqTypeNm: REQ_TYPE_NM[r.reqType] || r.reqType,
  moveTargetDate: fmtYmd(r.moveTargetDate),
  // 위치선택 확장: 지정 파트/시각 병기 라벨(미지정이면 빈 값 — 종전 표시 그대로)
  moveTargetPos: moveTargetPosLabelOf(
    r.moveTargetHalfPart,
    r.moveTargetStartTime,
    r.leaveMinutes
  ),
  initiatorTypeNm: INITIATOR_TYPE_NM[r.initiatorType] || r.initiatorType,
  reqStatusNm: REQ_STATUS_NM[r.reqStatus] || r.reqStatus,
  workerResponseNm: WORKER_RESPONSE_NM[r.workerResponse] || r.workerResponse,
});

// 사업장 목록 (세션 회사 스코프). 서버는 토큰 사업장과 일치 강제.
const fnLoadSiteList = async () => {
  try {
    const res = await axios.get("/comApi/baseinfo/site-lists", {
      params: { cmpnyCd: sessionStorage.getItem("gv_cmpnyCd") },
    });
    if (res.status === 200) {
      const list = res.data?.siteInfoResultList ?? [];
      siteList.value = list.map((s) => ({
        siteCd: s.siteCd,
        siteNm: s.siteNm,
      }));
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 선택 사업장의 부서 목록
const fnLoadNodeList = async () => {
  if (!siteCd.value) {
    nodeList.value = [];
    nodeCd.value = "";
    return;
  }
  try {
    const res = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
      },
    });
    if (res.status === 200) {
      const list = res.data?.siteNodeInfoList ?? [];
      nodeList.value = list.map((n) => ({
        nodeCd: n.nodeCd,
        nodeNm: n.nodeNm,
      }));
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// ── 조회 ─────────────────────────────────────────────────────────────────
//   GET /webApi/attd13/change-requests
//   서버가 권한/IDOR(canManageNodeExcludeSafe + INC_SUB) fail-closed 강제(safe 제외).
const fnSearch = async () => {
  // 노드 관리자(비 master/hr)는 담당 부서 선택 필수. 미선택 시 서버 호출(BadRequest) 대신 안내.
  if (!isMasterOrHr.value && !nodeCd.value) {
    changeReqList.value = [];
    await proxy.$alert("조회할 부서를 선택해 주세요.");
    return;
  }
  try {
    const res = await axios.get("/webApi/attd13/change-requests", {
      params: {
        SITE_CD: siteCd.value,
        NODE_CD: nodeCd.value,
        INC_SUB_NODE_YN: includeSubNode.value ? "Y" : "N",
        USER_NM: userKeyword.value,
        REQ_STATUS: reqStatus.value,
      },
    });
    if (res.status === 200) {
      const list = res.data?.list ?? [];
      changeReqList.value = list.map(toRow);
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR))
    );
  }
};

const fnOpenConfirmPop = (item) => {
  // 모든 상태 상세 열람 가능. 확인/반려 활성 여부는 팝업이 상태로 게이팅.
  selectedChangeReqId.value = item.changeReqId;
  showConfirmPop.value = true;
};

const fnAfterConfirm = () => {
  showConfirmPop.value = false;
  fnSearch();
};

// 초기화 동안 siteCd watch 의 부서 초기화/재조회를 억제(프리셋 nodeCd 클로버 방지).
const initializing = ref(true);

onMounted(async () => {
  await fnLoadSiteList();
  if (isMasterOrHr.value) {
    // master/hr: 회사 전사 기본(사업장 '전체'). 부서 자유. 진입 즉시 전사 자동조회.
    siteCd.value = "";
    nodeCd.value = "";
    await fnSearch();
  } else {
    // 노드 관리자: 세션 사업장 고정 + 본인 담당 부서 프리셋. 부서 프리셋이 있으면 자동조회,
    //   없으면 자동조회를 건너뛰고 부서 선택을 안내(403 즉시 발생 방지).
    siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
    await fnLoadNodeList();
    nodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
    if (nodeCd.value) {
      await fnSearch();
    }
  }
  initializing.value = false;
});

// 사업장 변경 시 부서 목록 갱신(초기화 중에는 프리셋 보존 위해 스킵)
watch(siteCd, async () => {
  if (initializing.value) return;
  nodeCd.value = "";
  await fnLoadNodeList();
});
</script>

<style scoped>
/* 하위부서 포함 체크박스 (User_01/Attd_14 checkbox-label 패턴 차용) */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  user-select: none;
  margin-left: -1rem;
  margin-right: 0.4rem;
  white-space: nowrap;
}
.checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
}

.btn-confirm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
  border: 1px solid var(--color-primary);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
  color: var(--color-primary);
  cursor: pointer;
}

.btn-confirm:disabled {
  border-color: var(--color-border);
  color: var(--color-text-muted);
  background: var(--color-bg);
  cursor: not-allowed;
}

.edu-grid-empty {
  text-align: center;
  padding: var(--card-padding);
  color: var(--color-text-muted);
}
</style>
