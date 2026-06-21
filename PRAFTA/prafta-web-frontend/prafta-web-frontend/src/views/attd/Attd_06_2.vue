<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <!-- 조회 영역 -->
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
          <input type="checkbox" v-model="incSubNodeYn" />
          하위부서 조회
        </label>
      </div>
      <div>
        <label>교대타입</label>
        <select v-model="viewFilterType">
          <option value="">전체</option>
          <option v-for="t in shiftTypes" :key="t.code" :value="t.code">
            {{ t.name }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody a06-body">
      <!-- 배정 현황 카드 -->
      <div class="a06-view-card">
        <div class="a06-view-card-header">
          <span class="view-card-title">교대근무 배정 현황</span>
        </div>

        <!-- 빈 상태 -->
        <div
          v-if="filteredshiftTeamUserInfosResultList.length === 0"
          class="view-empty-state"
        >
          <div class="empty-title">조회된 배정 내역이 없습니다</div>
          <div class="empty-desc">
            조회 조건을 입력하고 조회 버튼을 눌러주세요.
          </div>
        </div>

        <!-- 그룹 목록 -->
        <div
          v-for="group in filteredshiftTeamUserInfosResultList"
          :key="group.shiftTeamId"
          class="view-group-section"
        >
          <div
            class="view-group-header"
            :class="{ collapsed: !group.expanded }"
            @click="toggleGroupExpand(group.shiftTeamId)"
          >
            <span class="group-chevron">▼</span>
            <!-- prafta-com-013-05-5(2): 코드값(식별자)은 교대근무팀ID(shiftTeamId)를 읽기전용으로 표시.
                 편집은 명칭(shiftTeamNm)만 한다(아래 group-title-text). -->
            <span class="group-code-badge">{{ group.shiftTeamId }}</span>
            <template v-if="editingTeamNmId === group.shiftTeamId">
              <input
                ref="editTeamNmInputRef"
                v-model="editingTeamNmValue"
                class="group-title-input"
                maxlength="50"
                :placeholder="group.shiftTeamNm"
                @click.stop
                @keyup.enter="confirmTeamNmEdit(group)"
                @keyup.esc="cancelTeamNmEdit"
              />
              <button
                class="team-nm-save-btn"
                title="저장"
                @click.stop="confirmTeamNmEdit(group)"
              >
                <svg
                  width="11"
                  height="11"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </button>
              <button
                class="team-nm-cancel-btn"
                title="취소"
                @click.stop="cancelTeamNmEdit"
              >
                <svg
                  width="11"
                  height="11"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </template>
            <span
              v-else
              class="group-title-text editable"
              title="팀명 수정"
              @click.stop="openTeamNmEdit(group)"
            >
              {{ group.shiftTeamNm }}
              <svg
                class="group-title-pencil"
                width="11"
                height="11"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"
                />
                <path
                  d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"
                />
              </svg>
            </span>
            <span class="group-cycle-tag">{{ group.shiftNo }}</span>
            <div class="group-period-tag">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                width="11"
                height="11"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              {{ formatYmdDot(group.strDate) }} ~
              {{ formatYmdDot(group.endDate) }}
              <button
                class="period-edit-btn"
                title="기간 수정"
                @click.stop="openPeriodEdit(group)"
              >
                <svg
                  width="11"
                  height="11"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path
                    d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"
                  />
                  <path
                    d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"
                  />
                </svg>
              </button>
            </div>
            <span class="group-member-cnt">
              총 <strong>{{ getTotalMembers(group) }}명</strong> /
              {{ group.teams.length }}개 조
            </span>
            <button
              class="btn btn-sm a06-btn-danger"
              :disabled="!isGroupDeletable(group)"
              @click.stop="deleteAssignment(group)"
            >
              삭제
            </button>
          </div>

          <div v-show="group.expanded" class="view-group-body">
            <div
              v-for="team in group.teams"
              :key="team.teamIdx"
              class="view-team-card"
            >
              <div class="view-team-header">
                <div
                  class="view-team-badge"
                  :style="teamBadgeStyle(team.teamNm)"
                >
                  {{ team.teamNm }}
                </div>
                <span class="view-team-name">{{ team.teamNm }}</span>
                <span v-if="getLeaderName(team)" class="view-team-leader">
                  ★ {{ getLeaderName(team) }}
                </span>
                <span class="view-team-cnt">{{ team.members.length }}명</span>
                <button
                  class="team-member-btn team-member-add-btn"
                  title="사용자 추가"
                  @click.stop="fnAddUsersToTeam(group, team)"
                >
                  <svg
                    width="11"
                    height="11"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </button>
              </div>
              <div class="view-team-body">
                <div v-if="team.members.length === 0" class="view-no-member">
                  배정 없음
                </div>
                <div
                  v-for="member in team.members"
                  :key="member.userCd"
                  class="view-member-row"
                >
                  <div
                    class="view-member-avatar"
                    :style="avatarMiniStyle(team.teamNm)"
                  >
                    {{ member.userNm?.[0] }}
                  </div>
                  <span class="view-member-name">{{ member.userNm }}</span>
                  <button
                    class="leader-toggle-btn"
                    :class="{ active: member.leaderYn === 'Y' }"
                    :title="member.leaderYn === 'Y' ? '조장 해제' : '조장 지정'"
                    :disabled="member.editableYn !== 'Y'"
                    @click.stop="fnToggleLeader(member)"
                  >
                    <svg
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      :fill="member.leaderYn === 'Y' ? 'currentColor' : 'none'"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <polygon
                        points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
                      />
                    </svg>
                  </button>
                  <span class="view-member-dept">{{ member.nodeNm }}</span>
                  <button
                    class="member-remove-btn"
                    title="사용자 제거"
                    :disabled="member.editableYn !== 'Y'"
                    @click.stop="fnRemoveUserFromTeam(group, team, member)"
                  >
                    <svg
                      width="10"
                      height="10"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 기간 수정 인라인 모달 -->
    <Transition name="fade">
      <div
        v-if="showPeriodModal"
        class="a06-overlay"
        @click.self="showPeriodModal = false"
      >
        <div class="a06-modal">
          <div class="a06-modal-header">
            <span>적용 기간 수정</span>
            <button class="icon-button" @click="showPeriodModal = false">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-6 h-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
          <div class="a06-modal-body">
            <div class="a06-modal-field">
              <label>시작일</label>
              <CalendarSrch v-model="editStart" class="a06-date-input" />
            </div>
            <div class="a06-modal-field">
              <label>종료일</label>
              <CalendarSrch v-model="editEnd" class="a06-date-input" />
            </div>
            <p class="a06-modal-notice">
              ※ 마감된 기간의 변경은 적용되지 않습니다.
            </p>
            <p class="a06-modal-notice">
              ※ 연장된 기간에만 교대 근무계획이 추가 생성되며, 기존 스케줄은 유지됩니다.
            </p>
          </div>
          <div class="a06-modal-footer">
            <button
              class="btn btn-sm btn-second"
              @click="showPeriodModal = false"
            >
              취소
            </button>
            <button class="btn btn-sm btn-primary" @click="fnSave">저장</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  nextTick,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineExpose,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import { formatYmdDot } from "@/utils/dateFormat";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import UsersMultiSearchPop from "@/components/popup/UsersMultiSearchPop.vue";
import ShiftLeaveNoticePop from "@/components/popup/ShiftLeaveNoticePop.vue";

defineOptions({ name: "Attd_06_2" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

const fnButtonControll = () => {
  localButtons.value.create = "N";
  localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

// ── 조회 조건 ─────────────────────────────────────────────
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteDisabled = ref(false);
const nodeCd = ref("");
const nodeNm = ref("");
const nodeDisabled = ref(true);
const incSubNodeYn = ref(false);
const viewFilterType = ref("");

const siteNoFcs = ref(null);

// ── 사업장 포커스 종료 처리 ───────────────────────────────
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

// ── 사업장 조회 ──────────────────────────────────────────
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
    await proxy.$alert(resolveApiErrorMessage(err, "조회 오류"));
  }
};

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
    await proxy.$alert(resolveApiErrorMessage(err, "조회 오류"));
  }
};

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
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
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

