<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ session.title || "TBM 교육 콘솔" }}</span>
          <div class="header-actions">
            <span class="status-badge" :class="statusClass(session.statusCd)">
              {{ session.statusNm || statusNm(session.statusCd) }}
            </span>
            <button class="icon-button" @click="$emit('close')">
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
        </div>

        <div class="detail-wrapper">
          <!-- 메타 -->
          <div class="detail-meta">
            <span class="meta-item"
              >사업장: {{ session.siteNm || session.siteCd }}</span
            >
            <span class="meta-item">개설자: {{ session.managerUserNm }}</span>
            <span class="meta-item">등록일: {{ session.insertDate }}</span>
            <span v-if="session.openedAt" class="meta-item"
              >교육준비: {{ session.openedAt }}</span
            >
          </div>

          <!-- ===== 상태머신 액션 바 ===== -->
          <div class="console-section">
            <div class="console-title">교육 진행</div>

            <!-- DRAFT(개설): 교육준비 시작 -->
            <div v-if="session.statusCd === 'DRAFT'" class="console-actions">
              <p class="console-hint">
                교육준비를 시작하면 입실 비밀번호가 발급되고, 현재 위치가 GPS
                중심좌표로 수집됩니다.
              </p>
              <div class="gps-coord">
                <span v-if="gpsStatus === 'ok'" class="gps-ok">
                  위도 {{ form.managerGpsLat }} / 경도 {{ form.managerGpsLon }}
                </span>
                <span v-else-if="gpsStatus === 'loading'" class="gps-muted"
                  >위치 수집 중...</span
                >
                <span v-else class="gps-muted">위치 미수집</span>
                <button
                  type="button"
                  class="btn btn-second btn-sm"
                  @click="fnCaptureGps"
                >
                  현재 위치 가져오기
                </button>
              </div>
            </div>

            <!-- OPENED(교육준비) -->
            <div
              v-else-if="session.statusCd === 'OPENED'"
              class="console-actions"
            >
              <div class="pwd-section">
                <div class="pwd-box">
                  <span class="pwd-label">입실 비밀번호</span>
                  <span class="pwd-value">{{ session.entryPwd || "-" }}</span>
                </div>
                <button
                  type="button"
                  class="btn btn-second btn-sm"
                  @click="fnRegenEntry"
                >
                  입실비번 재발급
                </button>
              </div>

              <div
                class="timer-box"
                :class="{ 'timer-warn': remainSec !== null && remainSec <= 60 }"
              >
                <span class="timer-label">자동 교육시작까지</span>
                <span class="timer-value">{{ remainText }}</span>
              </div>

              <div class="btn-group console-btn-row">
                <button class="btn btn-second" @click="fnOpenManagerEntry">
                  대리 / 일용직 입실
                </button>
                <button class="btn btn-second" @click="fnOpenGpsPanel">
                  입실자
                </button>
                <button
                  class="btn btn-second"
                  :disabled="isBusy"
                  @click="fnExtend"
                >
                  교육 연장(+15분)
                </button>
                <button
                  class="btn btn-primary"
                  :disabled="isBusy"
                  @click="fnStart"
                >
                  교육 시작
                </button>
              </div>
            </div>

            <!-- IN_PROGRESS(교육시작): 종료 버튼은 footer 로 이동(T6-12) -->
            <div
              v-else-if="session.statusCd === 'IN_PROGRESS'"
              class="console-actions"
            >
              <p class="console-hint">교육이 시작되어 입실이 마감되었습니다.</p>
            </div>

            <!-- COMPLETED(교육종료) -->
            <div
              v-else-if="session.statusCd === 'COMPLETED'"
              class="console-actions"
            >
              <div class="pwd-section">
                <div class="pwd-box">
                  <span class="pwd-label">종료 비밀번호</span>
                  <span class="pwd-value">{{ session.exitPwd || "-" }}</span>
                </div>
                <button
                  type="button"
                  class="btn btn-second btn-sm"
                  @click="fnRegenExit"
                >
                  종료비번 재발급
                </button>
              </div>
              <button class="btn btn-second" @click="fnOpenAttendance">
                참가자 · 미이수 처리
              </button>
            </div>

            <!-- CANCELLED(취소) -->
            <div
              v-else-if="session.statusCd === 'CANCELLED'"
              class="console-actions"
            >
              <p class="console-hint">
                취소된 교육입니다. 사유: {{ session.cancelReason || "-" }}
              </p>
            </div>
          </div>

          <!-- 연동 회사(PRAFTA-SUBCON-T5) — DRAFT/OPENED 구간에서만 관리 가능 -->
          <div class="detail-section">
            <div class="detail-section-title share-title">
              <span>연동 회사</span>
              <button
                v-if="canManageShare"
                type="button"
                class="btn btn-second btn-sm"
                @click="fnOpenSharePop"
              >
                연동 회사 지정
              </button>
            </div>
            <ul v-if="shareRows.length" class="ref-list">
              <li v-for="row in shareRows" :key="row.shareId" class="ref-item">
                <span class="ref-name">{{ row.cmpnyNm }}</span>
                <span class="ref-meta">
                  {{ row.designatedDtime }}
                  <template v-if="row.subCount > 0">
                    · 하위 지정 {{ row.subCount }}개사
                  </template>
                </span>
                <button
                  v-if="canManageShare"
                  type="button"
                  class="btn btn-second btn-sm"
                  @click="fnReleaseShare(row)"
                >
                  해제
                </button>
              </li>
            </ul>
            <div v-else class="detail-empty">지정된 연동 회사가 없습니다.</div>

            <p v-if="!canManageShare" class="console-hint">
              교육이 시작되어 연동 회사를 변경할 수 없습니다.
            </p>
          </div>

          <!-- 교육자료 (6.2-(1)-2 / (3)-2) -->
          <div v-if="showEduRefs" class="detail-section">
            <div class="detail-section-title">교육자료</div>
            <ul v-if="contents.length" class="ref-list">
              <li v-for="c in contents" :key="c.mtrlCd" class="ref-item">
                <span class="ref-name">{{ c.title }}</span>
                <span
                  class="ref-scope"
                  :class="
                    c.isCommonContent === 'Y' ? 'scope-common' : 'scope-site'
                  "
                >
                  {{ c.isCommonContent === "Y" ? "공통" : "사업장" }}
                </span>
                <span v-if="c.mtrlTypeNm || c.mtrlType" class="ref-type">
                  {{ c.mtrlTypeNm || c.mtrlType }}
                </span>
              </li>
            </ul>
            <div v-else class="detail-empty">연계된 교육자료가 없습니다.</div>
          </div>

          <!-- 위험성평가 (6.2-(1)-2 / (3)-2). 항목 클릭 → 읽기전용 상세 -->
          <div v-if="showEduRefs" class="detail-section">
            <div class="detail-section-title">위험성평가</div>
            <ul v-if="risks.length" class="ref-list">
              <li
                v-for="(r, i) in risks"
                :key="(r.assessmentCd || '') + '-' + i"
                class="ref-item ref-item-risk"
              >
                <button
                  type="button"
                  class="title-link ref-name"
                  @click="fnOpenRiskDetail(r)"
                >
                  {{ r.displayName || "(이름 미정)" }}
                </button>
                <span class="ref-meta">
                  평가요청일 {{ r.initAssessDate || "-" }} · 평가요청자
                  {{ r.initAssessorNm || "-" }}
                </span>
              </li>
            </ul>
            <div v-else class="detail-empty">연계된 위험성평가가 없습니다.</div>
          </div>

          <!-- 교육 내용 -->
          <div class="detail-section">
            <div class="detail-section-title">교육 내용</div>
            <div
              v-if="session.contentBody"
              class="content-html"
              v-html="session.contentBody"
            ></div>
            <div v-else class="detail-empty">교육 내용이 없습니다.</div>
          </div>

          <!-- GPS 설정 -->
          <div class="detail-section">
            <div class="detail-section-title">GPS 검증</div>
            <div class="detail-meta">
              <span class="meta-item"
                >유형: {{ gpsTypeNm(session.gpsVerifyTypeCd) }}</span
              >
              <span
                v-if="session.gpsVerifyTypeCd !== 'DISABLED'"
                class="meta-item"
              >
                반경: {{ session.gpsVerifyRadiusM }}m
              </span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button v-if="canEdit" class="btn btn-second" @click="fnEdit">
              수정
            </button>
            <button
              v-if="canCancel && session.statusCd !== 'DRAFT'"
              class="btn btn-second"
              @click="fnCancelSession"
            >
              취소
            </button>
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
            <button
              v-if="session.statusCd === 'DRAFT'"
              class="btn btn-primary"
              :disabled="isBusy"
              @click="fnPrepare"
            >
              교육준비 시작
            </button>
            <!-- T6-12: 교육시작 상태의 교육 종료 버튼을 닫기 우측으로 이동 -->
            <button
              v-if="session.statusCd === 'IN_PROGRESS'"
              class="btn btn-primary"
              :disabled="isBusy"
              @click="fnComplete"
            >
              교육 종료
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  onMounted,
  onUnmounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";
