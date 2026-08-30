<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <!-- 드래그되는 영역 -->
      <div
        class="modal-content dch-modal"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 1. Title 영역 (여기서만 드래그 가능) -->
        <div class="modal-header" @mousedown="startDrag">
          <span>계약이력 — {{ userNm_p }}</span>
          <button class="icon-button" @click="$emit('close')">✕</button>
        </div>

        <div class="viewBody dch-body">
          <div v-if="loading" class="dch-state">조회 중…</div>
          <template v-else>
            <!-- ═══ 섹션 1: 계약서 서명 이력 ═══ -->
            <div class="dch-section-title">계약서 서명 이력</div>
            <div class="dch-table-wrap">
              <table class="dch-table">
                <thead>
                  <tr>
                    <th style="width: 40px">No</th>
                    <th style="width: 150px">서명일시</th>
                    <th>계약서</th>
                    <th style="width: 100px">최초근로일</th>
                    <th style="width: 90px">서명본</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="signList.length === 0">
                    <td colspan="5" class="dch-empty">서명 이력이 없습니다.</td>
                  </tr>
                  <tr v-for="(s, idx) in signList" :key="s.signId">
                    <td class="t-center">{{ idx + 1 }}</td>
                    <td>{{ s.signDtime }}</td>
                    <td>{{ contractLabel(s) }}</td>
                    <td class="t-center">{{ fmtYmd(s.firstWorkDate) }}</td>
                    <td class="t-center">
                      <button class="btn btn-sm btn-primary" @click="fnViewSign(s)">
                        열람
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- ═══ 섹션 2: 입장 승인/로그인 이력 ═══
                 계약서 없이 로그인된 경우(승인 시점 활성 계약서 미등록 = pin 0)도 여기서 확인한다. -->
            <div class="dch-section-title">입장 승인 / 로그인 이력</div>
            <div class="dch-table-wrap">
              <table class="dch-table">
                <thead>
                  <tr>
                    <th style="width: 40px">No</th>
                    <th style="width: 80px">요청유형</th>
                    <th style="width: 70px">상태</th>
                    <th style="width: 150px">요청일시</th>
                    <th style="width: 150px">로그인일시</th>
                    <th>계약서 서명</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="entryList.length === 0">
                    <td colspan="6" class="dch-empty">
                      입장 승인 이력이 없습니다.
                    </td>
                  </tr>
                  <tr v-for="(e, idx) in entryList" :key="e.reqId">
                    <td class="t-center">{{ idx + 1 }}</td>
                    <td class="t-center">{{ reqTypeLabel(e.reqType) }}</td>
                    <td class="t-center">{{ reqStatusLabel(e.reqStatus) }}</td>
                    <td>{{ e.reqDtime }}</td>
                    <td>{{ e.consumeDtime ?? "-" }}</td>
                    <td>
                      <span
                        :class="['dch-badge', signStateOf(e).cls]"
                      >
                        {{ signStateOf(e).label }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <p class="dch-note">
              ※ '계약서 미등록'은 승인 시점에 사업장 활성 계약서가 없어 서명 없이
              입장(로그인)한 건입니다.
            </p>
          </template>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-secondary" @click="$emit('close')">
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance, defineProps } from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import {
  resolveApiErrorMessage,
  resolveBlobApiErrorMessage,
} from "@/utils/apiError";

defineOptions({ name: "DailyContractHistPop" });

const props = defineProps({
  // 대상 일용직 사용자코드(TB_DAILY_USER.USER_CD)
  userCd_p: { type: String, required: true },
  // 헤더 표시용 사용자명(목록 행 값 그대로)
  userNm_p: { type: String, default: "" },
});
const { userCd_p, userNm_p } = props;

defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const loading = ref(false);
const signList = ref([]);
const entryList = ref([]);

// 요청유형 [SYS081] / 요청상태 [SYS082] — TB_DAILY_ENTRY_REQUEST DDL 코멘트 기준.
const REQ_TYPE_LABELS = { "01": "신규가입", "02": "재입장" };
const REQ_STATUS_LABELS = {
  "01": "대기",
  "02": "승인",
  "03": "거부",
  "04": "만료",
  "05": "소진",
};
const reqTypeLabel = (cd) => REQ_TYPE_LABELS[cd] ?? cd ?? "-";
const reqStatusLabel = (cd) => REQ_STATUS_LABELS[cd] ?? cd ?? "-";

// yyyyMMdd → yyyy-MM-dd
const fmtYmd = (ymd) => {
  const s = String(ymd ?? "");
  if (s.length !== 8) return s || "-";
  return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
};

// 서명 이력 행 계약서 라벨 — 계약서명(v버전). 이름 없으면 버전만.
const contractLabel = (s) => {
  const ver = s.contractVer != null ? `v${s.contractVer}` : "";
  if (s.contractNm) return ver ? `${s.contractNm} (${ver})` : s.contractNm;
  return ver || "-";
};