// ── 교대타입 목록 ─────────────────────────────────────────
const shiftTypes = ref([
  {
    code: "2J2G",
    name: "2조 2교대",
    teams: 2,
    cycle: 2,
    pattern: [
      ["D", "N"],
      ["N", "D"],
    ],
  },
  {
    code: "3J2G",
    name: "3조 2교대",
    teams: 3,
    cycle: 3,
    pattern: [
      ["D", "N", "O"],
      ["O", "D", "N"],
      ["N", "O", "D"],
    ],
  },
  {
    code: "3J3G",
    name: "3조 3교대",
    teams: 3,
    cycle: 3,
    pattern: [
      ["D", "E", "N"],
      ["N", "D", "E"],
      ["E", "N", "D"],
    ],
  },
  {
    code: "4J3G",
    name: "4조 3교대",
    teams: 4,
    cycle: 4,
    pattern: [
      ["D", "E", "N", "O"],
      ["O", "D", "E", "N"],
      ["N", "O", "D", "E"],
      ["E", "N", "O", "D"],
    ],
  },
  {
    code: "4J4G",
    name: "4조 4교대",
    teams: 4,
    cycle: 4,
    pattern: [
      ["D", "D", "O", "O"],
      ["O", "D", "D", "O"],
      ["O", "O", "D", "D"],
      ["D", "O", "O", "D"],
    ],
  },
  { code: "DAY", name: "주간 고정", teams: 1, cycle: 1, pattern: [["D"]] },
  { code: "NIGHT", name: "야간 고정", teams: 1, cycle: 1, pattern: [["N"]] },
]);

