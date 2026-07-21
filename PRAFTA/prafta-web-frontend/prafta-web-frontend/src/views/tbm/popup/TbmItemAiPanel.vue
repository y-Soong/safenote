<template>
  <!-- TBM_AI T3-3: 이미지(01)·PDF(04) 세부항목 AI 대화형 확정 팝업.
       RiskAiAnalysisPanel.vue 패턴 준용(초안 → 멀티턴 채팅 → 확정 / 재분석 / AI 작업중 블러 오버레이).
       분석 실패(FAILED) 항목은 수기 요지 입력으로 확정(manual-confirm).
       ★template/style 골격만. script 의 API 호출/폴링/상태전이는 developer 가 T1 EP 확정본으로 채운다. -->
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @keydown.ctrl.a.stop
      @keydown.meta.a.stop
    >
      <div
        class="modal-content-normal tbmai-panel"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>AI 분석 확정 — {{ itemTypeLabel }}</span>
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

        <!-- 본문 -->
        <div class="tbmai-body">
          <!-- AI 작업중 블러 오버레이(초안 로드/채팅/재분석/확정 공통) -->
          <div v-if="aiBusy" class="tbmai-busy">
            <LoadingSpinner />
            <span class="tbmai-busy__text">{{ busyText }}</span>
          </div>

          <div class="tbmai-split">
            <!-- 좌측: 첨부 미리보기(이미지 / PDF) -->
            <div class="tbmai-preview">
              <div class="tbmai-preview__title">첨부 미리보기</div>
              <div class="tbmai-preview__frame">
                <img
                  v-if="props.itemType_p === '01' && props.fileUrl_p"
                  :src="props.fileUrl_p"
                  class="tbmai-preview__img"
                  alt="이미지 미리보기"
                />
                <iframe
                  v-else-if="props.itemType_p === '04' && props.fileUrl_p"
                  :src="props.fileUrl_p"
                  class="tbmai-preview__pdf"
                  frameborder="0"
                ></iframe>
                <div v-else class="tbmai-preview__na">
                  미리보기를 제공할 수 없습니다.
                </div>
              </div>
              <a
                v-if="props.itemType_p === '04' && props.fileUrl_p"
                :href="props.fileUrl_p"
                target="_blank"
                rel="noopener"
                class="tbmai-preview__link"
                >새 탭에서 PDF 열기</a
              >
              <p v-if="props.fileNm_p" class="tbmai-preview__name">
                {{ props.fileNm_p }}
              </p>
            </div>

            <!-- 우측: 상태 + (DRAFT/CONFIRMED) 채팅 확정 / (FAILED) 수기 요지 -->
            <div class="tbmai-right">
              <div class="tbmai-status">
                <span class="tbmai-status__label">AI 분석 상태</span>
                <span class="tbmai-status__badge" :class="statusBadgeClass">
                  {{ statusLabel }}
                </span>
              </div>

              <!-- 분석 실패: 수기 요지 입력으로 확정 -->
              <template v-if="aiStatus === 'FAILED'">
                <p class="tbmai-alert tbmai-alert--error">
                  AI 분석에 실패했습니다. 아래에 이 자료의 안전 요지를 직접
                  입력해 확정할 수 있습니다.
                </p>
                <label class="tbmai-field-label" for="tbmaiManual">
                  수기 확정 요지
                </label>
                <textarea
                  id="tbmaiManual"
                  v-model="manualDesc"
                  class="tbmai-textarea"
                  rows="6"
                  placeholder="이미지·PDF에서 강조할 핵심 안전 요지를 입력하세요."
                  :disabled="aiBusy"
                ></textarea>
              </template>

              <!-- 분석완료(초안/확정): 멀티턴 채팅으로 보완·정정 후 확정 -->
              <template v-else>
                <div class="tbmai-chat__log" ref="chatLogRef">
                  <div
                    v-for="(t, i) in visibleTurns"
                    :key="'turn-' + i"
                    class="tbmai-chat__row"
                    :class="
                      t.role === 'user'
                        ? 'tbmai-chat__row--user'
                        : 'tbmai-chat__row--assistant'
                    "
                  >
                    <span class="tbmai-chat__bubble">{{ t.text }}</span>
                  </div>
                  <div
                    v-if="visibleTurns.length === 0"
                    class="tbmai-chat__empty"
                  >
                    AI 초안을 불러오는 중입니다…
                  </div>
                </div>

                <p v-if="chatErrorMsg" class="tbmai-alert tbmai-alert--error">
                  {{ chatErrorMsg }}
                </p>

                <div class="tbmai-chat__input">
                  <textarea
                    v-model="chatInput"
                    class="tbmai-textarea"
                    rows="2"
                    placeholder="AI 초안을 보완·정정할 설명이나 질문을 입력하세요. (예: 방호덮개가 아니라 안전난간입니다.)"
                    :disabled="aiBusy"
                  ></textarea>
                  <div class="tbmai-chat__input-actions">
                    <span class="tbmai-chat__hint">
                      확정 시 이 대화의 결과가 교육안 생성에 반영됩니다.
                    </span>
                    <button
                      type="button"
                      class="btn btn-second tbmai-chat__send"
                      :disabled="aiBusy || !chatInput.trim()"
                      @click="fnSendChat"
                    >
                      전송
                    </button>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-second"
              :disabled="aiBusy"
              @click="$emit('close')"
            >
              닫기
            </button>
            <button
              class="btn btn-second"
              :disabled="aiBusy"
              @click="fnReanalyze"
            >
              재분석
            </button>
            <button
              class="btn btn-primary"
              :disabled="aiBusy"
              @click="fnConfirm"
            >
              확정
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
// ================ Imports ================
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  onBeforeUnmount,
  nextTick,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage, isAiQuotaExceeded } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";

