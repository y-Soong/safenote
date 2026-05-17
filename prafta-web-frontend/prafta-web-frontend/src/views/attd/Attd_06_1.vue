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
    </div>

    <div class="viewBody a06-body">
      <!-- STEP 1: 교대타입 카드 목록 (조회 후 표시, 미리보기 시 접힘) -->
      <Transition name="crew-accordion">
        <div
          v-show="shiftTypes.length > 0 && showCrewWrap"
          class="a06-type-list"
        >
          <div
            v-for="type in shiftTypes"
            :key="type.shiftCd"
            class="a06-type-card"
            :class="{ selected: selectedType?.shiftCd === type.shiftCd }"
            @click="selectShiftType(type.shiftCd)"
          >
            <div class="type-code">{{ type.shiftNo }}</div>
            <div class="type-name">{{ type.shiftNm }}</div>
            <div class="type-desc">{{ type.schNmList }}</div>
          </div>
        </div>
      </Transition>

      <!-- STEP 2: 조 편성 & 사용자 배정 -->
      <Transition name="crew-accordion">
        <div v-show="showCrewWrap" class="a06-crew-wrap">
          <div v-if="users.length === 0" class="a06-crew-empty">
            교대근무에 투입 가능한 인원이 없습니다.
          </div>
          <div v-else class="a06-crew-layout">
            <!-- 좌: 사용자 풀 -->
            <div class="a06-user-pool">
              <div class="pool-header">
                <div class="pool-title">
                  <span>사용자 목록</span>
                  <span class="pool-count">
                    미배정 {{ unassignedCount }}/{{ filteredUsers.length }}
                  </span>
                </div>
                <div class="pool-search">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                  </svg>
                  <input
                    v-model="poolSearchKeyword"
                    type="text"
                    placeholder="이름 검색"
                  />
                </div>
              </div>
              <div class="a06-user-list">
                <div
                  v-for="user in filteredUsers"
                  :key="user.userCd"
                  class="a06-user-chip"
                  :class="{
                    assigned: isAssigned(user.userCd),
                    dragging: dragUserId === user.userCd,
                  }"
                  :draggable="!isAssigned(user.userCd)"
                  @dragstart="handleDragStart($event, user.userCd)"
                  @dragend="handleDragEnd"
                >
                  <div class="chip-avatar">{{ user.userId[0] }}</div>
                  <div class="chip-info">
                    <div class="chip-name">{{ user.userNm }}</div>
                    <div class="chip-dept">{{ user.nodeNm }}</div>
                  </div>
                </div>
                <div v-if="filteredUsers.length === 0" class="pool-empty">
                  조건에 맞는 사용자가 없습니다.
                </div>
              </div>
            </div>

            <!-- 우: 조 영역 -->
            <div class="a06-teams-area">
              <!-- 교대근무 팀명 -->
              <div class="a06-team-nm-bar">
                <label class="team-nm-label">교대근무 팀명</label>
                <input
                  v-model="shiftTeamNm"
                  type="text"
                  class="team-nm-input"
                  placeholder="팀명을 입력하세요"
                />
              </div>
              <div class="teams-area-header">
                <span class="teams-summary">
                  배정 <strong>{{ assignedCount }}</strong> /
                  <strong>{{ users.length }}</strong
                  >명 &nbsp;·&nbsp;
                  <span class="teams-hint">★ 클릭 시 조장 지정</span>
                </span>
                <button class="btn btn-sm btn-second" @click="clearTeams">
                  편성 초기화
                </button>
              </div>
              <div v-if="teams.length === 0" class="teams-no-type-hint">
                교대근무 카드를 선택하면 조 편성 영역이 표시됩니다
              </div>
              <div v-else class="a06-teams-grid">
                <div
                  v-for="team in teams"
                  :key="team.key"
                  class="a06-team-box"
                  :class="{ 'drag-over': dragOverTeam === team.key }"
                  @dragover.prevent="dragOverTeam = team.key"
                  @dragleave="dragOverTeam = null"
                  @drop.prevent="handleDrop(team.key)"
                >
                  <div class="team-box-header">
                    <div
                      class="team-badge-icon"
                      :style="teamBadgeStyle(team.key)"
                    >
                      {{ team.key }}
                    </div>
                    <input
                      v-model="team.name"
                      class="team-name-input"
                      :placeholder="`${team.key}조`"
                      @click.stop
                    />
                    <span class="team-count-badge"
                      >{{ team.members.length }}명</span
                    >
                  </div>
                  <div
                    class="team-drop-zone"
                    :class="{ empty: team.members.length === 0 }"
                  >
                    <div
                      v-if="team.members.length === 0"
                      class="team-empty-hint"
                    >
                      사용자를 드래그해<br />여기에 놓으세요
                    </div>
                    <div
                      v-for="member in team.members"
                      :key="member.userCd"
                      class="team-member-row"
                      :class="{ leader: team.leaderYn === member.userCd }"
                      :style="teamMemberStyle(team.key)"
                    >
                      <div
                        class="member-avatar"
                        :style="memberAvatarStyle(team.key)"
                      >
                        {{ member.userNm[0] }}
                      </div>
                      <div class="member-name">{{ member.userNm }}</div>
                      <div class="member-dept">{{ member.nodeNm }}</div>
                      <div class="member-actions">
                        <button
                          class="act-btn leader-btn"
                          :class="{ active: team.leaderYn === member.userCd }"
                          :title="
                            team.leaderYn === member.userCd
                              ? '조장 해제'
                              : '조장 지정'
                          "
                          @click.stop="toggleLeader(team.key, member.userCd)"
                        >
                          ★
                        </button>
                        <button
                          class="act-btn remove-btn"
                          title="제거"
                          @click.stop="removeMember(team.key, member.userCd)"
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 하단 바: 적용 기간 + 미리보기 버튼 -->
          <div v-if="users.length > 0" class="a06-bottom-bar">
            <div class="bottom-period">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                width="14"
                height="14"
                style="color: var(--color-primary); flex-shrink: 0"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              <span class="period-label">적용 기간</span>
              <input type="date" v-model="strDate" @change="validatePeriod" />
              <span class="period-sep">~</span>
              <input type="date" v-model="endDate" @change="validatePeriod" />
            </div>
            <div class="bottom-summary">
              <strong
                >{{ assignedCount }}명이 {{ filledTeamCount }}/{{
                  teams.length
                }}개 조</strong
              >에 배정됨
            </div>
            <button
              class="btn btn-sm btn-primary"
              :disabled="!allTeamsFilled"
              @click="
                showPreview = true;
                showCrewWrap = false;
              "
            >
              미리보기
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                width="12"
                height="12"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
        </div>
      </Transition>

      <!-- STEP 3: 미리보기 -->
      <div v-if="showPreview" class="a06-preview-area">
        <!-- 메타 정보 + 저장 액션 -->
        <div class="preview-meta-bar">
          <div class="pm-item">
            <div class="pm-label">교대타입</div>
            <div class="pm-value">
              <strong>{{ selectedType?.shiftNm }}</strong>
            </div>
          </div>
          <div class="pm-sep"></div>
          <div class="pm-item">
            <div class="pm-label">적용 기간</div>
            <div class="pm-value">{{ strDate }} ~ {{ endDate }}</div>
          </div>
          <div class="pm-sep"></div>
          <div class="pm-item">
            <div class="pm-label">배정 인원</div>
            <div class="pm-value">
              <strong>{{ assignedCount }}명</strong> / {{ teams.length }}개 조
            </div>
          </div>
          <div class="save-notice">
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              style="flex-shrink: 0; color: #d97706"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>
              저장 확인 시 <strong>적용 시작일부터</strong> 배정된 사용자의 기존
              스케줄이 <strong>설정한 교대근무 스케줄로 변경</strong>됩니다.
            </span>
          </div>
          <div class="preview-action-btns">
            <button
              class="btn btn-sm btn-second"
              @click="
                showPreview = false;
                showCrewWrap = true;
              "
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                width="12"
                height="12"
              >
                <polyline points="15 18 9 12 15 6" />
              </svg>
              조 편성으로
            </button>
            <button class="btn btn-sm btn-primary" @click="fnSave">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                width="12"
                height="12"
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
              저장
            </button>
          </div>
        </div>

        <!-- 조 구성 요약 -->
        <div
          class="preview-teams-grid"
          :style="{ gridTemplateColumns: `repeat(${teams.length}, 1fr)` }"
        >
          <div
            v-for="team in teams"
            :key="team.key"
            class="preview-team-col"
            :style="teamColStyle(team.key)"
          >
            <div class="preview-team-head">
              <div class="preview-badge" :style="teamBadgeStyle(team.key)">
                {{ team.key }}
              </div>
              <span class="preview-team-name">{{ team.name }}</span>
              <span class="preview-team-cnt">{{ team.members.length }}명</span>
            </div>
            <div v-if="getLeader(team)" class="preview-leader-row">
              ★ 조장: {{ getLeader(team).userNm }}
            </div>
            <div class="preview-members-row">
              {{ team.members.map((m) => m.userNm).join(", ") || "–" }}
            </div>
          </div>
        </div>

        <!-- 달력 툴바 -->
        <div class="preview-cal-toolbar">
          <div class="month-nav">
            <button class="month-nav-btn" @click="navMonth(-1)">
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
            <span class="month-title">{{ previewMonthLabel }}</span>
            <button class="month-nav-btn" @click="navMonth(1)">
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
          <div class="cal-view-toggle">
            <button
              :class="['view-btn', { active: calViewMode === 'month' }]"
              @click="calViewMode = 'month'"
            >
              월별
            </button>
            <button
              :class="['view-btn', { active: calViewMode === 'timeline' }]"
              @click="calViewMode = 'timeline'"
            >
              조별
            </button>
          </div>
          <div class="shift-legend">
            <span
              v-for="sch in uniqueScheds"
              :key="sch.schCd"
              class="legend-item"
            >
              <span class="legend-chip" :style="getShiftStyle(sch)"></span>
              {{ sch.fstSchTime
              }}{{ sch.schType === "02" ? " / " + sch.secSchTime : "" }}
            </span>
            <span class="legend-item">
              <span class="legend-chip shift-off"></span>휴무
            </span>
          </div>
        </div>

        <!-- 달력 그리드 (월별 뷰) -->
        <div v-if="calViewMode === 'month'" class="preview-cal-wrap">
          <table class="preview-cal-table">
            <thead>
              <tr>
                <th class="cal-sun">일</th>
                <th>월</th>
                <th>화</th>
                <th>수</th>
                <th>목</th>
                <th>금</th>
                <th class="cal-sat">토</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(week, wi) in calendarRows" :key="wi">
                <td
                  v-for="(cell, di) in week"
                  :key="di"
                  :class="{ 'cal-out': !cell.isCurrentMonth }"
                >
                  <div class="cal-cell-inner">
                    <div
                      class="cal-date"
                      :class="{
                        'cal-sun': di === 0,
                        'cal-sat': di === 6,
                        'cal-out': !cell.isCurrentMonth,
                      }"
                    >
                      {{ cell.day }}
                    </div>
                    <div class="cal-team-dots">
                      <div
                        v-for="s in cell.shifts"
                        :key="s.teamKey"
                        class="team-dot"
                        :style="getShiftStyle(s)"
                        :title="`${s.teamKey}조: ${s.assignYn === 'N' ? '휴무' : s.fstSchTime + (s.schType === '02' ? ' / ' + s.secSchTime : '')}`"
                      >
                        {{ s.teamKey }}
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 조별 타임라인 뷰 -->
        <div v-else class="preview-timeline-wrap">
          <table class="preview-timeline-table">
            <thead>
              <tr>
                <th class="tl-team-head">조</th>
                <th
                  v-for="d in timelineData.days"
                  :key="d.d"
                  class="tl-day-head"
                  :class="{
                    'tl-sun': d.dow === 0,
                    'tl-sat': d.dow === 6,
                    'tl-out-range': !d.inRange,
                  }"
                >
                  <div class="tl-day-num">{{ d.d }}</div>
                  <div class="tl-dow">
                    {{ ["일", "월", "화", "수", "목", "금", "토"][d.dow] }}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in timelineData.teamRows" :key="row.team.key">
                <td class="tl-team-label">
                  <div
                    class="tl-team-badge"
                    :style="teamBadgeStyle(row.team.key)"
                  >
                    {{ row.team.key }}
                  </div>
                  <span class="tl-team-name">{{
                    row.team.name || row.team.key + "조"
                  }}</span>
                </td>
                <td
                  v-for="(shift, di) in row.shifts"
                  :key="di"
                  class="tl-shift-cell"
                  :class="{ 'tl-out-range': !timelineData.days[di]?.inRange }"
                >
                  <div
                    class="tl-shift-block"
                    :style="getShiftStyle(shift)"
                    :title="
                      shift.assignYn === 'N'
                        ? '휴무'
                        : shift.fstSchTime +
                          (shift.schType === '02'
                            ? ' / ' + shift.secSchTime
                            : '')
                    "
                  >
                    <template v-if="!timelineData.days[di]?.inRange">
                      –
                    </template>
                    <template v-else-if="shift.assignYn === 'N'">OFF</template>
                    <template v-else>
                      {{ shift.fstSchTime.slice(0, 2) }}
                    </template>
                  </div>
                </td>
              </tr>
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
  computed,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineEmits,
  defineOptions,
} from "vue";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { useModal } from "@/utils/useModal";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";