const fmtDisplayDate = (d) => {
  if (!d) return "";
  if (d.includes("-")) return d;
  return `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`;
};

// ── 배정 현황 ─────────────────────────────────
const shiftTeamUserInfosResultList = ref([]);

const filteredshiftTeamUserInfosResultList = computed(() =>
  shiftTeamUserInfosResultList.value.filter(
    (g) => !viewFilterType.value || g.shiftCd === viewFilterType.value
  )
);

// ── 조회 ─────────────────────────────────────────────────
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "사업장" })
    );
    siteNoFcs.value?.focus();
    return;
  }

  try {
    const res = await axios.get("/webApi/attd06/shift-team-user-infos", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
        shiftCd: viewFilterType.value,
      },
    });

    console.log(res.data.shiftTeamUserInfosResultList);

    const list = res.data.shiftTeamUserInfosResultList ?? [];
    const groupMap = new Map();
    list.forEach((row) => {
      if (!groupMap.has(row.shiftTeamId)) {
        groupMap.set(row.shiftTeamId, {
          shiftTeamId: row.shiftTeamId,
          shiftTeamNm: row.shiftTeamNm,
          shiftCd: row.shiftCd,
          shiftNo: row.shiftNo,
          strDate: row.strDate,
          endDate: row.endDate,
          expanded: true,
          teams: new Map(),
        });
      }
      const group = groupMap.get(row.shiftTeamId);
      if (!group.teams.has(row.teamIdx)) {
        group.teams.set(row.teamIdx, {
          teamIdx: row.teamIdx,
          teamNm: row.teamNm,
          members: [],
        });
      }
      group.teams.get(row.teamIdx).members.push({
        userCd: row.userCd,
        userNm: row.userNm,
        nodeNm: row.nodeNm,
        leaderYn: row.leaderYn,
        editableYn: row.editableYn,
        // prafta-com-016-D 보안 재작업: 조장 토글 WHERE 를 교대팀 단위로 좁히기 위한 키(서버 가드 1차 + 키 정합).
        shiftCd: row.shiftCd,
        shiftTeamId: row.shiftTeamId,
        teamIdx: row.teamIdx,
      });
    });
    shiftTeamUserInfosResultList.value = [...groupMap.values()].map((g) => ({
      ...g,
      teams: [...g.teams.values()],
    }));
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const toggleGroupExpand = (shiftTeamId) => {
  const group = shiftTeamUserInfosResultList.value.find(
    (a) => a.shiftTeamId === shiftTeamId
  );
  if (group) group.expanded = !group.expanded;
};

const getShiftTypeName = (code) =>
  shiftTypes.value.find((t) => t.code === code)?.name || code;

const getTotalMembers = (group) =>
  group.teams.reduce((s, t) => s + t.members.length, 0);

const isGroupDeletable = (group) =>
  group.teams.every((t) => t.members.every((m) => m.editableYn === "Y"));

const getLeaderName = (team) => {
  const leader = team.members.find((m) => m.leaderYn === "Y");
  return leader?.userNm || null;
};