// ================ Instance ================
const { proxy } = getCurrentInstance();

// ================ Props & Emits ================
// mtrlCd_p/mtrlItemCd_p: 확정 대상 세부항목 식별. itemType_p: '01'(이미지)/'04'(PDF).
// fileUrl_p/fileNm_p: 좌측 미리보기용(서버 서명 URL). onConfirmed: 확정/재분석 후 부모 그리드 배지 갱신 콜백.
const props = defineProps({
  mtrlCd_p: String,
  mtrlItemCd_p: String,
  itemType_p: String,
  fileUrl_p: String,
  fileNm_p: String,
  onConfirmed: Function,
});
const emit = defineEmits(["close"]);

// ================ Refs (Variables) ================
const modalRef = ref(null);
const chatLogRef = ref(null);

// 서버 상태(초기값만 — developer 가 analysis-status/chat-item 응답으로 채움)
const aiStatus = ref(""); // SYS056: DRAFT | FAILED | CONFIRMED | ANALYZING | NONE
const chatTurns = ref([]); // [{ role: 'user'|'assistant', text }]
const draftText = ref(""); // AI 초안(AI_DRAFT_TEXT)
const manualDesc = ref(""); // FAILED 수기 요지 입력값

// UI 상태
const initialLoading = ref(false);
const chatSending = ref(false);
const reanalyzing = ref(false);
const confirming = ref(false);
const analyzing = ref(false); // 비동기 VLM 분석(ANALYZING) 진행 중(폴링 구간) — 블러 오버레이 표시용
const chatInput = ref("");
const chatErrorMsg = ref("");

// 폴링 제어(ANALYZING → DRAFT/FAILED 전이 감시). VLM 은 수 초~수십 초 소요될 수 있어 넉넉히 잡는다.
const POLL_INTERVAL_MS = 2500;
const POLL_MAX_TRIES = 60; // 2.5s * 60 = 최대 약 2분 30초
let pollTimer = null;
let pollTries = 0;

