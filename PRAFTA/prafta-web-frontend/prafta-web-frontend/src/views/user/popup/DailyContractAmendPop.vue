<template>
  <!--
    DailyContractAmendPop.vue — 일용직 근로계약서 정정(in-place) 팝업 (웹)
    - 분해: .claude/requests/common/작업지시서_계약서-승인시점-버전확정.ui.plan.md §4 UI-T7b
    - 요청서 근거: K6·K7(서명 0건 조건 · 당일 조건 없음), J7~J11, Q2(pin 카운트 분리), Q5(계약서명 수정 불허)
    - 참조 패턴: DailyContractRegPop(modal-overlay/modal-content-narrow 규약, 파일 필드·미리보기·PDF 안내)
    - ★등록 팝업과 분리한 이유: 문면이 "전원 재서명" ↔ "재서명 없음"으로 반대라 한 파일에 두면 오조작 위험이 커진다.
    - script: precheck 조회 / 저장(multipart POST) 구현 완료(developer). template·style 은 planner 골격 원문.
  -->
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <!-- Title -->
        <div class="modal-header">
          <span>계약서 정정 (버전 유지)</span>
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

        <!-- Form -->
        <div class="form-container">
          <!-- 항상 표시: 정정의 결과를 명시(교체와 반대) -->
          <p class="amend-guide">
            현재 버전 <strong>v{{ props.contractVer }}</strong> 의 파일만 제자리
            교체합니다.<br />
            <strong>버전은 올라가지 않으며 재서명이 발생하지 않습니다.</strong
            ><br />
            계약서명은 변경할 수 없습니다.
          </p>

          <!-- 차단: 서명자 존재 / precheck 실패 (서버가 최종 방어하지만 업로드 자체를 막는다) -->
          <p v-if="signCnt > 0" class="amend-block">
            이미 서명한 근로자가 {{ signCnt }}명 있어 정정할 수 없습니다.<br />
            내용을 바꿔야 한다면 [등록]으로 새 버전을 등록해 주세요(해당 사업장
            전원 재서명).
          </p>
          <p v-else-if="precheckFailed" class="amend-block">
            정정 가능 여부를 확인하지 못했습니다. 닫고 다시 시도해 주세요.
          </p>

          <!-- 경고: Q2 — 승인 건(이미 pin)과 대기 건(승인 시 확정)을 분리해 표현 -->
          <ul v-if="showWarn" class="amend-warn">
            <li v-if="pinnedApprovedCnt > 0">
              이 계약서를 확정(pin)한 승인 {{ pinnedApprovedCnt }}건이 정정된
              내용으로 서명하게 됩니다.
            </li>
            <li v-if="pendingCnt > 0">
              대기 중인 입장 요청 {{ pendingCnt }}건은 승인 시 정정된 내용으로
              확정됩니다.
            </li>
          </ul>
          <p v-else-if="loading" class="amend-note">
            정정 가능 여부를 확인하고 있습니다.
          </p>
          <p v-else-if="showNoRefNote" class="amend-note">
            이 버전을 확정한 승인 요청과 대기 중인 입장 요청이 없습니다.
          </p>

          <!-- 계약서명: Q5 — 정정에서는 수정 불허(고정 표시만) -->
          <div class="form-row-max">
            <label>계약서명</label>
            <input id="amendContractNm" :value="props.contractNm" disabled />
          </div>

          <!-- 정정 대상 버전 명시(오인 방지) -->
          <div class="form-row-max">
            <label>대상 버전</label>
            <span class="amend-ver">v{{ props.contractVer }} (사용중)</span>
          </div>

          <div class="form-row-max form-row-top">
            <label>계약서 파일</label>
            <div class="file-field">
              <input
                ref="fileInputRef"
                type="file"
                accept="application/pdf,image/png,image/jpeg"
                class="file-field__input"
                @change="onFileChange"
              />
              <p class="file-field__hint">
                PDF 또는 이미지(JPG/PNG), 10MB 이하, 최대 20페이지
              </p>
              <span class="form-msg">{{ fileMsg }}</span>
            </div>
          </div>

          <!-- 미리보기(이미지 전용) — 팝업 높이 바운딩 규약: 내부 스크롤로 수용 -->
          <div v-if="previewUrl" class="preview-box">
            <img
              class="preview-box__img"
              :src="previewUrl"
              alt="계약서 미리보기"
            />
          </div>

          <!-- PDF 선택 시: 팝업 내 렌더 없이 파일 정보 + 확인 경로 안내 -->
          <div v-else-if="isPdfSelected" class="file-note">
            <p class="file-note__name">{{ selectedFile.name }}</p>
            <p class="file-note__meta">PDF · {{ selectedFileSizeText }}</p>
            <p class="file-note__guide">
              PDF는 이 팝업에서 미리보기를 제공하지 않습니다. 저장 후 활성
              카드의 [미리보기]로 내용을 확인해 주세요.
            </p>
          </div>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(저장, primary), 오른쪽=이탈(취소, ghost), 폭 균등 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="!canSave"
              @click="fnSave"
            >
              저장
            </button>
            <button class="btn btn-second" @click="$emit('close')">
              취소
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  getCurrentInstance,
  onMounted,
  onBeforeUnmount,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