const deleteAssignment = async (group) => {
  // prafta-com-016-D-6: 교대타입명이 아닌 교대근무 팀명(shiftTeamNm) 표시. 빈값이면 팀ID 폴백.
  const teamNm = group.shiftTeamNm || group.shiftTeamId;
  const ok = await proxy.$confirm(
    getMessage(MSG.SHIFT_TEAM_DELETE_CONFIRM, { teamNm })
  );
  if (!ok) return;
  try {
    const res = await axios.post("/webApi/attd06/delete-shift-teams", {
      siteCd: siteCd.value,
      shiftCd: group.shiftCd,
      shiftTeamId: group.shiftTeamId,
    });
    if (res.status === 200) {
      await proxy.$alert(getMessage(MSG.DELETE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.")
    );
  }
};

// ── 교대근무 팀명 수정 ────────────────────────────────────
const editingTeamNmId = ref(null);
const editingTeamNmValue = ref("");
const editTeamNmInputRef = ref(null);

const openTeamNmEdit = (group) => {
  editingTeamNmId.value = group.shiftTeamId;
  editingTeamNmValue.value = group.shiftTeamNm ?? "";
  nextTick(() => {
    const el = Array.isArray(editTeamNmInputRef.value)
      ? editTeamNmInputRef.value[0]
      : editTeamNmInputRef.value;
    el?.focus();
    el?.select();
  });
};

const cancelTeamNmEdit = () => {
  editingTeamNmId.value = null;
  editingTeamNmValue.value = "";
};

const confirmTeamNmEdit = async (group) => {
  const newNm = editingTeamNmValue.value.trim();
  if (!newNm) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "교대근무 팀명" })
    );
    return;
  }
  if (newNm === group.shiftTeamNm) {
    cancelTeamNmEdit();
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const res = await axios.post("/webApi/attd06/update-shift-team-nms", {
      siteCd: siteCd.value,
      shiftCd: group.shiftCd,
      shiftTeamId: group.shiftTeamId,
      shiftTeamNm: newNm,
    });
    if (res.status === 200) {
      const target = shiftTeamUserInfosResultList.value.find(
        (g) => g.shiftTeamId === group.shiftTeamId
      );
      if (target) target.shiftTeamNm = newNm;
      cancelTeamNmEdit();
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "팀명 수정 중 오류가 발생했습니다.")
    );
  }
};

// ── 조원 추가/제거 ────────────────────────────────────────
const fnAddUsersToTeam = (group, team) => {
  openPop(UsersMultiSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    onSelect: async (selectedUsers) => {
      if (!selectedUsers?.length) return;
      // prafta-com-016-D-3: 덮어쓰기 범위(합류일 다음날 ~ 교대근무 종료일) 안내 confirm.
      //   실제 기준일은 서버 산출(클라 today 신뢰 금지) — 종료일만 안내용으로 표기.
      const ok = await proxy.$confirm(
        getMessage(MSG.SHIFT_USER_ADD_CONFIRM, {
          endDate: fmtDisplayDate(group.endDate),
        })
      );
      if (!ok) return;
      fnInsertShiftTeamUsers(group, team, selectedUsers);
    },
  });
};

const fnRemoveUserFromTeam = async (group, team, member) => {
  // prafta-com-013-05-5(3): 조원 제거 버튼 동작 + Confirm 문구를 제거 의미로 교정(기존 "저장하시겠습니까?" -> 제외 안내).
  const ok = await proxy.$confirm(
    `[${member.userNm}] 님을 교대팀에서 제외하시겠습니까?`
  );
  if (!ok) return;
  fnDeleteShiftTeamUser(group, team, member);
};