defineOptions({ name: "Attd_06_1" });

const props = defineProps({
  title: String,
  buttons: Object,
});

const emit = defineEmits(["save-complete"]);

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

const localButtons = ref({ ...props.buttons });

// ── 버튼 컨트롤 ──────────────────────────────────────────
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

const shiftTypes = ref([]);
const shiftSchDetails = ref([]);

const shiftSchMap = computed(() => {
  const map = {};
  shiftSchDetails.value.forEach((d) => {
    if (!map[d.teamIdx]) map[d.teamIdx] = {};
    map[d.teamIdx][d.dayNo] = d;
  });
  return map;
});

const uniqueScheds = computed(() => {
  const seen = new Set();
  return shiftSchDetails.value.filter((d) => {
    if (d.assignYn === "N" || seen.has(d.schCd)) return false;
    seen.add(d.schCd);
    return true;
  });
});

// ── 사용자 목록 ──────────────────────────────────────────
const users = ref([]);
const poolSearchKeyword = ref("");

const filteredUsers = computed(() =>
  users.value.filter((u) => {
    if (!poolSearchKeyword.value) return true;
    const kw = poolSearchKeyword.value.toLowerCase();
    return (
      u.userNm.toLowerCase().includes(kw) || u.nodeNm.toLowerCase().includes(kw)
    );
  })
);