// 입장 이력 행의 계약서 서명 상태 판정.
//   - signYn='Y'                              → 이번 입장에서 서명함
//   - 로그인(소진) + pin=0 + 서명 없음         → 계약서 미등록 상태 로그인(요구사항 핵심)
//   - 로그인(소진) + pin>0 + 서명 없음         → 기존 서명 유효(같은 버전 재입장 — 재서명 불요)
//   - 로그인(소진) + pin=null(레거시) + 없음   → 확인 불가(pin 도입 전)
//   - 미로그인(대기/거부/만료)                 → '-'
const signStateOf = (e) => {
  if (e.signYn === "Y") {
    return { label: `서명함${e.contractVer ? ` (v${e.contractVer})` : ""}`, cls: "b-signed" };
  }
  if (!e.consumeDtime) {
    return { label: "-", cls: "b-none" };
  }
  if (e.contractVer === 0) {
    return { label: "계약서 미등록", cls: "b-nocontract" };
  }
  if (e.contractVer == null) {
    return { label: "확인 불가(구 데이터)", cls: "b-none" };
  }
  return { label: `기존 서명 유효 (v${e.contractVer})`, cls: "b-prior" };
};

// 이력 조회 — GET /webApi/user05/daily-contract-history
const fnLoad = async () => {
  loading.value = true;
  try {
    const r = await axios.get("/webApi/user05/daily-contract-history", {
      params: { userCd: userCd_p },
    });
    signList.value = r.data?.signList ?? [];
    entryList.value = r.data?.entryList ?? [];
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "계약이력 조회 중 오류가 발생했습니다.")
    );
  } finally {
    loading.value = false;
  }
};

// 서명본 열람 — User_08 fnViewSign 미러(스트림 EP 재사용, 파일 경로 비노출).
const fnViewSign = async (row) => {
  try {
    const response = await axios.get("/webApi/user08/contract-sign-image", {
      params: { signId: row.signId },
      responseType: "blob",
    });
    const url = URL.createObjectURL(response.data);
    const win = window.open(url, "_blank");
    if (!win) {
      URL.revokeObjectURL(url);
      await proxy.$alert(
        "브라우저가 새 창을 차단했습니다.\n팝업 차단을 해제한 뒤 다시 시도해주세요."
      );
      return;
    }
    // 새 탭 로드가 끝난 뒤 objectURL 해제(즉시 해제 시 탭에서 로드 실패).
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    // 스트림 EP 라 에러 본문도 Blob 으로 도착한다 — blob 전용 리졸버로 서버 사유를 노출.
    const msg = await resolveBlobApiErrorMessage(
      err,
      "서명본 열람 중 오류가 발생했습니다."
    );
    await proxy.$alert(msg);
  }
};

onMounted(fnLoad);
</script>

<style scoped>
.dch-modal {
  width: 720px;
  max-width: 92vw;
  max-height: 84vh;
  /* 기본 modal-content 의 20px 패딩 제거 → 헤더/본문/푸터가 박스 끝에 밀착.
     overflow:hidden 으로 헤더/푸터 모서리를 16px 라운드에 맞춰 클립. */
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dch-body {
  padding: 0.75rem 1rem;
  max-height: 65vh;
  overflow-y: auto;
}

.dch-state {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted, #4b5563);
  font-size: 0.8125rem;
}

.dch-section-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-strong, #111827);
  margin: 0.5rem 0 0.375rem;
}

.dch-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 8px);
  margin-bottom: 0.75rem;
}

.dch-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.dch-table th {
  background: var(--color-bg, #f9fafb);
  color: var(--color-text-muted, #4b5563);
  font-weight: 600;
  text-align: center;
  padding: 0.375rem 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  white-space: nowrap;
}

.dch-table td {
  padding: 0.375rem 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
  word-break: keep-all;
}

.dch-table tbody tr:last-child td {
  border-bottom: none;
}

.t-center {
  text-align: center;
}

.dch-empty {
  text-align: center;
  color: var(--color-text-muted, #4b5563);
  padding: 0.75rem;
}

/* 서명 상태 배지 */
.dch-badge {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}
.b-signed {
  background: #d1fae5;
  color: #065f46;
}
.b-nocontract {
  background: #fee2e2;
  color: #991b1b;
}
.b-prior {
  background: #e0e7ff;
  color: #3730a3;
}
.b-none {
  background: transparent;
  color: var(--color-text-muted, #4b5563);
}

.dch-note {
  margin: 0;
  font-size: 0.6875rem;
  line-height: 1.5;
  color: var(--color-text-muted, #4b5563);
  word-break: keep-all;
}
</style>