const fnInsertShiftTeamUsers = async (group, team, users) => {
  if (!users?.length) return;

  const payload = users.map((u) => ({
    siteCd: siteCd.value,
    shiftCd: group.shiftCd,
    shiftTeamId: group.shiftTeamId,
    teamIdx: team.teamIdx,
    userCd: u.userCd,
  }));

  try {
    const res = await axios.post(
      "/webApi/attd06/insert-shift-team-users",
      payload
    );
    if (res.status === 200) {
      // prafta-com-016-D-3/D-4: 연차(any unit)/OT 가 있어 덮어쓰기에서 제외(보존)된 날짜가 있으면 팝업 안내.
      const blockedList = res.data?.blockedList ?? [];
      fnSearch();
      if (blockedList.length > 0) {
        openPop(ShiftLeaveNoticePop, {
          rows: buildBlockedRows(blockedList, group, users),
        });
      } else {
        await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.")
    );
  }
};

const fnToggleLeader = async (member) => {
  const newLeaderYn = member.leaderYn === "Y" ? "N" : "Y";
  const msg =
    member.leaderYn === "Y"
      ? "해당 사용자의 조장 지정을 해지하시겠습니까 ?"
      : "해당 사용자를 조장으로 지정하시겠습니까 ?";
  const ok = await proxy.$confirm(msg);
  if (!ok) return;

  try {
    const res = await axios.post("/webApi/attd06/update-shift-team-leaders", {
      siteCd: siteCd.value,
      userCd: member.userCd,
      leaderYn: newLeaderYn,
      // prafta-com-016-D 보안 재작업: 팀 단위 키 동봉(서버 WHERE 범위 축소).
      shiftCd: member.shiftCd,
      shiftTeamId: member.shiftTeamId,
      teamIdx: member.teamIdx,
    });
    if (res.status === 200) {
      fnSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.")
    );
  }
};

const fnDeleteShiftTeamUser = async (group, team, member) => {
  const payload = {
    siteCd: siteCd.value,
    shiftCd: group.shiftCd,
    shiftTeamId: group.shiftTeamId,
    teamIdx: team.teamIdx,
    userCd: member.userCd,
  };

  try {
    const res = await axios.post(
      "/webApi/attd06/delete-shift-team-users",
      payload
    );
    if (res.status === 200) {
      // prafta-com-013-05(재작업) 결함④: 멤버 제외는 "저장"이 아니라 "제외" 의미의 문구로 안내.
      await proxy.$alert(getMessage(MSG.SHIFT_TEAM_USER_EXCLUDED));
      fnSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.")
    );
  }
};

// ── 기간 수정 ─────────────────────────────────────────────
const showPeriodModal = ref(false);
const editTarget = ref(null);
const editStart = ref("");
const editEnd = ref("");

const openPeriodEdit = (group) => {
  editTarget.value = group;
  editStart.value = fmtDisplayDate(group.strDate);
  editEnd.value = fmtDisplayDate(group.endDate);
  showPeriodModal.value = true;
};

const fnSave = async () => {
  if (!editStart.value || !editEnd.value) {
    await proxy.$alert("기간을 입력해 주세요.");
    return;
  }
  if (editStart.value > editEnd.value) {
    await proxy.$alert("종료일은 시작일보다 이후여야 합니다.");
    return;
  }

  // prafta-com-016-D-5: 연장만 반영·단축 미변경 안내 confirm.
  const ok = await proxy.$confirm(getMessage(MSG.SHIFT_PERIOD_CHANGE_CONFIRM));
  if (!ok) return;

  // prafta-com-013-05-2(재작업): 기간 수정은 전 구간 재생성(update-shift-user-sch-infos)이 아니라
  //   update-shift-team-periods 로 일원화한다.
  //   - STR_DATE/END_DATE 갱신 + 연장 구간만 계획 생성(순환 위상은 원래 시작일 기준 보존)
  //     + 단축분/기존 수기 편집분 보존을 한 경로에서 일관 처리.
  //   - 응답 blockedList = 연장 구간 중 (연차+교대 휴무) 겹쳐 덮어쓰기 제외된 날짜.
  const saveData = {
    siteCd: siteCd.value,
    shiftCd: editTarget.value?.shiftCd,
    shiftTeamId: editTarget.value?.shiftTeamId,
    strDate: editStart.value.replaceAll("-", ""),
    endDate: editEnd.value.replaceAll("-", ""),
  };

  try {
    const res = await axios.post(
      "/webApi/attd06/update-shift-team-periods",
      saveData
    );
    if (res.status === 200) {
      showPeriodModal.value = false;
      // prafta-com-016-D-4/D-5: 연장 구간 중 연차(any unit)/OT 로 덮어쓰기 제외(보존)된 날짜가 있으면 팝업 안내.
      const blockedList = res.data?.blockedList ?? [];
      const group = editTarget.value;
      fnSearch();
      if (blockedList.length > 0) {
        openPop(ShiftLeaveNoticePop, { rows: buildBlockedRows(blockedList, group) });
      } else {
        await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.")
    );
  }
};

// ── 보존(연차/OT) 안내 행 빌더 (prafta-com-016-D-4) ───────
// blockedList(BE) 각 항목에 사용자명(userNm)을 붙여 ShiftLeaveNoticePop 에 넘긴다.
//   userNm 출처: 그룹의 현 소속 멤버 + (조원 추가 경로) 추가 대상 사용자 목록.
const buildBlockedRows = (blockedList, group, extraUsers) => {
  const userNmMap = {};
  (group?.teams ?? []).forEach((t) => {
    (t.members ?? []).forEach((m) => {
      userNmMap[m.userCd] = m.userNm;
    });
  });
  (extraUsers ?? []).forEach((u) => {
    if (u?.userCd) userNmMap[u.userCd] = u.userNm;
  });
  return (blockedList ?? []).map((b) => ({
    userCd: b.userCd,
    userNm: userNmMap[b.userCd] ?? b.userCd,
    workYmd: b.workYmd,
    reason: b.reason,
    dayType: b.dayType,
    leaveUseUnitType: b.leaveUseUnitType,
  }));
};

// ── 팀 컬러 ──────────────────────────────────────────────
const TEAM_COLORS = {
  A: { bg: "#EEF2FF", br: "#C7D2FE", tx: "#3730A3" },
  B: { bg: "#FCE7F3", br: "#F9A8D4", tx: "#9D174D" },
  C: { bg: "#FEF3C7", br: "#FCD34D", tx: "#854D0E" },
  D: { bg: "#DCFCE7", br: "#86EFAC", tx: "#166534" },
  E: { bg: "#E0F2FE", br: "#7DD3FC", tx: "#075985" },
  F: { bg: "#F5F3FF", br: "#C4B5FD", tx: "#5B21B6" },
};
const tc = (key) => TEAM_COLORS[key] || TEAM_COLORS.A;

const teamBadgeStyle = (key) => ({ background: tc(key).bg, color: tc(key).tx });
const avatarMiniStyle = (key) => ({
  background: tc(key).bg,
  color: tc(key).tx,
  border: `1px solid ${tc(key).br}`,
});

// ── 초기화 ───────────────────────────────────────────────
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

onMounted(() => {
  fnInit();
  fnButtonControll();
});

defineExpose({ refresh: fnSearch });
</script>

<style scoped>
/* ── 체크박스 라벨 ────────────────────────────────────────── */
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

.a06-body {
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 0;
  gap: 0;
}

.a06-view-card {
  margin: 12px 16px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
}
.a06-view-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg);
}
.view-card-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong);
}
.view-card-sub {
  font-size: 12px;
  color: var(--color-text-muted);
}
.view-card-actions {
  margin-left: auto;
  display: flex;
  gap: 7px;
}