// 화면 중앙(살짝 위) 배치 + 드래그
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Computed ================
const aiBusy = computed(
  () =>
    initialLoading.value ||
    chatSending.value ||
    reanalyzing.value ||
    confirming.value ||
    analyzing.value
);
const busyText = computed(() =>
  initialLoading.value ? "불러오는 중…" : "AI 분석중…"
);
const visibleTurns = computed(() => chatTurns.value.filter((t) => t && t.text));

const itemTypeLabel = computed(() =>
  props.itemType_p === "04" ? "PDF" : "이미지"
);

// SYS056 라벨/배지 색상(코드값 → 한글). developer 가 systCode(SYS056) 매핑으로 교체 가능하나
// 골격은 고정 매핑으로 표시.
const STATUS_LABEL = {
  // 이 패널은 01(이미지)/04(PDF) 에서만 열리므로 NONE 은 "분석대상아님"이 아니라 "분석대기"다.
  NONE: "분석대기",
  ANALYZING: "분석중",
  DRAFT: "분석완료-미확정",
  FAILED: "분석실패",
  CONFIRMED: "분석완료-확정",
};
const statusLabel = computed(() => STATUS_LABEL[aiStatus.value] || "-");
const statusBadgeClass = computed(() => ({
  "tbmai-status__badge--draft": aiStatus.value === "DRAFT",
  "tbmai-status__badge--analyzing": aiStatus.value === "ANALYZING",
  "tbmai-status__badge--failed": aiStatus.value === "FAILED",
  "tbmai-status__badge--confirmed": aiStatus.value === "CONFIRMED",
}));

// ================ Life Cycle Functions ================
onMounted(() => {
  fnLoad();
});

onBeforeUnmount(() => {
  stopPolling();
});