const assignedIds = computed(() => {
  const ids = new Set();
  teams.value.forEach((t) => t.members.forEach((m) => ids.add(m.userCd)));
  return ids;
});

const isAssigned = (userCd) => assignedIds.value.has(userCd);
const unassignedCount = computed(
  () => filteredUsers.value.filter((u) => !isAssigned(u.userCd)).length
);

// ── 교대타입 선택 ─────────────────────────────────────────
const selectedType = ref(null);
const showPreview = ref(false);
const showCrewWrap = ref(false);
const shiftTeamNm = ref("");

const selectShiftType = async (shiftCd) => {
  const type = shiftTypes.value.find((t) => t.shiftCd === shiftCd);
  if (!type) return;

  if (selectedType.value && selectedType.value.shiftCd !== shiftCd) {
    const hasMembers = teams.value.some((t) => t.members.length > 0);
    if (hasMembers) {
      const ok = await proxy.$confirm(
        "교대타입을 변경하면 지금까지의 조 편성이 초기화됩니다. 계속하시겠습니까?"
      );
      if (!ok) return;
    }
  }

  showPreview.value = false;
  selectedType.value = { ...type };
  teams.value = [];

  await fnShiftSchDetail(shiftCd);
};

// ── 조 편성 ──────────────────────────────────────────────
const teams = ref([]);
const dragUserId = ref(null);
const dragOverTeam = ref(null);