.view-empty-state {
  padding: 48px 20px;
  text-align: center;
  color: var(--color-text-muted);
}
.empty-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 3px;
}
.empty-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.view-group-section {
  border-bottom: 1px solid var(--color-border);
}
.view-group-section:last-child {
  border-bottom: none;
}

.view-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  background: #fff;
  cursor: pointer;
  transition: background 0.1s;
  flex-wrap: wrap;
}
.view-group-header:hover {
  background: var(--color-bg);
}

.group-chevron {
  color: var(--color-text-muted);
  font-size: 11px;
  transition: transform 0.15s;
}
.view-group-header.collapsed .group-chevron {
  transform: rotate(-90deg);
}

.group-code-badge {
  padding: 2px 8px;
  background: rgba(22, 163, 74, 0.12);
  color: var(--color-primary);
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;
}
.group-title-text {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.group-title-text.editable {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.1s;
}
.group-title-text.editable:hover {
  background: rgba(22, 163, 74, 0.08);
}
.group-title-text.editable:hover .group-title-pencil {
  opacity: 1;
}
.group-title-pencil {
  color: var(--color-primary);
  opacity: 0.5;
  transition: opacity 0.1s;
}
.group-title-input {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong);
  padding: 2px 8px;
  min-width: 160px;
  height: 26px;
  border: 1px solid var(--color-primary);
  border-radius: 5px;
  outline: none;
  background: #fff;
  font-family: "Pretendard", sans-serif;
}
.group-title-input:focus {
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.15);
}
.team-nm-save-btn,
.team-nm-cancel-btn {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  border: 1px solid var(--color-border);
  background: #fff;
  cursor: pointer;
  transition:
    background 0.1s,
    color 0.1s;
}
.team-nm-save-btn {
  color: var(--color-primary);
}
.team-nm-save-btn:hover {
  background: rgba(22, 163, 74, 0.12);
  border-color: var(--color-primary);
}
.team-nm-cancel-btn {
  color: #dc2626;
}
.team-nm-cancel-btn:hover {
  background: rgba(220, 38, 38, 0.1);
  border-color: #dc2626;
}
.group-cycle-tag {
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 2px 8px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 9999px;
}
.group-period-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: rgba(22, 163, 74, 0.04);
  border: 1px solid rgba(22, 163, 74, 0.2);
  border-radius: 6px;
  font-size: 11px;
  color: var(--color-primary);
  font-weight: 600;
}
.period-edit-btn {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-primary);
}
.period-edit-btn:hover {
  background: rgba(22, 163, 74, 0.12);
}

