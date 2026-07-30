<!--
  LeavePromotion_01_1.vue — 연차 사용촉진 1차 현황 (관리자 웹, 신규)
  - 출처: 작업지시서_연차촉진-1차현황-화면-및-배치활성화.md §5(API)/§6(화면), 확정 D1·D2·D4·D6·D7·D8
  - UI 명세: plan.md §8 (1차 현황 탭 UI 명세)
  - 참조 패턴: LeavePromotion_01_2.vue(=구 LeavePromotion_01, Attd_12 계열 조회조건/테이블),
              Attd_15.vue(.a15-badge/.status-* pill + color-mix 파생), Attd_07.vue(.lg-summary)
  - planner 라운드: template + scoped style 완성. script 는 import/ref 선언 + 선언적 상수맵 + TODO 만.
  - developer 라운드(plan.md T3-2):
      · GET /webApi/leavepromo01/first-targets → rows / summary
      · 행 [독촉] / 상단 [미제출 전체 독촉] → POST /webApi/leavepromo01/remind
      · 사업장/부서 자동조회·팝업·포커스 처리는 LeavePromotion_01_2 의 동일 함수를 미러
  - ⚠️ 표시 금지(D1): 지정 일수(STAGE1_DESIGNATED_DAYS)·지정 날짜.
  - ⚠️ 상태/기한/독촉가능 판정은 전부 서버 산출값(statusCd/statusNm/dDay/remindableYn/lateNoticeYn) 사용.
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 (2차 탭과 동일 패턴, 1년차 구분 select 없음) -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          ref="siteNoFcs"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="siteDisabled"
          @click="fnSiteSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          :disabled="siteDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label>소속부서</label>
        <input
          id="nodeCd"
          type="text"
          v-model="nodeCd"
          placeholder="부서코드"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
        <button
          class="search-btn"
          :disabled="nodeDisabled"
          @click="fnSiteNodeSearchPopOpen()"
        >
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="nodeNm"
          type="text"
          v-model="nodeNm"
          placeholder="부서명"
          :disabled="nodeDisabled"
          @blur="focusKill"
        />
      </div>
      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="incSubNodeYn" :disabled="!nodeCd" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>사용자명</label>
        <input type="text" v-model="userNm" placeholder="사용자명" />
      </div>
      <!-- 일괄 독촉 (미제출자 전체) -->
      <div class="lp1-actions">
        <button
          class="lp1-remind-all-btn"
          :disabled="isLoading || !summary.remindableCount"
          @click="fnRemindAll"
        >
          미제출 전체 독촉
        </button>
      </div>
    </div>

    <!-- 본문: 1차 통지 현황 -->
    <div class="viewBody lp1-body subtitle-pane">
      <div class="subtitle-row">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">1차 통지 현황</span>
        </div>
        <!-- 요약 뱃지 5종 (서버 summary 값 표시 전용) -->
        <div class="lp1-summary">
          <span class="lp1-sum lp1-sum--total">
            대상 <b>{{ summary.totalCount || 0 }}</b>명
          </span>
          <span class="lp1-sum lp1-sum--pending">
            미제출 <b>{{ summary.notSubmittedCount || 0 }}</b>
          </span>
          <span class="lp1-sum lp1-sum--overdue">
            기한초과 미제출 <b>{{ summary.overdueNotSubmittedCount || 0 }}</b>
          </span>
          <span class="lp1-sum lp1-sum--late-notice">
            지연통지 <b>{{ summary.lateNoticeCount || 0 }}</b>
          </span>
          <span class="lp1-sum lp1-sum--done">
            제출완료 <b>{{ summary.submittedCount || 0 }}</b>
          </span>
        </div>
      </div>

      <div class="lp1-table-wrap">
        <table class="lp1-table">
          <thead>
            <tr>
              <th>이름</th>
              <th>부서</th>
              <th>사업장</th>
              <th>통지일</th>
              <th>제출 기한</th>
              <th>상태</th>
              <th>2차 도래 예정일</th>
              <th>앱 안내 확인</th>
              <th>독촉</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="rows.length === 0">
              <td colspan="9" class="lp1-empty">
                1차 통지 대상자가 없습니다.
              </td>
            </tr>
            <tr v-for="r in rows" :key="r.userCd">
              <td class="lp1-cell-left">{{ r.userNm }}</td>
              <td class="lp1-cell-left">{{ r.nodeNm }}</td>
              <td class="lp1-cell-left">{{ r.siteNm }}</td>
              <td>{{ fmtYmd(r.noticedDate) }}</td>
              <td>
                <span>{{ fmtYmd(r.deadlineDate) }}</span>
                <span class="lp1-dday">{{ fmtDDay(r.dDay) }}</span>
              </td>
              <td>
                <span :class="['lp1-badge', STATUS_CLASS[r.statusCd]]">
                  {{ r.statusNm }}
                </span>
                <span v-if="r.lateNoticeYn === 'Y'" class="lp1-late-notice">
                  지연통지
                </span>
              </td>
              <td>{{ fmtYmd(r.stage2DueDate) }}</td>
              <td>{{ r.loginNotifiedYn === "Y" ? "확인" : "미확인" }}</td>
              <td>
                <button
                  class="lp1-remind-btn"
                  :disabled="isLoading || r.remindableYn !== 'Y'"
                  @click="fnRemind([r.userCd])"
                >
                  독촉
                </button>
                <span v-if="r.remindCnt" class="lp1-remind-hist">
                  {{ fmtRemind(r.remindCnt, r.lastRemindDate) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  getCurrentInstance,
  defineProps,
  defineOptions,
  onMounted,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "LeavePromotion_01_1" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ── 헤더 버튼 (조회 전용) ─────────────────────────────────
const localButtons = ref({ ...props.buttons });

// ── 조회 조건 (1년차 구분 없음 — 1차 대상은 구조적으로 1년차 이상) ──
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const userNm = ref("");
const siteNoFcs = ref(null);

// ── 조회 결과 ─────────────────────────────────────────────
//   row: { userCd, userNm, nodeCd, nodeNm, siteCd, siteNm, noticedDate, deadlineDate,
//          baseAvailToDate, stage2DueDate, dDay, statusCd, statusNm, submittedYn,
//          firstSubmitDate, lateNoticeYn, loginNotifiedYn, remindCnt, lastRemindDate,
//          remindableYn }
//   ⚠️ 지정 일수/지정 날짜는 응답에 없다(D1).
const rows = ref([]);
//   summary: { totalCount, notSubmittedCount, overdueNotSubmittedCount,
//              lateNoticeCount, submittedCount, remindableCount }
const summary = ref({});
const isLoading = ref(false);

// 서버 독촉 API 의 1회 요청 상한(WebLeavePromo01ServiceImpl.MAX_REMIND_TARGETS)과 동일.
// 초과분은 잘라서 보내고 남은 인원을 안내한다(요청 분할 연타로 스팸 발송되는 것을 막기 위함).
const REMIND_MAX_PER_REQUEST = 200;

// master/hr 여부 (그 외 권한은 사업장+소속부서 필수 — 2차 탭 동일)
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// 상태코드 → 뱃지 클래스 선언 맵(계산 없음. 라벨은 서버 statusNm 을 그대로 표시).
const STATUS_CLASS = {
  NOT_SUBMITTED: "status-pending",
  OVERDUE_NOT_SUBMITTED: "status-overdue",
  SUBMITTED: "status-done",
  LATE_SUBMITTED: "status-late-done",
};

// ── 표시 유틸 (표기 변환만. 날짜 계산 금지 — 서버 산출값 사용) ──
// YYYYMMDD → 'YYYY-MM-DD' (공용 util 재사용). 값이 없으면 '-'.
const fmtYmd = (v) => (proxy.$util.isEmpty(v) ? "-" : proxy.$util.formatDateString(v));
// 서버가 준 dDay(정수)를 부호만 보고 표기한다. 프론트에서 날짜를 다시 계산하지 않는다.
const fmtDDay = (d) => {
  if (d === null || d === undefined || d === "") return "";
  const n = Number(d);
  if (Number.isNaN(n)) return "";
  if (n > 0) return `D-${n}`;
  if (n === 0) return "D-day";
  return `D+${Math.abs(n)}`;
};
// '독촉 N회, 최종 MM-DD' (최종일이 없으면 횟수만).
const fmtRemind = (cnt, lastYmd) => {
  if (!cnt) return "";
  const base = `독촉 ${cnt}회`;
  if (proxy.$util.isEmpty(lastYmd) || String(lastYmd).length !== 8) return base;
  const s = String(lastYmd);
  return `${base}, 최종 ${s.substring(4, 6)}-${s.substring(6, 8)}`;
};

// ── 포커스/자동조회/팝업 (2차 탭 LeavePromotion_01_2 의 동일 함수를 미러) ──
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    } else {
      nodeNm.value = "";
      fnSrchNodeInfo();
    }
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
    } else {
      nodeCd.value = "";
      fnSrchNodeInfo();
    }
  }
};

