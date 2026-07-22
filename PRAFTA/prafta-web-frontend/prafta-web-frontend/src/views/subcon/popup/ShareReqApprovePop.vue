<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>데이터 공유 승인</span>
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

        <div class="form-container">
          <!-- 요청 요약 -->
          <ul class="approve-summary">
            <li><b>요청 회사</b> {{ info.reqCmpnyNm }}</li>
            <li><b>대상 사업장</b> {{ info.siteNm }}</li>
            <li><b>기간</b> {{ info.periodLabel }}</li>
            <li>
              <b>마감 근태만</b>
              {{ info.closedOnlyYn === "Y" ? "예" : "아니오" }}
            </li>
            <li><b>제공 목적</b> {{ info.purpose }}</li>
          </ul>

          <!-- 마감 상태 — 마감분만 요청 + 미마감 존재: 차단 대신 포함/제외 안내(부분 공유 전환 D-1/D-2) -->
          <div
            v-if="info.closedOnlyYn === 'Y' && !info.closedAll"
            class="gate-warn"
          >
            <p class="gate-title">마감분만 포함되어 제공됩니다.</p>
            <p
              v-for="(line, i) in coverageLines"
              :key="i"
              class="gate-body"
            >
              {{ line }}
            </p>
            <p class="gate-body">
              제외된 데이터는 해당 부서/월 마감 후 재요청·재승인 시 포함됩니다.
              실제 제공 건수는 제3자 제공 동의 여부에 따라 더 줄 수 있습니다.
            </p>
          </div>
          <div
            v-else-if="info.closedOnlyYn === 'N' && !info.closedAll"
            class="gate-warn"
          >
            <p class="gate-body">
              미마감 근태가 포함됩니다. 스냅샷에 <b>미마감 포함</b> 표식이 영구
              기록됩니다.
            </p>
          </div>

          <!-- 포함 0건 경고(D-1) — 빈 스냅샷 생성 예고 -->
          <div v-if="info.expectedEmptyYn === 'Y'" class="gate-block">
            <p class="gate-title">포함될 데이터가 0건입니다.</p>
            <p class="gate-body">승인 시 빈 스냅샷이 생성됩니다.</p>
          </div>

          <!-- 릴레이 후보(연동사로부터 수신 보유 중인 자료) -->
          <div v-if="(info.relayCandidates || []).length" class="relay-box">
            <p class="relay-title">함께 제공할 연동사 수신자료 (선택)</p>
            <p class="relay-note">
              선택한 자료는 우리 회사 소속으로 표시되어 함께 전달됩니다(하위
              회사 정보는 포함되지 않습니다).
            </p>
            <label
              v-for="c in info.relayCandidates"
              :key="c.snapshotId"
              class="relay-item"
            >
              <input
                v-model="bundleIds"
                type="checkbox"
                :value="c.snapshotId"
              />
              <span>
                {{ c.periodLabel }} · v{{ c.version }} · {{ c.rowCnt }}건
                <span v-if="c.unclosedIncludedYn === 'Y'" class="mini-badge"
                  >미마감 포함</span
                >
                <span v-if="c.closedPartialYn === 'Y'" class="mini-badge"
                  >부분 포함</span
                >
              </span>
            </label>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              닫기
            </button>
            <button
              class="btn btn-primary"
              :disabled="!canApprove"
              @click="fnApprove"
            >
              승인
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, computed, onMounted, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatCoverageYm } from "@/utils/snapshotCoverage";

