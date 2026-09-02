<!--
  Location_01.vue — 위치정보 동의 현황 (위치정보 동의철회·중지 S5)

  - 메뉴: tb_syst_menu_d MENU_D_ID='Location_01', MENU_VIEW='location/Location_01.vue'
  - 백엔드: GET /webApi/location01/consent-status-lists  (사용자별 4-state 현황)
            GET /webApi/location01/consent-histories     (전이 이력 + 파기 이력)
  - 조회 전용. ★관리자가 타인의 동의 상태를 바꾸는 기능은 만들지 않는다 —
    철회는 되돌릴 수 없는 파기를 동반하므로 본인만 수행한다.
  - 인가: 서버가 assertSiteAccess + isManager(master/hr) + canManageNode 로 3중 강제한다.
    LNB 미노출은 방어가 아니므로 화면 쪽 가드에 의존하지 않는다.
  - 레이아웃: 표준 목록 화면 구조(viewSearch → viewBody → table-wrapper/subtitle-pane →
    table-box → data-grid)를 따른다. 조회 조건 체크박스는 Attd_08/11/15·User_05 의
    .checkbox-label 패턴, 상태 배지는 Subcon_01/Attd_14 의 .status-badge 패턴을 차용한다.
-->
<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 — 사업장만 받는다. 부서 범위는 서버가 세션 클레임을 앵커로 강제하므로
         화면에서 넓힐 수 있는 입력을 두지 않는다. -->
    <div class="viewSearch">
      <div>
        <label>사업장</label>
        <input
          id="siteNo"
          type="text"
          v-model="siteNo"
          placeholder="사업장코드"
          @blur="focusKill"
        />
        <button class="search-btn" @click="fnSiteSearchPopOpen()">
          <img class="search_icon" :src="search_icon" alt="검색" />
        </button>
        <input
          id="siteNm"
          type="text"
          v-model="siteNm"
          placeholder="사업장명"
          @blur="focusKill"
        />
      </div>
      <div>
        <label class="checkbox-label">
          <input type="checkbox" v-model="issueOnly" />
          확인이 필요한 사람만 보기
        </label>
      </div>
    </div>

    <!-- 본문: 사용자 1명 = 1행 (동의 4-state 현황) -->
    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <!-- 소제목 바 (User_01 패턴 차용) -->
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">위치정보 동의 현황</span>
          </div>
        </div>

        <p class="hint">
          위치정보 제공·이용 동의를 <b>철회</b>하거나 <b>중지</b>한 근로자는
          출퇴근·TBM 참석을 이용할 수 없습니다. 재동의는 본인만 할 수 있습니다.
        </p>

        <div
          class="table-box"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="text-align: center; width: 4%">No</th>
                <th style="width: 18%">사용자</th>
                <th style="text-align: center; width: 8%">구분</th>
                <th style="width: 16%">부서</th>
                <th style="text-align: center; width: 12%">동의 상태</th>
                <th style="text-align: center; width: 18%">최근 변경</th>
                <th style="text-align: center; width: 12%">파기 이력</th>
                <th style="text-align: center; width: 12%">상세</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!statusList || statusList.length === 0">
                <tr>
                  <td colspan="8" class="edu-grid-empty">
                    조회된 내역이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="(row, idx) in statusList"
                  :key="row.userTypeCd + row.userCd"
                >
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>{{ userLabel(row) }}</td>
                  <td style="text-align: center">
                    {{ row.userTypeCd === "DAILY" ? "일용직" : "정규" }}
                  </td>
                  <td>{{ row.nodeNm || "-" }}</td>
                  <td style="text-align: center">
                    <span
                      class="status-badge"
                      :class="stateClass(row.consentState)"
                    >
                      {{ stateLabel(row.consentState) }}
                    </span>
                  </td>
                  <td style="text-align: center">
                    {{ row.lastActionDtime || "-" }}
                  </td>
                  <td style="text-align: center">
                    {{ row.purgeCount > 0 ? row.purgeCount + "건" : "-" }}
                  </td>
                  <td style="text-align: center">
                    <button
                      class="btn btn-sm btn-second"
                      @click="fnOpenHist(row)"
                    >
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
import {
  ref,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import LocationConsentHistPop from "@/views/location/popup/LocationConsentHistPop.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";

defineOptions({ name: "Location_01" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });
// 조회 전용 화면 — 생성/저장/삭제/엑셀 버튼을 노출하지 않는다.
(() => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
})();