// 사업장 자동조회 (코드/명 입력 후 blur) — 2차 탭 동일
const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) fnCallback(response);
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 부서 자동조회 (코드/명 입력 후 blur) — 2차 탭 동일
const fnSrchNodeInfo = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) return;
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    if (response.status === 200) {
      fnCallback({ ...response, config: { url: "/dummy/site-node-lists" } });
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 자동조회 응답 처리 — 0건/1건/다건 분기 (2차 탭 fnCallback 패턴)
const fnCallback = (res) => {
  if (!proxy.$util.isNotEmpty(res)) return;
  const apiId = res.config.url.split("/").pop();
  if (apiId === "site-lists") {
    const list = res.data?.siteInfoResultList ?? [];
    if (list.length === 1) {
      siteCd.value = list[0].siteCd;
      siteNo.value = list[0].siteNo;
      siteNm.value = list[0].siteNm;
      nodeDisabled.value = false;
      // 사업장 변경 시 소속부서 초기화(부서는 사업장 종속 — 팝업 선택 경로와 정합)
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
      nodeDisabled.value = true;
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } else if (apiId === "site-node-lists") {
    const list = res.data?.siteNodeInfoList || [];
    if (list.length === 0) {
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length === 1) {
      nodeCd.value = list[0].nodeCd ?? "";
      nodeNm.value = list[0].nodeNm ?? "";
    } else {
      fnSiteNodeSearchPopOpen();
    }
  }
};

// ── 사업장/부서 검색 팝업 (2차 탭 패턴) ────────────────────
const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
  nodeDisabled.value = false;
  nodeCd.value = "";
  nodeNm.value = "";
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSiteNodeSearchPopOpen = () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userCd_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// ── 조회 ───────────────────────────────────────────────────
const fnSearch = async () => {
  // 사업장 필수. master/hr 외에는 소속부서까지 필수(2차 탭 동일 스코프).
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_INPUT_REQUIRED));
    siteNoFcs.value?.focus();
    return;
  }
  if (!isMasterOrHr.value && proxy.$util.isEmpty(nodeCd.value)) {
    await proxy.$alert("소속 부서를 선택해 주세요.");
    return;
  }
  isLoading.value = true;
  try {
    // cmpnyCd/권한 스코프·siteCd 인가는 서버 JWT/원장 강제.
    // 상태·기한·D-day·독촉가능 여부는 전부 서버 산출값이라 프론트에서 재계산하지 않는다.
    const { data } = await axios.get("/webApi/leavepromo01/first-targets", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        userNm: userNm.value,
      },
    });
    rows.value = data?.targetList || [];
    summary.value = data?.summary || {};
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  } finally {
    isLoading.value = false;
  }
};