const assignedCount = computed(() =>
  teams.value.reduce((s, t) => s + t.members.length, 0)
);
const filledTeamCount = computed(
  () => teams.value.filter((t) => t.members.length > 0).length
);
const allTeamsFilled = computed(
  () => teams.value.length > 0 && teams.value.every((t) => t.members.length > 0)
);

const handleDragStart = (e, userId) => {
  if (isAssigned(userId)) {
    e.preventDefault();
    return;
  }
  dragUserId.value = userId;
  e.dataTransfer.effectAllowed = "move";
};
const handleDragEnd = () => {
  dragUserId.value = null;
  dragOverTeam.value = null;
};
const handleDrop = (teamKey) => {
  dragOverTeam.value = null;
  if (!dragUserId.value) return;
  const user = users.value.find((u) => u.userCd === dragUserId.value);
  if (!user) return;

  teams.value.forEach((t) => {
    if (t.leaderYn === user.userCd) t.leaderYn = null;
    t.members = t.members.filter((m) => m.userCd !== user.userCd);
  });

  const team = teams.value.find((t) => t.key === teamKey);
  if (team && !team.members.some((m) => m.userCd === user.userCd)) {
    team.members.push(user);
  }
  dragUserId.value = null;
};

const removeMember = (teamKey, userCd) => {
  const team = teams.value.find((t) => t.key === teamKey);
  if (team) {
    team.members = team.members.filter((m) => m.userCd !== userCd);
    if (team.leaderYn === userCd) team.leaderYn = null;
  }
};

const toggleLeader = (teamKey, userCd) => {
  const team = teams.value.find((t) => t.key === teamKey);
  if (team) team.leaderYn = team.leaderYn === userCd ? null : userCd;
};

const clearTeams = async () => {
  if (teams.value.every((t) => t.members.length === 0)) return;
  const ok = await proxy.$confirm(
    "모든 조 편성을 초기화합니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  teams.value.forEach((t) => {
    t.members = [];
    t.leaderYn = null;
  });
};

// ── 적용 기간 ─────────────────────────────────────────────
const strDate = ref("");
const endDate = ref("");

const validatePeriod = async () => {
  if (strDate.value && endDate.value && strDate.value > endDate.value) {
    await proxy.$alert("종료일은 시작일보다 이후여야 합니다.");
    endDate.value = strDate.value;
  }
};

// ── 미리보기 달력 ─────────────────────────────────────────
const previewYear = ref(new Date().getFullYear());
const previewMonth = ref(new Date().getMonth());
const calViewMode = ref("month");

const previewMonthLabel = computed(
  () => `${previewYear.value}년 ${previewMonth.value + 1}월`
);

const navMonth = (delta) => {
  let m = previewMonth.value + delta;
  let y = previewYear.value;
  if (m < 0) {
    m = 11;
    y--;
  }
  if (m > 11) {
    m = 0;
    y++;
  }
  previewMonth.value = m;
  previewYear.value = y;
};

const getLeader = (team) =>
  team.members.find((m) => m.userCd === team.leaderYn) || null;

const SHIFT_PALETTE = [
  { background: "#fef3c7", color: "#854d0e" },
  { background: "#dbeafe", color: "#1e3a8a" },
  { background: "#f3e8ff", color: "#6b21a8" },
  { background: "#dcfce7", color: "#166534" },
  { background: "#312e81", color: "#fff" },
  { background: "#ffe4e6", color: "#9f1239" },
  { background: "#e0f2fe", color: "#075985" },
  { background: "#fce7f3", color: "#9d174d" },
  { background: "#ecfdf5", color: "#065f46" },
  { background: "#fff7ed", color: "#9a3412" },
  { background: "#f0fdf4", color: "#14532d" },
  { background: "#eef2ff", color: "#3730a3" },
];
const SHIFT_OFF_STYLE = { background: "#f3f4f6", color: "#9ca3af" };

const schColorMap = computed(() => {
  const map = {};
  uniqueScheds.value.forEach((sch, i) => {
    map[sch.schCd] = SHIFT_PALETTE[i % SHIFT_PALETTE.length];
  });
  return map;
});

const getShiftStyle = (s) => {
  if (!s || s.assignYn === "N" || !s.schCd) return SHIFT_OFF_STYLE;
  return schColorMap.value[s.schCd] ?? SHIFT_OFF_STYLE;
};