.group-member-cnt {
  font-size: 12px;
  color: var(--color-text-muted);
}
.group-member-cnt strong {
  color: var(--color-text-strong);
}

.a06-btn-danger {
  background: transparent;
  border: 1px solid var(--color-border);
  color: #ef4444;
  margin-left: auto;
}
.a06-btn-danger:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #fca5a5;
}
.a06-btn-danger:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.view-group-body {
  padding: 12px 16px 14px;
  background: rgba(22, 163, 74, 0.03);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.view-team-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 9px;
  overflow: hidden;
}
.view-team-header {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 10px;
  border-bottom: 1px solid var(--color-border);
}
.view-team-badge {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 800;
  flex-shrink: 0;
}
.view-team-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-strong);
}
.view-team-leader {
  font-size: 10px;
  color: #d97706;
  font-weight: 700;
  padding: 1px 5px;
  background: #fef3c7;
  border-radius: 9999px;
}
.view-team-cnt {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-muted);
}
.team-member-btn {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  border: 1px solid var(--color-border);
  background: #fff;
  cursor: pointer;
  transition:
    background 0.1s,
    color 0.1s;
}
.team-member-add-btn {
  color: var(--color-primary);
}
.team-member-add-btn:hover {
  background: rgba(22, 163, 74, 0.12);
  border-color: var(--color-primary);
}
.member-remove-btn {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  border: 1px solid var(--color-border);
  background: #fff;
  cursor: pointer;
  color: #dc2626;
  transition:
    background 0.1s,
    color 0.1s;
}
.member-remove-btn:hover:not(:disabled) {
  background: rgba(220, 38, 38, 0.1);
  border-color: #dc2626;
}
.member-remove-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.view-team-body {
  padding: 7px 9px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: 160px;
  overflow-y: auto;
}
.view-no-member {
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
  padding: 6px 0;
}

.view-member-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
}
.view-member-avatar {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 700;
  flex-shrink: 0;
}
.view-member-name {
  font-weight: 500;
  color: var(--color-text-strong);
}
.leader-toggle-btn {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  transition:
    color 0.1s,
    background 0.1s;
}
.leader-toggle-btn.active {
  color: #f59e0b;
}
.leader-toggle-btn:hover:not(:disabled) {
  background: #fef3c7;
  color: #f59e0b;
}
.leader-toggle-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
.view-member-dept {
  margin-left: auto;
  font-size: 10px;
  color: var(--color-text-muted);
}

/* ── 기간 수정 인라인 모달 ──────────────────────────────── */
.a06-overlay {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.35);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}
.a06-modal {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  width: 380px;
  max-width: calc(100vw - 40px);
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(16, 24, 40, 0.12);
}
.a06-modal-header {
  height: 46px;
  padding: 0 16px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong);
}
.a06-modal-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.a06-modal-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.a06-modal-field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
}
/* 네이티브 date input → CalendarSrch 교체. 내부 input 셀렉터로 기존 사이즈 유지 */
.a06-date-input :deep(.calendar-input) {
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 13px;
  background: #fff;
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}
.a06-modal-notice {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.5;
}
.a06-modal-footer {
  padding: 11px 16px;
  background: var(--color-bg);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: 7px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