// siteCd/contractVer/contractNm: 정정 대상(활성 계약서) / onSaved: 저장 성공 콜백
const props = defineProps({
  siteCd: String,
  contractVer: Number,
  contractNm: String,
  onSaved: Function,
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
// precheck 는 부모가 넘긴 값을 쓰지 않고 팝업이 마운트 시 직접 조회한다 —
//   부모 조회 시점 이후에 서명이 커밋될 수 있고, 경고 건수는 "지금" 값이어야 판단이 유효하다.
const precheck = ref(null); // { amendable, signCnt, pinnedApprovedCnt, pendingCnt }
const loading = ref(false);
const precheckFailed = ref(false);

const fileMsg = ref("");
const selectedFile = ref(null);
const previewUrl = ref("");
const saving = ref(false);
const fileInputRef = ref(null);

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

// 서버 화이트리스트와 동일한 contentType 만 1차 통과시킨다(서버가 매직바이트/페이지 수/암호 PDF/픽셀 상한 재검증).
const PDF_MIME = "application/pdf";
const ALLOWED_MIMES = [PDF_MIME, "image/png", "image/jpeg"];

// =========================== Computed (표시 전용) ===========================
const signCnt = computed(() => precheck.value?.signCnt ?? 0);
const pinnedApprovedCnt = computed(
  () => precheck.value?.pinnedApprovedCnt ?? 0,
);
const pendingCnt = computed(() => precheck.value?.pendingCnt ?? 0);

const showWarn = computed(
  () => pinnedApprovedCnt.value > 0 || pendingCnt.value > 0,
);
const showNoRefNote = computed(
  () =>
    !!precheck.value &&
    signCnt.value === 0 &&
    pinnedApprovedCnt.value === 0 &&
    pendingCnt.value === 0,
);

// 저장 가능 = precheck 통과 + 파일 선택 + 저장 중 아님
const canSave = computed(
  () =>
    !saving.value &&
    !loading.value &&
    precheck.value?.amendable === true &&
    !!selectedFile.value,
);

const isPdfSelected = computed(
  () => (selectedFile.value?.type || "") === PDF_MIME,
);

// 파일 크기 표기(1MB 미만은 KB) — PDF 안내 블록 표시용
const selectedFileSizeText = computed(() => {
  const size = selectedFile.value?.size || 0;
  if (size <= 0) return "";
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))}KB`;
  return `${(size / (1024 * 1024)).toFixed(1)}MB`;
});

// =========================== Life Cycle ===========================
onMounted(async () => {
  await fnLoadPrecheck();
});

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
});

// =========================== Methods ===========================
// 정정 가능 여부 + 경고 건수 — GET /webApi/user07/contract-amend-precheck?siteCd=&contractVer=
//   실패는 $alert 로 흐름을 끊지 않고 화면 내 .amend-block 문구 + 저장 비활성으로 처리한다
//   (정정 가능 여부가 불명한 상태에서 업로드를 시도시키지 않는다).
const fnLoadPrecheck = async () => {
  loading.value = true;
  precheckFailed.value = false;
  precheck.value = null;

  try {
    const response = await axios.get("/webApi/user07/contract-amend-precheck", {
      params: { siteCd: props.siteCd, contractVer: props.contractVer },
    });

    if (response.status === 200) {
      const data = response.data || {};
      // amendable 은 반드시 엄격 비교(=== true)로 판정한다 — 문자열 "true"/누락(undefined)을
      //   truthy 로 통과시키면 서명자가 있는 버전에서도 저장 버튼이 열린다.
      precheck.value = {
        amendable: data.amendable === true,
        signCnt: Number(data.signCnt ?? 0),
        pinnedApprovedCnt: Number(data.pinnedApprovedCnt ?? 0),
        pendingCnt: Number(data.pendingCnt ?? 0),
      };
    }
  } catch (err) {
    precheck.value = null;
    precheckFailed.value = true;
    console.warn("[AmendPop] 정정 precheck 조회 실패:", err?.message);
  } finally {
    loading.value = false;
  }
};

// 파일 선택 → 클라이언트 1차 검증(형식/크기) + 미리보기(이미지만 — PDF 는 안내 블록으로 대체)
const onFileChange = (e) => {
  fileMsg.value = "";
  const file = e.target.files?.[0] || null;

  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
    previewUrl.value = "";
  }
  selectedFile.value = null;

  if (!file) return;

  if (!ALLOWED_MIMES.includes(file.type)) {
    fileMsg.value = "PDF 또는 이미지(JPG/PNG) 파일만 업로드할 수 있습니다.";
    fileInputRef.value.value = "";
    return;
  }
  if (file.size > MAX_FILE_SIZE) {
    fileMsg.value = "파일 크기는 10MB 이하여야 합니다.";
    fileInputRef.value.value = "";
    return;
  }

  selectedFile.value = file;
  // PDF 는 팝업 내 렌더를 하지 않으므로 objectURL 을 만들지 않는다(누수·높이 초과 방지).
  if (file.type !== PDF_MIME) {
    previewUrl.value = URL.createObjectURL(file);
  }
};

const fnSave = async () => {
  fileMsg.value = "";

  // 1) 입력 검증 — 파일 필수(계약서명은 수정 불허 항목이라 검증 대상 아님).
  if (!selectedFile.value) {
    fileMsg.value = "계약서 파일을 선택해주세요.";
    return;
  }

  // 저장 버튼 disabled 와 동일 조건을 코드에서도 한 번 더 막는다(중복 클릭·비정상 활성 방어).
  //   최종 방어는 서버(400_009 서명자 존재 / 409_002 활성 아님)다.
  if (saving.value || loading.value || precheck.value?.amendable !== true) {
    return;
  }

  // 2) 정정 confirm — 경고 건수가 있으면 2번째 줄을 붙인다(0인 세그먼트는 표기하지 않는다).
  const confirmLines = [`계약서 v${props.contractVer} 를 정정하시겠습니까?`];
  const refSegments = [];
  if (pinnedApprovedCnt.value > 0) {
    refSegments.push(`확정된 승인 ${pinnedApprovedCnt.value}건`);
  }
  if (pendingCnt.value > 0) {
    refSegments.push(`대기 요청 ${pendingCnt.value}건`);
  }
  if (refSegments.length > 0) {
    confirmLines.push(
      `${refSegments.join(" · ")}이 정정된 내용을 적용받습니다.`,
    );
  }
  confirmLines.push("버전은 올라가지 않으며 재서명은 발생하지 않습니다.");

  const ok = await proxy.$confirm(confirmLines.join("\n"));
  if (!ok) return;

  saving.value = true;

  // 3) 정정 — POST /webApi/user07/contract-amend (multipart/form-data).
  //    cmpnyCd 는 서버 JWT 클레임 사용(절대 전달 금지). contractVer 는 정정 대상 지정용이며
  //    서버가 활성 여부를 재검증한다(클라 값이 최종 권위가 아님 — K10).
  //    ★contractNm 은 전송하지 않는다(Q5 — 백엔드가 파라미터 자체를 두지 않는다).
  //    JSON 에러를 반환하는 EP 라 resolveApiErrorMessage 를 쓴다(blob 리졸버 혼용 금지).
  try {
    const formData = new FormData();
    formData.append("siteCd", props.siteCd);
    formData.append("contractVer", String(props.contractVer));
    formData.append("file", selectedFile.value);

    const response = await axios.post(
      "/webApi/user07/contract-amend",
      formData,
    );

    if (response.status === 200) {
      // 서버는 정정 대상 버전을 그대로 반환한다(증가하지 않음) — 응답값 우선, 없으면 props 폴백.
      const keptVer = response.data?.contractVer ?? props.contractVer;
      await proxy.$alert(
        `정정되었습니다. 버전은 v${keptVer} 로 유지되며 재서명은 발생하지 않습니다.`,
      );
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    // 409_002(이미 변경되었습니다) 등 실패 시 팝업을 닫지 않는다 —
    //   관리자가 사유를 읽고 스스로 닫아 새로고침해야 한다.
    const msg = resolveApiErrorMessage(err, "정정 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 520px;
  margin: 0 auto;
}

/* 항상 표시되는 정정 안내 — 등록 팝업의 .reg-guide 와 대칭이지만 톤을 낮춰(중립) 구분한다 */
.amend-guide {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  font-size: var(--btn-font-sm);
  line-height: 1.6;
  color: var(--color-text-muted);
}
.amend-guide strong {
  color: var(--color-text);
}

/* 차단 안내 — 서명자 존재 / precheck 실패 */
.amend-block {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border-left: var(--focus-ring-width) solid var(--color-danger);
  border-radius: var(--btn-radius);
  background: var(--color-bg);
  font-size: var(--btn-font-sm);
  line-height: 1.6;
  color: var(--color-text);
}

/* 경고 — pin 된 승인 / 대기 요청 (Q2: 분리 표기) */
.amend-warn {
  margin: 0;
  padding: 0.5rem 0.75rem 0.5rem 1.6rem;
  border-radius: var(--btn-radius);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  font-size: var(--btn-font-sm);
  line-height: 1.6;
}
.amend-warn li + li {
  margin-top: 0.2rem;
}

.amend-note {
  margin: 0;
  font-size: var(--btn-font-sm);
  line-height: 1.5;
  color: var(--color-text-muted);
}

/* 대상 버전 표기 */
.amend-ver {
  font-weight: 600;
  color: var(--color-text);
}

.form-row-top {
  align-items: flex-start;
}

/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-group .btn {
  flex: 1;
}

.file-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  width: 100%;
}
.file-field__input {
  font-size: var(--btn-font-lg);
}
.file-field__hint {
  margin: 0;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* 미리보기 — 팝업 높이 바운딩 규약: 내부 스크롤로 수용 */
.preview-box {
  max-height: 38vh;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  padding: 0.5rem;
}
.preview-box__img {
  display: block;
  width: 100%;
  height: auto;
}

/* PDF 선택 안내 — 팝업 내 PDF 렌더 없음(파일명·크기 + 확인 경로만) */
.file-note {
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  padding: 0.5rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.file-note__name {
  margin: 0;
  font-size: var(--btn-font-lg);
  font-weight: 600;
  color: var(--color-text);
  word-break: break-all;
}
.file-note__meta {
  margin: 0;
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}
.file-note__guide {
  margin: 0.15rem 0 0;
  font-size: var(--btn-font-sm);
  line-height: 1.5;
  color: var(--color-text-muted);
}
</style>