const calendarRows = computed(() => {
  if (!selectedType.value || !strDate.value) return [];
  const type = selectedType.value;
  const rangeStart = new Date(strDate.value);
  const rangeEnd = endDate.value
    ? new Date(endDate.value)
    : new Date("2099-12-31");
  const year = previewYear.value;
  const month = previewMonth.value;
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startWeekday = firstDay.getDay();
  const totalCells = Math.ceil((startWeekday + lastDay.getDate()) / 7) * 7;

  const rows = [];
  for (let r = 0; r < totalCells / 7; r++) {
    const week = [];
    for (let c = 0; c < 7; c++) {
      const dayOffset = r * 7 + c - startWeekday + 1;
      const cellDate = new Date(year, month, dayOffset);
      const isCurrentMonth = cellDate.getMonth() === month;
      const inRange = cellDate >= rangeStart && cellDate <= rangeEnd;

      let shifts = [];
      if (isCurrentMonth && inRange) {
        const diffDays = Math.floor(
          (cellDate - rangeStart) / (1000 * 60 * 60 * 24)
        );
        const cycle = parseInt(type.shiftCycleDays) || 1;
        const cycleDay = ((diffDays % cycle) + cycle) % cycle;
        const dayNo = cycleDay + 1;
        shifts = teams.value.map((t) => {
          const detail = shiftSchMap.value[t.teamIdx]?.[dayNo];
          return {
            teamKey: t.key,
            assignYn: detail?.assignYn ?? "N",
            schNo: detail?.schNo ?? "",
            schCd: detail?.schCd ?? "",
            fstSchTime: detail?.fstSchTime ?? "",
            secSchTime: detail?.secSchTime ?? "",
            schType: detail?.schType ?? "01",
          };
        });
      }
      week.push({ day: cellDate.getDate(), isCurrentMonth, shifts });
    }
    rows.push(week);
  }
  return rows;
});

const timelineData = computed(() => {
  if (!selectedType.value || !strDate.value) return { days: [], teamRows: [] };
  const type = selectedType.value;
  const rangeStart = new Date(strDate.value);
  const rangeEnd = endDate.value
    ? new Date(endDate.value)
    : new Date("2099-12-31");
  const year = previewYear.value;
  const month = previewMonth.value;
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const days = [];
  for (let d = 1; d <= daysInMonth; d++) {
    const cellDate = new Date(year, month, d);
    days.push({
      d,
      dow: cellDate.getDay(),
      inRange: cellDate >= rangeStart && cellDate <= rangeEnd,
      date: cellDate,
    });
  }

  const cycle = parseInt(type.shiftCycleDays) || 1;
  const teamRows = teams.value.map((t) => {
    const shifts = days.map(({ inRange, date }) => {
      if (!inRange)
        return {
          assignYn: "N",
          schCd: "",
          fstSchTime: "",
          secSchTime: "",
          schType: "01",
        };
      const diffDays = Math.floor((date - rangeStart) / (1000 * 60 * 60 * 24));
      const dayNo = (((diffDays % cycle) + cycle) % cycle) + 1;
      const detail = shiftSchMap.value[t.teamIdx]?.[dayNo];
      return {
        assignYn: detail?.assignYn ?? "N",
        schCd: detail?.schCd ?? "",
        fstSchTime: detail?.fstSchTime ?? "",
        secSchTime: detail?.secSchTime ?? "",
        schType: detail?.schType ?? "01",
      };
    });
    return { team: t, shifts };
  });

  return { days, teamRows };
});