// ── 독촉 (단건/일괄) ───────────────────────────────────────
// 발송 전 Confirm → POST /remind → 결과 Alert → 재조회.
// 중복 클릭은 isLoading 으로 1차 차단하되, 최종 중복 방지 권위는 서버 DEDUP_KEY UNIQUE 다.
const fnRemind = async (userCds) => {
  if (!userCds || userCds.length === 0) return;
  const single = userCds.length === 1;
  const targetNm = single
    ? rows.value.find((r) => r.userCd === userCds[0])?.userNm ?? ""
    : "";
  const confirmMsg = single
    ? `${targetNm}님에게 연차 사용 계획 제출 독촉 알림을 보냅니다. ` +
      `(이미 보낸 사용촉진 통지에 대한 재안내이며, 법정 통지 기한은 다시 시작되지 않습니다.) 발송하시겠습니까?`
    : `미제출자 ${userCds.length}명에게 독촉 알림을 발송합니다. ` +
      `오늘 이미 발송한 대상은 자동 제외됩니다. 계속하시겠습니까?`;

  const ok = await proxy.$confirm(confirmMsg);
  if (!ok) return;

  isLoading.value = true;
  try {
    // siteCd 는 조회 중인 사업장(서버가 원장 인가를 재검증한 뒤에만 스코프로 사용).
    // 이걸 빼면 서버가 세션 사업장으로 폴백해 타 사업장 대상이 전건 스킵된다.
    const { data } = await axios.post("/webApi/leavepromo01/remind", {
      userCds: userCds,
      siteCd: siteCd.value,
    });
    await proxy.$alert(buildRemindResultMsg(data, single));
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "독촉 알림 발송 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
  // 성공/실패와 무관하게 최신 상태(독촉 횟수/최종일)로 갱신.
  await fnSearch();
};

// 발송 결과 문구 합성. 스킵 사유는 상위 2개만 병기(서버가 사유 라벨을 내려준다).
const buildRemindResultMsg = (data, single) => {
  const sent = data?.sentCount ?? 0;
  const skipped = data?.skippedCount ?? 0;
  const failed = data?.failedCount ?? 0;
  const items = data?.skippedItems || [];
  if (single) {
    if (sent > 0) return "독촉 알림 1건을 발송했습니다.";
    if (items.length > 0) return items[0].reasonNm || "발송 대상이 아닙니다.";
    return "독촉 알림을 발송하지 못했습니다.";
  }
  let msg = `발송 ${sent}건 / 스킵 ${skipped}건`;
  if (failed > 0) msg += ` / 실패 ${failed}건`;
  if (items.length > 0) {
    const reasons = [];
    items.forEach((it) => {
      const nm = it?.reasonNm;
      if (nm && !reasons.includes(nm)) reasons.push(nm);
    });
    if (reasons.length > 0) msg += `\n(${reasons.slice(0, 2).join(" / ")})`;
  }
  return msg;
};

// 미제출 전체 독촉 — 서버가 내려준 remindableYn 만 보고 대상을 모은다(프론트 재판정 금지).
const fnRemindAll = async () => {
  const list = rows.value
    .filter((r) => r.remindableYn === "Y")
    .map((r) => r.userCd);
  if (list.length === 0) {
    await proxy.$alert("독촉할 미제출자가 없습니다.");
    return;
  }
  if (list.length > REMIND_MAX_PER_REQUEST) {
    await proxy.$alert(
      `한 번에 최대 ${REMIND_MAX_PER_REQUEST}명까지 발송할 수 있습니다. ` +
        `상위 ${REMIND_MAX_PER_REQUEST}명만 발송하며, 나머지는 조회 조건을 좁혀 다시 발송해 주세요.`
    );
    await fnRemind(list.slice(0, REMIND_MAX_PER_REQUEST));
    return;
  }
  await fnRemind(list);
};

// ── 초기화 ─────────────────────────────────────────────────
const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    nodeDisabled.value = false;
    nodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
    nodeNm.value = sessionStorage.getItem("gv_nodeNm") ?? "";
  }
};