import TbmManagerEntryPop from "./TbmManagerEntryPop.vue";
import TbmEntryGpsPanel from "./TbmEntryGpsPanel.vue";
import TbmAttendanceDetail from "./TbmAttendanceDetail.vue";
import TbmSessionForm from "./TbmSessionForm.vue";
import TbmShareCmpnyPop from "./TbmShareCmpnyPop.vue";
import RiskAssessInfo from "@/views/risk/popup/RiskAssessInfo.vue";

const { proxy } = getCurrentInstance();
const { open: openPop, close: closePop } = useModal();

const props = defineProps({
  sessionCd_p: String,
  onSearch: Function,
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);

const session = reactive({
  sessionCd: "",
  siteCd: "",
  siteNm: "",
  title: "",
  contentBody: "",
  statusCd: "",
  statusNm: "",
  entryPwd: "",
  exitPwd: "",
  managerUserNm: "",
  gpsVerifyTypeCd: "",
  gpsVerifyRadiusM: null,
  gpsManualConfirmYn: "",
  openedAt: "",
  prepStartAt: "",
  cancelReason: "",
  insertDate: "",
});

// 교육준비 전이 시 수집할 GPS 좌표(웹 위치권한)
const form = reactive({
  managerGpsLat: "",
  managerGpsLon: "",
});
const gpsStatus = ref("idle"); // idle | loading | ok | fail
const isBusy = ref(false);
const remainSec = ref(null); // 자동 교육시작까지 남은 초(표시 전용)
const rawDetail = ref(null); // 수정 폼(TbmSessionForm) 전달용 원본 응답
const contents = ref([]); // 연계 교육자료 목록(6.2-(1)-2 / (3)-2)
const risks = ref([]); // 연계 위험성평가 목록(6.2-(1)-2 / (3)-2)
let autoStartRefetched = false; // 자동 교육시작 카운트다운 0 도달 시 재조회 1회 가드(T6-09)

// 교육준비 자동 교육시작 타이머 길이(15분)
const PREP_TIMER_SEC = 900;

let timerId = null;

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const canEdit = computed(
  () => session.statusCd === "DRAFT" || session.statusCd === "OPENED"
);
const canCancel = computed(
  () => session.statusCd === "DRAFT" || session.statusCd === "OPENED"
);

// 교육자료/위험성평가 표시: 교육시작(IN_PROGRESS)/교육종료(COMPLETED) 상태에서 노출(6.2-(1)-2 / (3)-2)
const showEduRefs = computed(
  () => session.statusCd === "IN_PROGRESS" || session.statusCd === "COMPLETED"
);

const remainText = computed(() => {
  if (remainSec.value === null) return "--:--";
  const s = Math.max(0, remainSec.value);
  const mm = String(Math.floor(s / 60)).padStart(2, "0");
  const ss = String(s % 60).padStart(2, "0");
  return `${mm}:${ss}`;
});

onMounted(async () => {
  await fnSearch();
  startCountdown();
});

onUnmounted(() => {
  stopCountdown();
});

// 'yyyy-MM-dd HH:mm:ss' 문자열을 로컬 Date 로 파싱(브라우저 타임존 차이 회피)
const parsePrepStartAt = (str) => {
  if (!str) return null;
  const m = String(str).match(
    /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/
  );
  if (!m) return null;
  return new Date(
    Number(m[1]),
    Number(m[2]) - 1,
    Number(m[3]),
    Number(m[4]),
    Number(m[5]),
    Number(m[6])
  );
};

// prepStartAt 기준 남은 초 산출(표시 전용). OPENED 가 아니면 null.
const computeRemainSec = () => {
  if (session.statusCd !== "OPENED") {
    remainSec.value = null;
    return;
  }
  const base = parsePrepStartAt(session.prepStartAt);
  if (!base) {
    remainSec.value = null;
    return;
  }
  const elapsedSec = Math.floor((Date.now() - base.getTime()) / 1000);
  remainSec.value = Math.max(0, PREP_TIMER_SEC - elapsedSec);
  // 남은 시간이 다시 생기면(연장/재조회) 자동 재조회 가드 해제
  if (remainSec.value > 0) autoStartRefetched = false;
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/session-detail", {
      params: { sessionCd: props.sessionCd_p },
    });

    if (response.status === 200) {
      const data = response.data || {};
      rawDetail.value = data;
      contents.value = data.contents || [];
      risks.value = data.risks || [];
      const s = data.session || {};
      session.sessionCd = s.sessionCd || "";
      session.siteCd = s.siteCd || "";
      session.siteNm = s.siteNm || "";
      session.title = s.title || "";
      session.contentBody = s.contentBody || "";
      session.statusCd = s.statusCd || "";
      session.statusNm = s.statusNm || "";
      session.entryPwd = s.entryPwd || "";
      session.exitPwd = s.exitPwd || "";
      session.managerUserNm = s.managerUserNm || "";
      session.gpsVerifyTypeCd = s.gpsVerifyTypeCd || "";
      session.gpsVerifyRadiusM = s.gpsVerifyRadiusM;
      session.gpsManualConfirmYn = s.gpsManualConfirmYn || "";
      session.openedAt = s.openedAt || "";
      session.prepStartAt = s.prepStartAt || "";
      session.cancelReason = s.cancelReason || "";
      session.insertDate = s.insertDate || "";

      // 교육준비 단계면 prepStartAt 기준으로 남은 초 재산출
      computeRemainSec();

      // 연동 회사 지정 현황(PRAFTA-SUBCON-T5)
      fnSearchShares();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// ===== 연동 회사 지정(PRAFTA-SUBCON-T5) =====
// shareRows = 내가 직접 지정한 회사만(하위 재지정은 subCount 로만 노출 — 2차 이하 회사명 비노출).
const shareRows = ref([]);

// 지정/해제 가능 구간 = DRAFT | OPENED (서버 TBM_409_063 과 동일 기준)
const canManageShare = computed(
  () => session.statusCd === "DRAFT" || session.statusCd === "OPENED"
);

const fnSearchShares = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/session-shares", {
      params: { sessionCd: props.sessionCd_p },
    });
    if (response.status === 200) {
      shareRows.value = response.data?.shareList || [];
    }
  } catch (err) {
    shareRows.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnOpenSharePop = () => {
  openPop(TbmShareCmpnyPop, {
    sessionCd_p: props.sessionCd_p,
    onSaved: fnSearchShares,
  });
};

const fnReleaseShare = async (row) => {
  const confirmed = await proxy.$confirm(
    "지정을 해제하면 해당 회사(및 그 하위 재지정 회사)의 신규 입실이 차단됩니다. 이미 입실한 참석자는 유지됩니다."
  );
  if (!confirmed) return;

  try {
    const response = await axios.post("/webApi/tbm02/session-share-release", {
      sessionCd: props.sessionCd_p,
      shareCmpnyCd: row.cmpnyCd,
    });
    if (response.status === 200) {
      await proxy.$alert("연동 회사 지정을 해제했습니다.");
      fnSearchShares();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "처리 중 오류가 발생했습니다.")
    );
  }
};