const props = defineProps({ shareReqId: [Number, String], onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

// =========================== Ref ===========================
const info = ref({}); // GET share-req-approve-info 응답(요청 요약 + closedAll/unclosedYms + relayCandidates)
const bundleIds = ref([]); // 묶을 수신 스냅샷 ID (서버가 4조건 재검증 — §5-7)

// 승인 중복 클릭 방지 플래그.
const saving = ref(false);

// 승인 가능 여부 — 부분 공유 전환(D-1)으로 마감 미완료 차단 제거. 조회 전(빈 객체)에 버튼이 눌리지 않도록 로드 여부만 본다.
const canApprove = computed(() => !!info.value.shareReqId);

// 월별 커버리지 안내 줄(coverageMonths — 승인 사전정보 API, ym 'YYYY-MM').
//   FULL → 전체 포함 / PARTIAL → 부분 포함(제외 부서·무부서 병기) / NONE → 포함 없음.
//   필드 부재(undefined)·비정형에도 크래시 없이 빈 목록/기본 문구로 렌더한다.
const coverageLines = computed(() => {
  const months = Array.isArray(info.value.coverageMonths) ? info.value.coverageMonths : [];
  return months.map((m) => {
    const ym = formatCoverageYm(m?.ym);
    if (m?.status === "FULL") return `${ym} : 전체 포함`;
    if (m?.status === "NONE") return `${ym} : 포함 없음 (미마감)`;

    // PARTIAL(및 미상 status 방어) — 제외 부서명 나열 + 무부서 근태 제외 병기
    const depts = Array.isArray(m?.excludedDeptNms)
      ? m.excludedDeptNms.filter((d) => typeof d === "string" && d.trim() !== "")
      : [];
    let line = `${ym} : 부분 포함`;
    if (depts.length) line += ` (제외: ${depts.join(", ")} 미마감)`;
    if (m?.orphanUnclosedYn === "Y") line += " · 무부서 근태 제외";
    return line;
  });
});

// 부분 포함 여부(확인 문구 분기용) — coverageMonths 에 PARTIAL/NONE 이 하나라도 있으면 true.
const hasPartialMonth = computed(() =>
  (Array.isArray(info.value.coverageMonths) ? info.value.coverageMonths : []).some(
    (m) => m && (m.status === "PARTIAL" || m.status === "NONE")
  )
);

// =========================== Life Cycle ===========================
// 승인 사전정보 조회 — GET /webApi/subcon03/share-req-approve-info?shareReqId=...
//   마감 상태(closedAll/unclosedYms)와 릴레이 후보를 서버가 판정해 내려준다.
onMounted(async () => {
  try {
    const response = await axios.get("/webApi/subcon03/share-req-approve-info", {
      params: { shareReqId: props.shareReqId },
    });

    if (response.status === 200) {
      info.value = response.data || {};
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
    emit("close");
  }
});

// =========================== Methods ===========================
// 승인 — POST /webApi/subcon03/share-req-approve { shareReqId, bundleSnapshotIds }.
//   승인 시점에 서버가 마감/관계/릴레이 후보를 재검사한 뒤 스냅샷을 생성한다(단일 트랜잭션).
const fnApprove = async () => {
  // 확인 문구 3분기(D-1) — 0건 > 부분 포함 > 기존 순.
  let confirmMsg = "승인 시 해당 기간 근태가 요청 회사로 복제됩니다. 진행할까요?";
  if (info.value.expectedEmptyYn === "Y") {
    confirmMsg = "포함될 데이터가 0건입니다. 그래도 승인하여 빈 스냅샷을 생성할까요?";
  } else if (hasPartialMonth.value) {
    confirmMsg = "마감된 데이터만 포함되어 제공됩니다. 진행할까요?";
  }
  const ok = await proxy.$confirm(confirmMsg);
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon03/share-req-approve", {
      shareReqId: props.shareReqId,
      bundleSnapshotIds: bundleIds.value,
    });

    if (response.status === 200) {
      const rowCnt = response.data?.rowCnt ?? 0;
      await proxy.$alert(`승인되었습니다. (제공 ${rowCnt}건)`);
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "승인 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.approve-summary {
  margin: 0 0 var(--space-md, 0.75rem);
  padding: 0;
  list-style: none;
  line-height: 1.8;
}
.approve-summary b {
  display: inline-block;
  width: 90px;
  color: var(--color-text-muted, #6b7280);
  font-weight: 500;
}
.gate-block,
.gate-warn,
.relay-box {
  margin-top: var(--space-md, 0.75rem);
  padding: var(--space-sm, 0.5rem) var(--space-md, 0.75rem);
  border-radius: var(--btn-radius, 8px);
}
.gate-block {
  background: var(--color-danger-bg, #fee2e2);
}
.gate-warn {
  background: var(--color-warning-bg, #fef3c7);
}
.relay-box {
  border: 1px solid var(--color-border, #e5e7eb);
}
.gate-title {
  margin: 0 0 0.25rem;
  font-weight: 600;
  color: var(--color-danger, #dc2626);
}
/* 포함/제외 안내 블록(노란 톤) 제목 — 차단(빨간) 제목색 대신 경고 톤 재사용 */
.gate-warn .gate-title {
  color: var(--color-warning-text, #b45309);
}
.gate-body {
  margin: 0;
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}
.relay-title {
  margin: 0 0 0.25rem;
  font-weight: 600;
}
.relay-note {
  margin: 0 0 var(--space-sm, 0.5rem);
  font-size: var(--btn-font-sm, 12px);
  color: var(--color-text-muted, #6b7280);
}
.relay-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 0.5rem);
  padding: 0.15rem 0;
}
.mini-badge {
  margin-left: 0.35rem;
  padding: 0.05rem 0.4rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  font-size: var(--btn-font-sm, 11px);
}
</style>
