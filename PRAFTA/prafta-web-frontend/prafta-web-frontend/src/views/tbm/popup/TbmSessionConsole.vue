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
            <span class="meta-item">사업장: {{ session.siteNm || session.siteCd }}</span>
            <span class="meta-item">개설자: {{ session.managerUserNm }}</span>
            <span class="meta-item">등록일: {{ session.insertDate }}</span>
            <span v-if="session.openedAt" class="meta-item">교육준비: {{ session.openedAt }}</span>
          </div>

          <!-- ===== 상태머신 액션 바 ===== -->
          <div class="console-section">
            <div class="console-title">교육 진행</div>

            <!-- DRAFT(개설): 교육준비 시작 -->
            <div v-if="session.statusCd === 'DRAFT'" class="console-actions">
              <p class="console-hint">
                교육준비를 시작하면 입실 비밀번호가 발급되고, 현재 위치가 GPS 중심좌표로 수집됩니다.
              </p>
              <div class="gps-coord">
                <span v-if="gpsStatus === 'ok'" class="gps-ok">
                  위도 {{ form.managerGpsLat }} / 경도 {{ form.managerGpsLon }}
                </span>
                <span v-else-if="gpsStatus === 'loading'" class="gps-muted">위치 수집 중...</span>
                <span v-else class="gps-muted">위치 미수집</span>
                <button type="button" class="btn btn-second btn-sm" @click="fnCaptureGps">
                  현재 위치 가져오기
                </button>
              </div>
              <button class="btn btn-primary" :disabled="isBusy" @click="fnPrepare">
                교육준비 시작
              </button>
            </div>

            <!-- OPENED(교육준비) -->
            <div v-else-if="session.statusCd === 'OPENED'" class="console-actions">
              <div class="pwd-section">
                <div class="pwd-box">
                  <span class="pwd-label">입실 비밀번호</span>
                  <span class="pwd-value">{{ session.entryPwd || "-" }}</span>
                </div>
                <button type="button" class="btn btn-second btn-sm" @click="fnRegenEntry">
                  입실비번 재발급
                </button>
              </div>

              <div class="timer-box" :class="{ 'timer-warn': remainSec !== null && remainSec <= 60 }">
                <span class="timer-label">자동 교육시작까지</span>
                <span class="timer-value">{{ remainText }}</span>
              </div>

              <div class="btn-group">
                <button class="btn btn-second" :disabled="isBusy" @click="fnExtend">교육 연장(+15분)</button>
                <button class="btn btn-primary" :disabled="isBusy" @click="fnStart">교육 시작</button>
              </div>
              <div class="btn-group">
                <button class="btn btn-second" @click="fnOpenManagerEntry">대리 / 일용직 입실</button>
                <button class="btn btn-second" @click="fnOpenGpsPanel">입실자 · 내보내기</button>
              </div>
            </div>

            <!-- IN_PROGRESS(교육시작) -->
            <div v-else-if="session.statusCd === 'IN_PROGRESS'" class="console-actions">
              <p class="console-hint">교육이 시작되어 입실이 마감되었습니다.</p>
              <button class="btn btn-primary" :disabled="isBusy" @click="fnComplete">교육 종료</button>
            </div>

            <!-- COMPLETED(교육종료) -->
            <div v-else-if="session.statusCd === 'COMPLETED'" class="console-actions">
              <div class="pwd-section">
                <div class="pwd-box">
                  <span class="pwd-label">종료 비밀번호</span>
                  <span class="pwd-value">{{ session.exitPwd || "-" }}</span>
                </div>
                <button type="button" class="btn btn-second btn-sm" @click="fnRegenExit">
                  종료비번 재발급
                </button>
              </div>
              <button class="btn btn-second" @click="fnOpenAttendance">참가자 · 미이수 처리</button>
            </div>

            <!-- CANCELLED(취소) -->
            <div v-else-if="session.statusCd === 'CANCELLED'" class="console-actions">
              <p class="console-hint">취소된 세션입니다. 사유: {{ session.cancelReason || "-" }}</p>
            </div>
          </div>

          <!-- 교육 내용 -->
          <div class="detail-section">
            <div class="detail-section-title">교육 내용</div>
            <div v-if="session.contentBody" class="content-html" v-html="session.contentBody"></div>
            <div v-else class="detail-empty">교육 내용이 없습니다.</div>
          </div>

          <!-- GPS 설정 -->
          <div class="detail-section">
            <div class="detail-section-title">GPS 검증</div>
            <div class="detail-meta">
              <span class="meta-item">유형: {{ gpsTypeNm(session.gpsVerifyTypeCd) }}</span>
              <span v-if="session.gpsVerifyTypeCd !== 'DISABLED'" class="meta-item">
                반경: {{ session.gpsVerifyRadiusM }}m
              </span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button v-if="canEdit" class="btn btn-second" @click="fnEdit">수정</button>
            <button v-if="canCancel" class="btn btn-second" @click="fnCancelSession">취소</button>
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
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
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm02/session-detail", {
      params: { sessionCd: props.sessionCd_p },
    });

    if (response.status === 200) {
      const data = response.data || {};
      rawDetail.value = data;
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
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
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

// 카운트다운(표시 전용). 실제 자동전이는 서버 스케줄러.
const startCountdown = () => {
  stopCountdown();
  timerId = setInterval(() => {
    if (remainSec.value !== null) remainSec.value -= 1;
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
    await proxy.$alert("GPS 자동검증 세션은 현재 위치를 먼저 가져와야 합니다.");
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
      await proxy.$alert("교육이 종료되었습니다. 종료 비밀번호가 발급되었습니다.");
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
      if (data.exitPwd) session.exitPwd = data.exitPwd;
      await proxy.$alert("종료 비밀번호가 재발급되었습니다.");
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
    title: "TBM 세션 취소",
    message: "세션 취소 사유를 입력해 주세요.",
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
          await proxy.$alert("세션이 취소되었습니다.");
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
  font-size: var(--btn-font);
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
</style>