// 교육준비 전이용 현재 위치 수집(TbmSessionForm 패턴 재사용)
const fnCaptureGps = () => {
  if (!navigator.geolocation) {
    gpsStatus.value = "fail";
    return;
  }
  gpsStatus.value = "loading";
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      form.managerGpsLat = String(pos.coords.latitude.toFixed(7));
      form.managerGpsLon = String(pos.coords.longitude.toFixed(7));
      gpsStatus.value = "ok";
    },
    () => {
      gpsStatus.value = "fail";
    }
  );
};

// 카운트다운(표시 전용). 실제 자동전이는 서버 스케줄러(prafta.tbm.autostart.enabled=true).
// T6-09: 0 도달 시 1회 자동 재조회 → 서버가 IN_PROGRESS 로 전이했으면 상태/콘솔이 갱신된다.
const startCountdown = () => {
  stopCountdown();
  timerId = setInterval(() => {
    if (remainSec.value === null) return;
    if (remainSec.value > 0) {
      remainSec.value -= 1;
      return;
    }
    // remainSec === 0: 교육준비 상태에서 1회만 재조회(자동전이 반영 확인)
    if (session.statusCd === "OPENED" && !autoStartRefetched) {
      autoStartRefetched = true;
      fnRefresh();
    }
  }, 1000);
};
const stopCountdown = () => {
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
};

