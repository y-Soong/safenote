<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>{{ session.title || "TBM 세션 상세" }}</span>
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
            <span v-if="session.openedAt" class="meta-item">
              개설일시: {{ session.openedAt }}
            </span>
          </div>

          <!-- 비밀번호 영역(OPENED/IN_PROGRESS + 관리자만) -->
          <div v-if="session.pwdVisible" class="detail-section pwd-section">
            <div class="detail-section-title">입실 / 종료 비밀번호</div>
            <div class="pwd-row">
              <div class="pwd-box">
                <span class="pwd-label">입실</span>
                <span class="pwd-value">{{ session.entryPwd }}</span>
              </div>
              <div class="pwd-box">
                <span class="pwd-label">종료</span>
                <span class="pwd-value">{{ session.exitPwd }}</span>
              </div>
              <button
                v-if="session.statusCd === 'OPENED'"
                type="button"
                class="btn btn-second btn-sm"
                @click="fnRegenerate"
              >
                비밀번호 재발급
              </button>
            </div>
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
              <span v-if="session.gpsVerifyTypeCd === 'AUTO'" class="meta-item">
                좌표: {{ session.managerGpsLat }} / {{ session.managerGpsLon }}
              </span>
              <span
                v-if="session.gpsVerifyTypeCd !== 'DISABLED'"
                class="meta-item"
              >
                반경: {{ session.gpsVerifyRadiusM }}m
              </span>
            </div>
          </div>

          <!-- 콘텐츠 매핑 -->
          <div class="detail-section">
            <div class="detail-section-title">교육 콘텐츠</div>
            <template v-if="contents.length === 0">
              <div class="detail-empty">연계된 콘텐츠가 없습니다.</div>
            </template>
            <template v-else>
              <ul class="map-list">
                <li
                  v-for="(c, idx) in contents"
                  :key="c.mtrlCd"
                  class="map-item"
                >
                  <span class="map-idx">{{ idx + 1 }}.</span>
                  <span class="map-title">{{ c.title }}</span>
                  <span class="map-sub">{{ c.mtrlTypeNm || c.mtrlType }}</span>
                  <span class="map-sub">항목 {{ c.itemCnt }}개</span>
                  <span v-if="c.overrideDesc" class="map-desc">
                    {{ c.overrideDesc }}
                  </span>
                </li>
              </ul>
            </template>
          </div>

          <!-- 위험성평가 매핑 -->
          <div class="detail-section">
            <div class="detail-section-title">
              위험성평가
              <span v-if="risks.length === 0" class="risk-warn">
                ⚠️ 위험성평가가 연계되지 않았습니다.
              </span>
            </div>
            <template v-if="risks.length === 0">
              <div class="detail-empty">연계된 위험성평가가 없습니다.</div>
            </template>
            <template v-else>
              <ul class="map-list">
                <li
                  v-for="(r, idx) in risks"
                  :key="riskKey(r)"
                  class="map-item"
                >
                  <span class="map-idx">{{ idx + 1 }}.</span>
                  <span class="map-title">{{
                    r.displayName || "(이름 미정)"
                  }}</span>
                  <span class="map-sub">
                    {{ r.assessmentStatusNm || r.assessmentStatus }}
                  </span>
                </li>
              </ul>
            </template>
          </div>

          <!-- 취소 사유(CANCELLED) -->
          <div v-if="session.statusCd === 'CANCELLED'" class="detail-section">
            <div class="detail-section-title">취소 사유</div>
            <p class="detail-desc">{{ session.cancelReason || "-" }}</p>
            <span class="meta-item">취소일시: {{ session.cancelledAt }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <!-- 수정/취소: DRAFT/OPENED -->
            <button v-if="canEdit" class="btn btn-second" @click="fnEdit">
              수정
            </button>
            <button
              v-if="canEdit"
              class="btn btn-second"
              @click="fnCancelSession"
            >
              취소
            </button>
            <!-- 교육 시작(C 연동 예정): OPENED에서만 노출, 비활성 -->
            <button
              v-if="session.statusCd === 'OPENED'"
              class="btn btn-primary"
              disabled
              title="실시간 진행 콘솔은 추후(C 단계) 연동 예정입니다."
              @click="fnStartGuide"
            >
              교육 시작 (준비중)
            </button>
            <!-- 진행 콘솔(IN_PROGRESS, C 이동 예정): 비활성 -->
            <button
              v-if="session.statusCd === 'IN_PROGRESS'"
              class="btn btn-primary"
              disabled
              title="진행 콘솔은 추후(C 단계) 연동 예정입니다."
              @click="fnStartGuide"
            >
              진행 콘솔 보기 (준비중)
            </button>
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
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import TbmSessionForm from "./TbmSessionForm.vue";
import ReasonInputModal from "@/components/modal/ReasonInputModal.vue";

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
  pwdVisible: false,
  managerUserNm: "",
  managerGpsLat: "",
  managerGpsLon: "",
  gpsVerifyTypeCd: "",
  gpsVerifyRadiusM: null,
  openedAt: "",
  cancelledAt: "",
  cancelReason: "",
  insertDate: "",
});
const contents = ref([]);
const risks = ref([]);
const rawDetail = ref(null);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const canEdit = computed(
  () => session.statusCd === "DRAFT" || session.statusCd === "OPENED"
);