// 조회 전용 화면 — 생성/저장/삭제/엑셀 전부 숨김(엑셀 요구 없음).
const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

onMounted(() => {
  fnInit();
  fnButtonControll();
});
</script>

<style scoped>
/* ── 조회 영역 (2차 탭 패턴 차용) ── */
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
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
  accent-color: var(--color-primary);
  flex-shrink: 0;
}

/* ── 조회 영역 액션 (일괄 독촉) ── */
.lp1-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.lp1-remind-all-btn {
  height: 28px;
  padding: 0 0.6rem;
  border-radius: 4px;
  border: 1px solid var(--color-primary);
  background: var(--color-surface);
  color: var(--color-primary);
  font-size: 0.875rem;
  cursor: pointer;
}
.lp1-remind-all-btn:hover:not(:disabled) {
  border-color: var(--color-primary-hover);
  color: var(--color-primary-hover);
}
.lp1-remind-all-btn:disabled {
  border-color: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}

/* ── 본문 / 테이블 ── */
.lp1-body {
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
  overflow: hidden;
  min-height: 0;
}
.lp1-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-surface);
}
.lp1-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.lp1-table thead th {
  background: var(--thead-bg, var(--color-bg));
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.4rem;
  line-height: 1.2;
  position: sticky;
  top: 0;
  z-index: 1;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 600;
}
.lp1-table tbody td {
  border: 1px solid var(--color-border);
  padding: 0.4rem;
  text-align: center;
  white-space: nowrap;
  color: var(--color-text);
  vertical-align: middle;
}
.lp1-table tbody tr:hover {
  background: var(--color-bg);
}
.lp1-cell-left {
  text-align: left;
}
.lp1-empty {
  padding: 2rem;
  color: var(--color-text-muted);
  text-align: center;
}