// ================ User Functions ================
// 채팅 로그를 최하단으로 스크롤(신규 턴 반영 후 호출).
const scrollChatToBottom = () => {
  nextTick(() => {
    const el = chatLogRef.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
};

// 서버 항목 응답(analysis-status 항목 or 액션 EP 응답)을 로컬 상태에 반영.
//  ★대화턴이 비어 있고 초안만 있으면 초안을 assistant 첫 발화로 시드(러너가 시드하지만 방어).
const applyItem = (data) => {
  if (!data) return;
  aiStatus.value = data.aiStatus || "";
  draftText.value = data.aiDraftText || "";
  const turns = Array.isArray(data.turns)
    ? data.turns.filter((t) => t && t.text)
    : [];
  if (turns.length > 0) {
    chatTurns.value = turns;
  } else if (draftText.value) {
    chatTurns.value = [{ role: "assistant", text: draftText.value }];
  } else if (data.aiStatus === "CONFIRMED" && data.aiConfirmDesc) {
    chatTurns.value = [{ role: "assistant", text: data.aiConfirmDesc }];
  } else {
    chatTurns.value = [];
  }
  // FAILED 재진입 시 기존 확정 서술이 있으면 수기 요지 입력값으로 프리필(사용자 미입력 상태에서만).
  if (data.aiStatus === "FAILED" && data.aiConfirmDesc && !manualDesc.value) {
    manualDesc.value = data.aiConfirmDesc;
  }
};

// analysis-status 응답에서 이 항목(mtrlItemCd) 1건을 추출.
const pickItem = (data) => {
  const items = (data && Array.isArray(data.items)) ? data.items : [];
  return items.find((it) => it && it.mtrlItemCd === props.mtrlItemCd_p) || null;
};

// 폴링 중지(타이머 해제 + 분석 플래그 해제).
const stopPolling = () => {
  if (pollTimer) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
  analyzing.value = false;
};

// ANALYZING → DRAFT/FAILED 전이 폴링 시작(블러 오버레이 유지).
const startPolling = () => {
  stopPolling();
  analyzing.value = true;
  pollTries = 0;
  pollTick();
};

const pollTick = () => {
  pollTimer = setTimeout(async () => {
    pollTries += 1;
    try {
      const response = await axios.get("/webApi/tbmai01/analysis-status", {
        params: { mtrlCd: props.mtrlCd_p },
      });
      const item = pickItem(response.data);
      if (item) applyItem(item);
      // 전이 완료(ANALYZING 아님) → 폴링 종료 + 스크롤.
      if (!item || item.aiStatus !== "ANALYZING") {
        stopPolling();
        scrollChatToBottom();
        return;
      }
    } catch (err) {
      // 폴링 실패는 조용히 종료(사용자는 재분석/닫기로 재시도).
      stopPolling();
      return;
    }
    if (pollTries >= POLL_MAX_TRIES) {
      // 시간 초과: 분석이 지연/실패 가능. 폴링만 멈추고 상태는 화면 유지.
      stopPolling();
      return;
    }
    pollTick();
  }, POLL_INTERVAL_MS);
};

// ================ API Functions ================
// 초안/대화이력/상태 로드. DRAFT 이면서 대화 없으면 초안을 첫 assistant 턴으로 표시.
const fnLoad = async () => {
  initialLoading.value = true;
  try {
    const response = await axios.get("/webApi/tbmai01/analysis-status", {
      params: { mtrlCd: props.mtrlCd_p },
    });
    const item = pickItem(response.data);
    if (item) applyItem(item);
    scrollChatToBottom();
    // 로드 시점에 분석 중이면 폴링 재개(다른 화면에서 트리거된 분석 반영).
    if (aiStatus.value === "ANALYZING") startPolling();
  } catch (err) {
    chatErrorMsg.value = resolveApiErrorMessage(
      err,
      "AI 분석 정보를 불러오지 못했습니다."
    );
  } finally {
    initialLoading.value = false;
  }
};

// 멀티턴 채팅 전송 → AI 응답 turn 추가.
const fnSendChat = async () => {
  const message = chatInput.value.trim();
  if (!message) return;
  chatErrorMsg.value = "";
  chatSending.value = true;
  try {
    // ★타임아웃 상향: VLM(이미지 재부착) 응답 지연 대비 — 전역 axios 10초로는 부족할 수 있음.
    const response = await axios.post(
      "/webApi/tbmai01/chat-item",
      { mtrlItemCd: props.mtrlItemCd_p, userMessage: message },
      { headers: { "Content-Type": "application/json" }, timeout: 60000 }
    );
    applyItem(response.data);
    chatInput.value = "";
    scrollChatToBottom();
  } catch (err) {
    chatErrorMsg.value = resolveApiErrorMessage(
      err,
      "대화 처리 중 오류가 발생했습니다."
    );
    // 회사 월간 AI 토큰 쿼터 소진(AI_429_001) → Alert 모달 우선 표출(inline 병기 — §2-5)
    if (isAiQuotaExceeded(err)) {
      await proxy.$alert(chatErrorMsg.value);
    }
  } finally {
    chatSending.value = false;
  }
};

// 재분석: VLM 재판독(비동기) → 상태 ANALYZING → 폴링 재개.
//  (NONE/미분석 항목의 최초 분석 시작에도 사용된다 — 서버 reanalyze 는 상태 무관 재진입.)
const fnReanalyze = async () => {
  const ok = await proxy.$confirm(
    "이 항목을 다시 분석할까요? 진행 중인 초안/대화가 새 분석 결과로 대체됩니다."
  );
  if (!ok) return;
  chatErrorMsg.value = "";
  reanalyzing.value = true;
  try {
    const response = await axios.post(
      "/webApi/tbmai01/reanalyze",
      { mtrlItemCd: props.mtrlItemCd_p },
      { headers: { "Content-Type": "application/json" }, timeout: 60000 }
    );
    applyItem(response.data);
    // 서버가 ANALYZING 선커밋 후 비동기 트리거 → 폴링으로 완료 감시.
    startPolling();
  } catch (err) {
    chatErrorMsg.value = resolveApiErrorMessage(
      err,
      "재분석 요청 중 오류가 발생했습니다."
    );
    // 회사 월간 AI 토큰 쿼터 소진(AI_429_001) → Alert 모달 우선 표출(inline 병기 — §2-5)
    if (isAiQuotaExceeded(err)) {
      await proxy.$alert(chatErrorMsg.value);
    }
  } finally {
    reanalyzing.value = false;
  }
};

// 확정: FAILED 이면 manual-confirm(수기 요지), 그 외엔 confirm-item(대화/초안 결과 확정).
const fnConfirm = async () => {
  if (aiStatus.value === "ANALYZING") {
    await proxy.$alert("분석이 진행 중입니다. 완료 후 확정해 주세요.");
    return;
  }

  const isFailed = aiStatus.value === "FAILED";

  if (isFailed) {
    if (!manualDesc.value.trim()) {
      await proxy.$alert("수기 확정 요지를 입력해 주세요.");
      return;
    }
  } else {
    // 확정할 근거(대화/초안)가 없으면 먼저 분석을 실행하도록 안내.
    const hasBasis =
      draftText.value.trim() ||
      chatTurns.value.some((t) => t && t.role === "assistant" && t.text);
    if (!hasBasis) {
      await proxy.$alert("먼저 AI 분석을 실행한 뒤 확정해 주세요. (재분석)");
      return;
    }
  }

  confirming.value = true;
  try {
    if (isFailed) {
      await axios.post(
        "/webApi/tbmai01/manual-confirm",
        { mtrlItemCd: props.mtrlItemCd_p, manualText: manualDesc.value.trim() },
        { headers: { "Content-Type": "application/json" } }
      );
    } else {
      // confirmText 미지정 → 서버가 최신 assistant 턴(없으면 초안)을 확정 서술로 채택.
      await axios.post(
        "/webApi/tbmai01/confirm-item",
        { mtrlItemCd: props.mtrlItemCd_p },
        { headers: { "Content-Type": "application/json" } }
      );
    }
    if (typeof props.onConfirmed === "function") props.onConfirmed();
    emit("close");
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "확정 중 오류가 발생했습니다.")
    );
  } finally {
    confirming.value = false;
  }
};
</script>