// ── 조회 ─────────────────────────────────────────────────
const fnSearch = async () => {
  if (proxy.$util.isEmpty(siteCd.value)) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "사업장" })
    );
    siteNoFcs.value?.focus();
    return;
  }

  console.log(sessionStorage.getItem("gv_authCd"));
  if (
    !proxy.$util.isEmpty(sessionStorage.getItem("gv_authCd")) &&
    !(
      sessionStorage.getItem("gv_authCd") == "master" ||
      sessionStorage.getItem("gv_authCd") == "hr"
    )
  ) {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      await proxy.$alert(
        getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "소속부서" })
      );
      siteNoFcs.value?.focus();
      return;
    }
  }

  shiftTypes.value = [];
  shiftSchDetails.value = [];
  users.value = [];
  selectedType.value = null;
  teams.value = [];
  showPreview.value = false;
  showCrewWrap.value = false;

  try {
    const typeRes = await axios.get("/webApi/attd06/shift-type-lists", {
      params: { siteCd: siteCd.value },
    });
    shiftTypes.value = typeRes.data.shiftTypeListsResultList;

    console.log(shiftTypes.value);

    const userRes = await axios.get("/webApi/attd06/user-lists", {
      params: {
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
      },
    });

    users.value = userRes.data.userListsResultList ?? [];
    showCrewWrap.value = true;
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnShiftSchDetail = async (shiftCd) => {
  try {
    const res = await axios.get("/webApi/attd06/shift-type-detail-lists", {
      params: { siteCd: siteCd.value, shiftCd },
    });
    const list = res.data.shiftTypeDetailListsResultList ?? [];
    shiftSchDetails.value = list;

    const teamMap = new Map();
    list.forEach((d) => {
      if (!teamMap.has(d.teamIdx)) teamMap.set(d.teamIdx, d.teamNm);
    });
    const prevMembers = Object.fromEntries(
      teams.value.map((t) => [
        t.teamIdx,
        { members: t.members, leaderYn: t.leaderYn },
      ])
    );
    teams.value = [...teamMap.entries()]
      .sort((a, b) => a[0] - b[0])
      .map(([teamIdx, teamNm]) => ({
        teamIdx,
        key: teamNm,
        name: prevMembers[teamIdx]?.name ?? `${teamNm}조`,
        leaderYn: prevMembers[teamIdx]?.leaderYn ?? null,
        members: prevMembers[teamIdx]?.members ?? [],
      }));
  } catch {
    shiftSchDetails.value = [];
    teams.value = [];
  }
};

// ── 저장 ─────────────────────────────────────────────────
const fnSave = async () => {
  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  const saveData = {
    shiftMeta: {
      siteCd: siteCd.value,
      shiftCd: selectedType.value.shiftCd,
      shiftTeamNm: shiftTeamNm.value,
      startDate: strDate.value.replaceAll("-", ""),
      endDate: endDate.value.replaceAll("-", ""),
    },
    teamList: teams.value.map((t) => ({
      teamIdx: t.teamIdx,
      teamNm: t.name,
      members: t.members.map((m) => ({
        userCd: m.userCd,
        leaderYn: t.leaderYn === m.userCd ? "Y" : "N",
      })),
    })),
  };

  try {
    // TODO: API 연동
    await axios.post("/webApi/attd06/insert-shift-sch-infos", saveData);

    await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
    selectedType.value = null;
    teams.value = [];
    shiftTeamNm.value = "";
    showPreview.value = false;
    emit("save-complete");
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  }
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
const teamColStyle = (key) => ({
  background: tc(key).bg,
  borderColor: tc(key).br,
});
const teamMemberStyle = (key) => ({ background: tc(key).bg });
const memberAvatarStyle = (key) => ({
  background: tc(key).br,
  color: tc(key).tx,
});

// ── 초기화 ───────────────────────────────────────────────
const fmtDate = (d) => {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
};

const fnInit = () => {
  siteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
  siteNo.value = sessionStorage.getItem("gv_siteNo") ?? "";
  siteNm.value = sessionStorage.getItem("gv_siteNm") ?? "";
  if (siteCd.value) {
    if (
      !proxy.$util.isEmpty(sessionStorage.getItem("gv_authCd")) &&
      !(
        sessionStorage.getItem("gv_authCd") == "master" ||
        sessionStorage.getItem("gv_authCd") == "hr"
      )
    ) {
      nodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
      nodeNm.value = sessionStorage.getItem("gv_nodeNm") ?? "";
    }
    nodeDisabled.value = false;
  }

  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const end = new Date(start);
  end.setMonth(end.getMonth() + 6);
  end.setDate(end.getDate() - 1);
  strDate.value = fmtDate(start);
  endDate.value = fmtDate(end);
};

onMounted(() => {
  fnInit();
  fnButtonControll();
});
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

/* ── 공통 바디 ──────────────────────────────────────────── */
.a06-body {
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 0;
  gap: 0;
}

/* ── 아코디언 트랜지션 ───────────────────────────────────── */
.crew-accordion-enter-active,
.crew-accordion-leave-active {
  overflow: hidden;
  transition:
    max-height 0.35s ease,
    opacity 0.25s ease;
}
.crew-accordion-enter-from,
.crew-accordion-leave-to {
  max-height: 0;
  opacity: 0;
}
.crew-accordion-enter-to,
.crew-accordion-leave-from {
  max-height: 2000px;
  opacity: 1;
}

/* ── 교대타입 카드 목록 ──────────────────────────────────── */
.a06-type-list {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  overflow-x: auto;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-shrink: 0;
}
.a06-type-list::-webkit-scrollbar {
  height: 5px;
}
.a06-type-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.a06-type-card {
  flex-shrink: 0;
  width: 200px;
  background: #fff;
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition:
    border-color 0.15s,
    transform 0.15s,
    box-shadow 0.15s;
  position: relative;
}
.a06-type-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(16, 24, 40, 0.07);
}
.a06-type-card.selected {
  border-color: var(--color-primary);
  background: rgba(22, 163, 74, 0.04);
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.1);
}
.a06-type-card.selected::after {
  content: "✓";
  position: absolute;
  top: 8px;
  right: 8px;
  width: 18px;
  height: 18px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}
.type-code {
  display: inline-block;
  padding: 2px 7px;
  background: rgba(22, 163, 74, 0.12);
  color: var(--color-primary);
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 6px;
}
.type-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong);
  margin-bottom: 3px;
}
.type-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.4;
  margin-bottom: 8px;
}
.type-meta {
  display: flex;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px dashed var(--color-border);
}
.type-meta-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.type-meta-label {
  font-size: 10px;
  color: #9ca3af;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.type-meta-value {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-strong);
}

/* ── 교대근무 팀명 ────────────────────────────────────────── */
.a06-team-nm-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 2px 8px;
  flex-shrink: 0;
}
.team-nm-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  white-space: nowrap;
}
.team-nm-input {
  width: 260px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 7px;
  font-size: 0.8125rem;
  background: #fff;
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
  transition: border-color 0.15s;
}
.team-nm-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* ── 교대타입 미선택 힌트 ────────────────────────────────── */
.teams-no-type-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  font-size: 0.8125rem;
  color: #9ca3af;
  border: 1.5px dashed var(--color-border);
  border-radius: 10px;
  text-align: center;
  padding: 20px;
}

/* ── 조 편성 ─────────────────────────────────────────────── */
.a06-crew-wrap {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.a06-crew-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  padding: 40px 20px;
  margin: 14px 16px;
  font-size: 0.875rem;
  color: #9ca3af;
  border: 1.5px dashed var(--color-border);
  border-radius: 10px;
  text-align: center;
}
.a06-crew-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 14px;
  padding: 14px 16px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.a06-user-pool {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  align-self: flex-start;
  max-height: calc(100vh - 360px);
  position: sticky;
  top: 0;
}
.pool-header {
  padding: 10px 11px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}