const riskKey = (row) =>
  [row.siteCd, row.processCd, row.assessmentCd].join("|");

onMounted(async () => {
  await fnSearch();
});

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
      session.pwdVisible = !!s.pwdVisible;
      session.managerUserNm = s.managerUserNm || "";
      session.managerGpsLat = s.managerGpsLat || "";
      session.managerGpsLon = s.managerGpsLon || "";
      session.gpsVerifyTypeCd = s.gpsVerifyTypeCd || "";
      session.gpsVerifyRadiusM = s.gpsVerifyRadiusM;
      session.openedAt = s.openedAt || "";
      session.cancelledAt = s.cancelledAt || "";
      session.cancelReason = s.cancelReason || "";
      session.insertDate = s.insertDate || "";

      contents.value = data.contents || [];
      risks.value = data.risks || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

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
  // 취소 사유 입력(공통 ReasonInputModal). 확인 시 cancel-session 호출
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
    onCancel: () => {
      closePop();
    },
  });
};

const fnRegenerate = async () => {
  const ok = await proxy.$confirm(
    "기존 비밀번호를 무효화하고 새 비밀번호를 발급합니다. 계속하시겠습니까?"
  );
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm02/regenerate-passwords",
      { sessionCd: session.sessionCd },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      const data = response.data || {};
      session.entryPwd = data.entryPwd || "";
      session.exitPwd = data.exitPwd || "";
      await proxy.$alert("비밀번호가 재발급되었습니다.");
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "재발급 중 오류가 발생했습니다.")
    );
  }
};

// prafta-033-B: 교육 시작/진행 콘솔은 C 단계 소관. B에서는 안내만.
const fnStartGuide = async () => {
  await proxy.$alert("실시간 진행 기능은 추후(C 단계)에 연동될 예정입니다.");
};

const statusNm = (statusCd) => {
  switch (statusCd) {
    case "DRAFT":
      return "작성중";
    case "OPENED":
      return "개설";
    case "IN_PROGRESS":
      return "진행중";
    case "COMPLETED":
      return "종료";
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
  height: calc(100% - 110px);
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

.detail-desc {
  margin: 0.5rem 0;
  color: var(--color-text);
  white-space: pre-wrap;
}

.content-html {
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  background: var(--color-surface);
  color: var(--color-text);
}

.pwd-section {
  background: var(--color-warning-bg);
  border-radius: var(--card-radius);
  padding: 0.75rem 1rem;
}

.pwd-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
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

.map-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.map-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
}

.map-idx {
  font-weight: 600;
  color: var(--color-text-strong);
}

.map-title {
  flex: 1;
  color: var(--color-text-strong);
}

.map-sub {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.map-desc {
  width: 100%;
  font-size: var(--btn-font-sm);
  color: var(--color-text);
}

.risk-warn {
  font-size: var(--btn-font-sm);
  color: var(--color-danger);
  margin-left: 0.5rem;
}

/* 상태 배지 */
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