const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
// 기본값 = 확인이 필요한 사람만. 이 화면의 목적이 "지금 누가 막혀 있는가" 이기 때문이다.
const issueOnly = ref(true);
const statusList = ref([]);

// 4-state 표시 — utils 로 빼지 않고 화면에 둔다(웹에서 쓰는 곳이 여기뿐).
const STATE_LABEL = {
  AGREED: "동의",
  SUSPENDED: "일시 중지",
  PENDING_REAGREE: "재동의 필요",
  WITHDRAWN: "동의 철회",
};
// 사용자 표기 — "사용자명(사용자ID)". 동명이인 구분을 위해 ID 를 함께 보인다.
//   ID 가 없으면(구 데이터) 이름만, 이름도 없으면 사용자코드로 폴백한다.
const userLabel = (row) => {
  const nm = row?.userNm || "";
  const id = row?.userId || "";
  if (nm && id) return `${nm}(${id})`;
  return nm || id || row?.userCd || "-";
};

const stateLabel = (state) => STATE_LABEL[state] || state || "-";
const stateClass = (state) => ({
  "is-agreed": state === "AGREED",
  "is-suspended": state === "SUSPENDED",
  "is-pending": state === "PENDING_REAGREE",
  "is-withdrawn": state === "WITHDRAWN",
});

const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  }
};

const fnSrchSiteInfo = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    if (response.status === 200) {
      const list = response.data?.siteInfoList ?? [];
      if (list.length === 1) {
        onSiteSelected(list[0].siteCd, list[0].siteNo, list[0].siteNm);
      } else {
        siteCd.value = "";
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const onSiteSelected = (siteCdVal, siteNoVal, siteNmVal) => {
  siteCd.value = siteCdVal;
  siteNo.value = siteNoVal;
  siteNm.value = siteNmVal;
};

const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: onSiteSelected,
  });
};

const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(getMessage(MSG.SITE_REQUIRED_FIRST));
    return;
  }
  try {
    const response = await axios.get(
      "/webApi/location01/consent-status-lists",
      {
        params: {
          siteCd: siteCd.value,
          stateFilter: issueOnly.value ? "ISSUE" : "",
        },
      }
    );
    if (response.status === 200) {
      statusList.value = response.data?.consentStatusList ?? [];
    }
  } catch (err) {
    statusList.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

const fnOpenHist = (row) => {
  openPop(LocationConsentHistPop, {
    siteCd_p: siteCd.value,
    userCd_p: row.userCd,
    userNm_p: row.userNm,
  });
};

onMounted(() => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) fnSearch();
});
</script>

<style scoped>
/* ── 조회 영역 체크박스 (Attd_08/11/15·User_05 패턴 차용) ─────────── */
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

/* ── 안내 문구 (소제목 바 아래) ──────────────────────────────────── */
.hint {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--color-text-muted, #4b5563);
  word-break: keep-all;
}

/* ── 동의 상태 배지 (Subcon_01/Attd_14 .status-badge 패턴 차용) ─────
   동의=primary 연한 배경, 일시 중지=warning, 재동의 필요=중립(테두리),
   동의 철회=danger 강조. */
.status-badge {
  display: inline-block;
  min-width: 48px;
  padding: 0.1rem 0.5rem;
  border-radius: var(--btn-radius, 8px);
  font-size: var(--btn-font-sm, 11px);
  font-weight: 600;
  line-height: 1.4;
  text-align: center;
  white-space: nowrap;
}
.status-badge.is-agreed {
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
}
.status-badge.is-suspended {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
}
.status-badge.is-pending {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #4b5563);
  border: 1px solid var(--color-border, #e5e7eb);
}
.status-badge.is-withdrawn {
  background: var(--color-danger, #ef4444);
  color: var(--color-surface, #ffffff);
}
</style>