.pool-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-strong);
  margin-bottom: 7px;
}
.pool-count {
  font-size: 11px;
  padding: 1px 7px;
  background: rgba(22, 163, 74, 0.12);
  color: var(--color-primary);
  border-radius: 9999px;
  font-weight: 600;
}
.pool-search {
  position: relative;
}
.pool-search input {
  width: 100%;
  height: 26px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0 8px 0 24px;
  font-size: 11px;
  background: #fff;
  font-family: "Pretendard", sans-serif;
}
.pool-search svg {
  position: absolute;
  left: 7px;
  top: 50%;
  transform: translateY(-50%);
  width: 11px;
  height: 11px;
  color: #9ca3af;
  stroke-width: 2;
}
.a06-user-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}
.a06-user-list::-webkit-scrollbar {
  width: 5px;
}
.a06-user-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.a06-user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 7px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  margin-bottom: 4px;
  cursor: grab;
  font-size: 11px;
  transition:
    border-color 0.1s,
    background 0.1s;
}
.a06-user-chip:hover:not(.assigned) {
  border-color: var(--color-primary);
  background: rgba(22, 163, 74, 0.04);
}
.a06-user-chip.assigned {
  opacity: 0.4;
  cursor: not-allowed;
  text-decoration: line-through;
}
.a06-user-chip.dragging {
  opacity: 0.3;
}

.chip-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.12);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}
.chip-name {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-strong);
  line-height: 1.2;
}
.chip-dept {
  font-size: 10px;
  color: var(--color-text-muted);
  line-height: 1.2;
}
.pool-empty {
  padding: 20px 8px;
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
}

.a06-teams-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
}
.teams-area-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 0;
}
.teams-summary {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}
.teams-summary strong {
  color: var(--color-text-strong);
}
.teams-hint {
  font-size: 11px;
  color: #9ca3af;
}

.a06-teams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}
.a06-team-box {
  border: 1.5px dashed var(--color-border);
  border-radius: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition:
    border-color 0.15s,
    background 0.15s;
}
.a06-team-box.drag-over {
  border-style: solid;
  border-color: var(--color-primary);
  background: rgba(22, 163, 74, 0.04);
}
.team-box-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 9px;
  border-bottom: 1px solid var(--color-border);
}
.team-badge-icon {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  flex-shrink: 0;
}
.team-name-input {
  flex: 1;
  min-width: 0;
  height: 22px;
  padding: 0 5px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-strong);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  font-family: "Pretendard", sans-serif;
  transition: border-color 0.1s;
}
.team-name-input:hover {
  border-color: var(--color-border);
}
.team-name-input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: #fff;
}
.team-count-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 9999px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.team-drop-zone {
  flex: 1;
  padding: 5px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-height: 72px;
}
.team-drop-zone.empty {
  align-items: center;
  justify-content: center;
  padding: 14px 8px;
}
.team-empty-hint {
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
  line-height: 1.4;
}

.team-member-row {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 3px 6px;
  border-radius: 5px;
  font-size: 11px;
  position: relative;
}
.team-member-row.leader .member-name::after {
  content: " ★";
  color: #f59e0b;
  font-size: 11px;
}
.member-avatar {
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
.member-name {
  flex: 1;
  min-width: 0;
  font-weight: 500;
  color: var(--color-text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.member-dept {
  font-size: 10px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.member-actions {
  display: flex;
  gap: 1px;
}
.act-btn {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 11px;
  cursor: pointer;
  background: none;
  border: none;
  opacity: 0;
  transition:
    opacity 0.1s,
    background 0.1s;
}
.team-member-row:hover .act-btn {
  opacity: 1;
}
.leader-btn {
  color: #9ca3af;
}
.leader-btn.active,
.leader-btn:hover {
  color: #f59e0b;
  background: #fef3c7;
}
.remove-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* ── 하단 바 ─────────────────────────────────────────────── */
.a06-bottom-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--color-bg);
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}
.bottom-period {
  display: flex;
  align-items: center;
  gap: 7px;
}
.period-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  white-space: nowrap;
}
.bottom-period input[type="date"] {
  height: 28px;
  padding: 0 8px;
  border: 1px solid var(--color-border);
  border-radius: 7px;
  font-size: 12px;
  background: #fff;
  color: var(--color-text-strong);
  font-family: "Pretendard", sans-serif;
}
.period-sep {
  color: #9ca3af;
  font-size: 13px;
}
.bottom-summary {
  flex: 1;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  padding-left: 12px;
  border-left: 1px solid var(--color-border);
}
.bottom-summary strong {
  color: var(--color-text-strong);
}

/* ── 미리보기 ─────────────────────────────────────────────── */
.a06-preview-area {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.preview-meta-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(22, 163, 74, 0.04);
  border-bottom: 1px solid rgba(22, 163, 74, 0.15);
  flex-shrink: 0;
  flex-wrap: nowrap;
  min-height: 0;
}
.pm-sep {
  width: 1px;
  height: 28px;
  background: rgba(22, 163, 74, 0.2);
  flex-shrink: 0;
}
.pm-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex-shrink: 0;
}
.pm-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.pm-value {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-strong);
  white-space: nowrap;
}
.save-notice {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  background: #fff;
  border: 1px solid #fde047;
  border-radius: 7px;
  font-size: 11px;
  color: var(--color-text);
  line-height: 1.5;
  flex: none;
  white-space: nowrap;
}
.preview-action-btns {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

.preview-teams-grid {
  display: grid;
  gap: 10px;
  padding: 12px 16px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.preview-team-col {
  border: 1px solid;
  border-radius: 10px;
  padding: 9px 11px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.preview-team-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 5px;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.1);
}
.preview-badge {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  flex-shrink: 0;
}
.preview-team-name {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-strong);
}
.preview-team-cnt {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  padding: 1px 6px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 9999px;
}
.preview-leader-row {
  font-size: 13px;
  color: #d97706;
  font-weight: 600;
}
.preview-members-row {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
  word-break: keep-all;
}