<style scoped>
/* ── 패널 컨테이너 ── */
.tbmai-panel {
  width: 90%;
}

/* ── 본문 ──
   ★높이 체인: .modal-content-normal 은 max-height(90vh)만 있고 고정 height 가 없다.
     따라서 자식이 height:100% 로 부모 높이를 참조하면 auto 로 해석돼 무한히 늘어난다.
     본문을 flex 컨테이너로 만들어, 내용이 길어지면 flex-shrink 로 높이가 확정되고
     그 안의 채팅 로그가 스크롤되도록 한다(팝업은 내용이 짧으면 그만큼만 커진다). */
.tbmai-body {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  padding: 1.25rem;
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
}

/* AI 작업중 블러 오버레이 */
.tbmai-busy {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: var(--color-text);
}
.tbmai-busy__text {
  font-weight: 600;
}

/* 좌우 분할(좌 미리보기 / 우 확정).
   height:100% 대신 flex 로 남은 높이를 채운다(부모 높이가 auto 여도 안전). */
.tbmai-split {
  display: flex;
  gap: 1.25rem;
  flex: 1 1 auto;
  min-height: 0;
}

/* ── 좌측 미리보기 ── */
.tbmai-preview {
  flex: 0 0 42%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.tbmai-preview__title {
  flex: 0 0 auto;
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: var(--btn-font-sm);
}
/* 남은 높이를 채우되(min-height 로 최소 크기 확보) 사진이 컬럼을 밀어내지 않게 한다. */
.tbmai-preview__frame {
  flex: 1 1 auto;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  background: var(--color-bg);
  overflow: hidden;
}
.tbmai-preview__img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.tbmai-preview__pdf {
  width: 100%;
  height: 100%;
  border: none;
}
.tbmai-preview__na {
  padding: 1rem;
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}
.tbmai-preview__link {
  flex: 0 0 auto;
  color: var(--color-primary);
  font-size: var(--btn-font-sm);
}
.tbmai-preview__name {
  flex: 0 0 auto;
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
  word-break: break-all;
}

/* ── 우측 확정 영역 ──
   ★min-height:0 필수. 없으면 자동 최소높이(=내용 크기)가 걸려 채팅 로그가 줄어들지 못하고
     입력칸을 본문 밖으로 밀어낸다(overflow:hidden 이라 잘려서 사라진다). */
.tbmai-right {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

/* 상태 배지 줄은 축소 금지(항상 상단 고정) */
.tbmai-status {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.tbmai-status__label {
  font-weight: 600;
  color: var(--color-text-strong);
  font-size: var(--btn-font-sm);
}
.tbmai-status__badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}
.tbmai-status__badge--analyzing {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.tbmai-status__badge--draft {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}
.tbmai-status__badge--failed {
  background: var(--color-surface);
  color: var(--color-danger);
  border-color: var(--color-danger);
}
.tbmai-status__badge--confirmed {
  background: var(--color-surface);
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.tbmai-field-label {
  flex: 0 0 auto;
  font-weight: 600;
  color: var(--color-text);
  font-size: var(--btn-font-sm);
}

/* 오류/안내 문구도 축소 금지(채팅 로그만 줄어들게 한다) */
.tbmai-alert {
  flex: 0 0 auto;
  margin: 0;
  padding: 0.4rem 0.6rem;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
}
.tbmai-alert--error {
  color: var(--color-danger);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
}

.tbmai-textarea {
  resize: vertical;
  min-height: 2.4rem;
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--btn-font);
  font-family: inherit;
}
.tbmai-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
/* 수기 확정(FAILED) textarea 는 우측 컬럼의 직계 자식 — 남은 높이를 채우고 내부에서 스크롤한다.
   (채팅 입력 textarea 는 .tbmai-chat__input 안에 있어 이 규칙에 걸리지 않는다) */
.tbmai-right > .tbmai-textarea {
  flex: 1 1 auto;
  min-height: 0;
}

/* ── 채팅 ──
   유일하게 스크롤되는 영역. min-height 는 "최소 표시 높이"이자 축소 하한이므로,
   좁은 화면에서 입력칸을 밀어내지 않도록 낮게 잡는다(대화가 길면 여기서 스크롤). */
.tbmai-chat__log {
  flex: 1 1 auto;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  overflow-y: auto;
  padding: 0.6rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--input-radius);
}
.tbmai-chat__row {
  display: flex;
}
.tbmai-chat__row--user {
  justify-content: flex-end;
}
.tbmai-chat__row--assistant {
  justify-content: flex-start;
}
.tbmai-chat__bubble {
  max-width: 78%;
  padding: 0.45rem 0.7rem;
  border-radius: var(--input-radius);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: var(--btn-font);
  line-height: 1.4;
}
.tbmai-chat__row--user .tbmai-chat__bubble {
  background: var(--color-primary);
  color: #ffffff;
}
.tbmai-chat__row--assistant .tbmai-chat__bubble {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
.tbmai-chat__empty {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
  padding: 0.4rem 0;
  text-align: center;
}
/* 입력칸은 축소·클리핑 금지(대화 길이와 무관하게 항상 보인다) */
.tbmai-chat__input {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.tbmai-chat__input-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.tbmai-chat__hint {
  color: var(--color-text-muted);
  font-size: var(--btn-font-sm);
}
.tbmai-chat__send {
  margin-left: auto;
}
</style>