// 검색 갱신 + 목록 갱신 콜백 동시 호출
const fnRefresh = async () => {
  await fnSearch();
  if (typeof props.onSearch === "function") props.onSearch();
};

// 교육준비(OPENED) 전이: GPS 좌표 확인 후 prepare-session
const fnPrepare = async () => {
  // GPS 검증유형이 AUTO 면 중심좌표 필수
  if (
    session.gpsVerifyTypeCd === "AUTO" &&
    (!form.managerGpsLat || !form.managerGpsLon)
  ) {
    await proxy.$alert("GPS 자동검증 교육은 현재 위치를 먼저 가져와야 합니다.");
    return;
  }
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/prepare-session",
      {
        sessionCd: session.sessionCd,
        managerGpsLat: form.managerGpsLat || null,
        managerGpsLon: form.managerGpsLon || null,
        gpsVerifyTypeCd: session.gpsVerifyTypeCd,
        gpsVerifyRadiusM: session.gpsVerifyRadiusM,
        gpsManualConfirmYn: session.gpsManualConfirmYn || null,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      const data = response.data || {};
      const msg = data.warningMessage
        ? `교육준비가 시작되었습니다.\n${data.warningMessage}`
        : "교육준비가 시작되었습니다. 입실 비밀번호가 발급되었습니다.";
      await proxy.$alert(msg);
      await fnRefresh();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "교육준비 시작 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 교육준비 연장(+15분): extend-prep → 성공 시 타이머 리셋
const fnExtend = async () => {
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/extend-prep",
      { sessionCd: session.sessionCd },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      remainSec.value = PREP_TIMER_SEC;
      await proxy.$alert("교육준비 시간이 15분 연장되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "연장 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 교육시작(IN_PROGRESS) 수동 전이: 확인 후 start-session
const fnStart = async () => {
  const ok = await proxy.$confirm(
    "교육을 시작하면 추가 입실이 마감됩니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/start-session",
      { sessionCd: session.sessionCd },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      await proxy.$alert("교육이 시작되었습니다.");
      await fnRefresh();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "교육 시작 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 교육종료(COMPLETED) 전이: 확인 후 complete-session(종료비번 발급)
const fnComplete = async () => {
  const ok = await proxy.$confirm(
    "교육을 종료하면 종료 비밀번호가 발급됩니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/complete-session",
      { sessionCd: session.sessionCd },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      const data = response.data || {};
      if (data.exitPwd) session.exitPwd = data.exitPwd;
      await proxy.$alert(
        "교육이 종료되었습니다. 종료 비밀번호가 발급되었습니다."
      );
      await fnRefresh();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "교육 종료 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 입실 비밀번호 재발급(OPENED): 기입실자 무영향
const fnRegenEntry = async () => {
  const ok = await proxy.$confirm(
    "입실 비밀번호를 재발급합니다. 기존에 발급된 비밀번호는 더 이상 사용할 수 없습니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/regenerate-passwords",
      { sessionCd: session.sessionCd },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      const data = response.data || {};
      if (data.entryPwd) session.entryPwd = data.entryPwd;
      await proxy.$alert("입실 비밀번호가 재발급되었습니다.");
      await fnSearch();
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "재발급 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 종료 비밀번호 재발급(COMPLETED)
const fnRegenExit = async () => {
  const ok = await proxy.$confirm(
    "종료 비밀번호를 재발급합니다. 기존에 발급된 비밀번호는 더 이상 사용할 수 없습니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  if (isBusy.value) return;
  isBusy.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbm02/regenerate-exit-password",
      { sessionCd: session.sessionCd },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      const data = response.data || {};
      const newExitPwd = data.exitPwd || "";
      if (newExitPwd) session.exitPwd = newExitPwd;
      await proxy.$alert("종료 비밀번호가 재발급되었습니다.");
      // T6-04: fnSearch 후에도 재발급된 종료비번이 화면에서 사라지지 않도록 보존
      //  (T6-03 게이트로 COMPLETED 에서 exitPwd 가 응답에 채워지지만, 응답이 비면 재발급값 유지)
      await fnSearch();
      if (newExitPwd && !session.exitPwd) session.exitPwd = newExitPwd;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "재발급 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

// 위험성평가 항목 클릭 → 읽기전용 상세 열람(RiskAssessInfo 재사용, 편집/저장 숨김)
const fnOpenRiskDetail = (risk) => {
  openPop(RiskAssessInfo, {
    readOnly: true,
    riskAssessmentData: {
      cmpnyCd: risk.cmpnyCd || sessionStorage.getItem("gv_cmpnyCd") || "",
      siteCd: risk.siteCd || "",
      processCd: risk.processCd || "",
      processNm: risk.processNm || "",
      riskTypeCd: risk.riskTypeCd || "",
      riskTypeNm: risk.riskTypeNm || "",
      hazardCd: risk.hazardCd || "",
      hazardNm: risk.hazardNm || "",
      assessmentCd: risk.assessmentCd || "",
      assessmentStatus: risk.assessmentStatus || "",
      assessmentStatusNm: risk.assessmentStatusNm || "",
      initLikelihoodScore: risk.initLikelihoodScore || "",
      initSeverityScore: risk.initSeverityScore || "",
      initRiskLv: risk.initRiskLv || "",
      initDesc: risk.initDesc || "",
      initAssessorId: risk.initAssessorId || "",
      initAssessorNm: risk.initAssessorNm || "",
      initAssessDate: risk.initAssessDate || "",
      initFileMgmtCd: risk.initFileMgmtCd || "",
      initFilePath: risk.initFilePath || "",
      revalDate: risk.revalDate || "",
      revalBeforeDesc: risk.revalBeforeDesc || "",
      revalLikelihoodScore: risk.revalLikelihoodScore || "",
      revalSeverityScore: risk.revalSeverityScore || "",
      revalRiskLv: risk.revalRiskLv || "",
      revalDesc: risk.revalDesc || "",
      revalAssessorId: risk.revalAssessorId || "",
      revalAssessorNm: risk.revalAssessorNm || "",
      revalAssessDate: risk.revalAssessDate || "",
      revalFileMgmtCd: risk.revalFileMgmtCd || "",
      revalFilePath: risk.revalFilePath || "",
    },
  });
};

// 수정(DRAFT/OPENED): TbmSessionForm 열기(상세 패턴 재사용)
const fnEdit = () => {
  openPop(TbmSessionForm, {
    sessionCd_p: session.sessionCd,
    detail_p: rawDetail.value,
    onSearch: () => {
      if (typeof props.onSearch === "function") props.onSearch();
      fnSearch();
    },
  });
  emit("close");
};

const fnCancelSession = () => {
  openPop(ReasonInputModal, {
    title: "TBM 교육 취소",
    message: "교육 취소 사유를 입력해 주세요.",
    placeholder: "취소 사유를 입력해 주세요.",
    required: true,
    onConfirm: async (reason) => {
      closePop();
      try {
        const response = await axios.post(
          "/webApi/tbm02/cancel-session",
          { sessionCd: session.sessionCd, cancelReason: reason },
          { headers: { "Content-Type": "application/json" } }
        );

        if (response.status === 200) {
          await proxy.$alert("교육이 취소되었습니다.");
          emit("close");
          if (typeof props.onSearch === "function") props.onSearch();
        }
      } catch (err) {
        await proxy.$alert(
          resolveApiErrorMessage(err, "취소 중 오류가 발생했습니다.")
        );
      }
    },
    onCancel: () => closePop(),
  });
};

const fnOpenManagerEntry = () => {
  openPop(TbmManagerEntryPop, {
    sessionCd_p: session.sessionCd,
    siteCd_p: session.siteCd,
    onSearch: fnSearch,
  });
};
const fnOpenGpsPanel = () => {
  openPop(TbmEntryGpsPanel, {
    sessionCd_p: session.sessionCd,
    radiusM_p: session.gpsVerifyRadiusM,
    onSearch: fnSearch,
  });
};
const fnOpenAttendance = () => {
  openPop(TbmAttendanceDetail, {
    sessionCd_p: session.sessionCd,
    sessionTitle_p: session.title,
    sessionStatusCd_p: session.statusCd,
    onSearch: fnSearch,
  });
};

const statusNm = (statusCd) => {
  switch (statusCd) {
    case "DRAFT":
      return "개설";
    case "OPENED":
      return "교육준비";
    case "IN_PROGRESS":
      return "교육시작";
    case "COMPLETED":
      return "교육종료";
    case "CANCELLED":
      return "취소";
    default:
      return statusCd || "-";
  }
};
const statusClass = (statusCd) => {
  switch (statusCd) {
    case "IN_PROGRESS":
      return "status-progress";
    case "OPENED":
      return "status-opened";
    case "DRAFT":
      return "status-draft";
    case "COMPLETED":
      return "status-completed";
    case "CANCELLED":
      return "status-cancelled";
    default:
      return "status-draft";
  }
};
const gpsTypeNm = (type) => {
  switch (type) {
    case "AUTO":
      return "자동";
    case "MANUAL":
      return "수동 확인";
    case "DISABLED":
      return "비활성";
    default:
      return type || "-";
  }
};
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.detail-wrapper {
  padding: 1.2rem;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
}

.meta-item {
  font-size: var(--btn-font-lg);
  font-weight: 600;
}

.console-section {
  margin-top: 1rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  background: var(--color-bg);
}

.console-title {
  font-weight: 600;
  color: var(--color-text-strong);
  margin-bottom: 0.75rem;
}

.console-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.console-hint {
  margin: 0;
  font-size: var(--btn-font);
  color: var(--color-text-muted);
}

/* 연동 회사: 섹션 제목 우측 끝에 지정 버튼을 배치 */
.share-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.gps-coord {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.gps-ok {
  font-size: var(--btn-font);
  color: var(--color-text-strong);
}

.gps-muted {
  font-size: var(--btn-font);
  color: var(--color-text-muted);
}

.pwd-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  background: var(--color-warning-bg);
  border-radius: var(--card-radius);
  padding: 0.75rem 1rem;
}

.pwd-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pwd-label {
  font-size: var(--btn-font-sm);
  color: var(--color-warning-text);
}

.pwd-value {
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.2rem;
  color: var(--color-text-strong);
}

.timer-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
}

.timer-box.timer-warn {
  border-color: var(--color-danger);
}

.timer-label {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.timer-value {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

.timer-warn .timer-value {
  color: var(--color-danger);
}

/* 교육준비 액션 버튼은 4개가 항상 한 행에 놓이도록 줄바꿈을 막는다 */
.console-btn-row {
  flex-wrap: nowrap;
}

.console-btn-row .btn {
  white-space: nowrap;
}

.detail-section {
  margin-top: 1.25rem;
}

.detail-section-title {
  font-weight: 600;
  color: var(--color-text-strong);
  margin-bottom: 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid var(--color-border);
}

.detail-empty {
  padding: 1rem;
  text-align: center;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-radius: var(--btn-radius);
}

.content-html {
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  background: var(--color-surface);
  color: var(--color-text);
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  white-space: nowrap;
}

.status-progress {
  background: var(--color-primary);
  color: var(--color-surface);
}

.status-opened {
  background: var(--color-surface);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-text);
}

.status-draft {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.status-completed {
  background: var(--color-bg);
  color: var(--color-text-strong);
  border: 1px solid var(--color-border-strong);
}

.status-cancelled {
  background: var(--color-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-border);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}

/* 교육자료 / 위험성평가 연계 목록(6.2-(1)-2 / (3)-2) */
.ref-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ref-item {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-surface);
}

.ref-item-risk {
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
}

.ref-name {
  font-size: var(--btn-font);
  font-weight: 600;
  color: var(--color-text-strong);
}

.ref-scope {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
}

.scope-common {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.scope-site {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.ref-type {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.ref-meta {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.title-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
  text-decoration: underline;
  font: inherit;
  font-weight: 600;
}

.title-link:hover {
  color: var(--color-primary-hover);
}
</style>