/* ── 요약 뱃지 5종 (소제목 바 우측) ── */
.lp1-summary {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-left: auto;
  flex-wrap: wrap;
}
.lp1-sum {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  border: 1px solid transparent;
}
.lp1-sum b {
  font-weight: 700;
}
.lp1-sum--total {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border-color: var(--color-border-strong);
}
.lp1-sum--pending {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.lp1-sum--overdue {
  background: var(--color-danger);
  color: var(--color-surface);
}
/* 지연통지 = 회사 귀책 표식(정보성). 위험색을 쓰지 않는다. */
.lp1-sum--late-notice {
  background: var(--color-surface);
  color: var(--color-text-muted);
  border-color: var(--color-border-strong);
}
.lp1-sum--done {
  background: color-mix(in srgb, var(--color-primary) 15%, var(--color-surface));
  color: var(--color-primary-pressed);
}

/* ── 상태 뱃지 4종 (Attd_15 .a15-badge 패턴 — tokens.css 에 "주의-강" 토큰이 없어
     color-mix() 로 기존 토큰만 조합. 신규 하드코딩 hex 없음) ── */
.lp1-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status-pending {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.status-overdue {
  background: var(--color-danger);
  color: var(--color-surface);
}
.status-done {
  background: color-mix(in srgb, var(--color-primary) 15%, var(--color-surface));
  color: var(--color-primary-pressed);
}
.status-late-done {
  background: color-mix(in srgb, var(--color-danger) 35%, var(--color-warning-bg));
  color: color-mix(in srgb, var(--color-danger) 70%, var(--color-text-strong));
}

/* 지연통지 보조 뱃지 — 중립(Attd_15 .a15-provisional 패턴) */
.lp1-late-notice {
  display: inline-block;
  margin-left: 0.3rem;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 600;
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border-strong);
}

/* D-day 표기 */
.lp1-dday {
  margin-left: 0.3rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

/* ── 행 액션 (독촉) ── */
.lp1-remind-btn {
  height: 26px;
  padding: 0 0.6rem;
  border-radius: 4px;
  border: 0;
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 0.8rem;
  cursor: pointer;
}
.lp1-remind-btn:disabled {
  background: var(--color-border);
  color: var(--color-text-muted);
  cursor: default;
}
.lp1-remind-hist {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.7rem;
  color: var(--color-text-muted);
}
</style>