.preview-cal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  flex-shrink: 0;
}
.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.month-nav-btn {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-text);
  cursor: pointer;
}
.month-nav-btn:hover {
  background: var(--color-bg);
}
.month-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-strong);
  min-width: 110px;
  text-align: center;
}

.shift-legend {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--color-text-muted);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
.legend-chip {
  width: 13px;
  height: 13px;
  border-radius: 3px;
}

.preview-cal-wrap {
  padding: 0 16px 16px;
  overflow: auto;
}
.preview-cal-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
}
.preview-cal-table th {
  background: var(--color-bg);
  padding: 8px 5px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-muted);
  border-bottom: 1px solid var(--color-border);
  border-right: 1px solid var(--color-border);
  text-align: center;
}
.preview-cal-table th:last-child {
  border-right: none;
}
.preview-cal-table td {
  padding: 0;
  border-bottom: 1px solid var(--color-border);
  border-right: 1px solid var(--color-border);
  vertical-align: top;
  min-width: 72px;
  height: auto;
  min-height: 52px;
}
.preview-cal-table td:last-child {
  border-right: none;
}
.preview-cal-table tr:last-child td {
  border-bottom: none;
}
.cal-out {
  background: #fafafa;
}
.cal-cell-inner {
  padding: 4px 6px;
  height: 100%;
}
.cal-date {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
  margin-bottom: 3px;
}
.cal-date.cal-sun {
  color: #ef4444;
}
.cal-date.cal-sat {
  color: #2563eb;
}
.cal-date.cal-out {
  opacity: 0.35;
}
/* ── 월별 뷰: 팀 도트 ─────────────────────────────────── */
.cal-team-dots {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 2px;
  margin-top: 3px;
}
.team-dot {
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  cursor: default;
  height: 20px;
  min-width: 0;
}

.shift-off {
  background: #f3f4f6;
  color: var(--color-text-muted);
}

.legend-chip.shift-off {
  background: #f3f4f6;
  border: 1px solid var(--color-border);
}

/* ── 뷰 토글 ──────────────────────────────────────────── */
.cal-view-toggle {
  display: flex;
  border: 1px solid var(--color-border);
  border-radius: 7px;
  overflow: hidden;
  flex-shrink: 0;
}
.view-btn {
  padding: 4px 12px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  background: #fff;
  border: none;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
  font-family: "Pretendard", sans-serif;
}
.view-btn:first-child {
  border-right: 1px solid var(--color-border);
}
.view-btn.active {
  background: var(--color-primary);
  color: #fff;
}
.view-btn:hover:not(.active) {
  background: var(--color-bg);
}

/* ── 조별 타임라인 뷰 ─────────────────────────────────── */
.preview-timeline-wrap {
  overflow: auto;
  padding: 0 16px 16px;
}
.preview-timeline-table {
  border-collapse: collapse;
  font-size: 11px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  min-width: 100%;
  table-layout: fixed;
}
.tl-team-head {
  width: 90px;
  min-width: 90px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  border-right: 2px solid var(--color-border);
  padding: 6px 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-muted);
  text-align: left;
  position: sticky;
  left: 0;
  z-index: 2;
}
.tl-day-head {
  width: 34px;
  min-width: 34px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  border-right: 1px solid var(--color-border);
  padding: 4px 2px;
  text-align: center;
  vertical-align: middle;
}
.tl-day-head:last-child {
  border-right: none;
}
.tl-day-num {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-strong);
  line-height: 1.2;
}
.tl-dow {
  font-size: 9px;
  color: var(--color-text-muted);
  line-height: 1.2;
}
.tl-sun .tl-day-num,
.tl-sun .tl-dow {
  color: #ef4444;
}
.tl-sat .tl-day-num,
.tl-sat .tl-dow {
  color: #2563eb;
}
.tl-out-range {
  opacity: 0.3;
}
.tl-team-label {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border-right: 2px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  background: #fff;
  position: sticky;
  left: 0;
  z-index: 1;
  white-space: nowrap;
}
.tl-team-badge {
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
.tl-team-name {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-strong);
  overflow: hidden;
  text-overflow: ellipsis;
}
.tl-shift-cell {
  padding: 3px 2px;
  border-right: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  text-align: center;
}
.tl-shift-cell:last-child {
  border-right: none;
}
.preview-timeline-table tr:last-child td {
  border-bottom: none;
}
.tl-shift-block {
  width: 100%;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 800;
  cursor: default;
}
</style>
