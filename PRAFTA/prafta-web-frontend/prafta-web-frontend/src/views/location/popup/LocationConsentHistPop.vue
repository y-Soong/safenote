<!--
  LocationConsentHistPop.vue — 위치정보 동의 이력 팝업 (위치정보 동의철회·중지 S5)

  - 백엔드: GET /webApi/location01/consent-histories?siteCd=&userCd=
  - 전이 이력(상태가 바뀌었다)과 파기 이력(그래서 무엇을 지웠다)을 한 화면에 함께 둔다.
    책임 추궁은 둘을 대조할 수 있을 때 성립한다.
  - ★파기 이력에는 좌표가 없다(테이블 자체에 담지 않는다). 남는 것은 건수·기간·사유·주체뿐이다.
  - 조회 전용. 이 팝업에서 상태를 바꾸는 동작은 제공하지 않는다.
  - 레이아웃: 표준 팝업 셸(modal-overlay prafta-modal-popup → modal-content-normal →
    modal-header / modal-body / modal-footer)을 따른다. 대상자 메타 행은 SchInfoHistPop 의
    hist-info-row, 섹션 표는 목록 화면과 같은 subtitle-pane + table-box + data-grid,
    상태 표시는 Location_01 과 동일한 .status-badge 를 쓴다.
-->
<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-show="true"
        ref="overlayRef"
        class="modal-overlay prafta-modal-popup"
        tabindex="-1"
        @click.self="onClose"
        @keydown.esc="onClose"
      >
        <div
          class="modal-content-normal lch-pop"
          role="dialog"
          aria-modal="true"
        >
          <div class="modal-header">
            <span>위치정보 동의 이력</span>
            <button
              class="icon-button"
              type="button"
              aria-label="닫기"
              @click="onClose"
            >
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

          <div class="modal-body lch-body">
            <!-- 대상자 메타 (SchInfoHistPop hist-info-row 패턴) -->
            <div class="hist-info-row">
              <span class="hist-label">대상자</span>
              <span class="hist-value">{{ userNm_p || "-" }}</span>
            </div>

            <p v-if="loading" class="lch-state">불러오는 중...</p>

            <template v-else>
              <!-- 동의 전이 이력 -->
              <div class="table-wrapper subtitle-pane">
                <div class="subtitle-row">
                  <div class="subtitle">
                    <span class="subtitle-icon" aria-hidden="true">
                      <svg viewBox="0 0 24 24" width="18" height="18">
                        <path d="M4 4h16v4H4zM4 10h10v10H4z" />
                      </svg>
                    </span>
                    <span class="subtitle-text">동의 상태 변경 이력</span>
                  </div>
                </div>
                <div class="table-box" style="--box-h: auto; --box-ox: auto">
                  <table class="data-grid w-full table-fixed text-sm text-left">
                    <thead>
                      <tr>
                        <th style="text-align: center; width: 22%">일시</th>
                        <th style="text-align: center; width: 32%">변경</th>
                        <th style="text-align: center; width: 12%">
                          약관 버전
                        </th>
                        <th style="text-align: center; width: 18%">경로</th>
                        <th style="text-align: center; width: 16%">수행자</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="consentHistList.length === 0">
                        <td colspan="5" class="edu-grid-empty">
                          변경 이력이 없습니다.
                        </td>
                      </tr>
                      <tr v-for="h in consentHistList" :key="h.histId">
                        <td style="text-align: center">{{ h.actionDtime }}</td>
                        <td style="text-align: center">
                          <span
                            class="status-badge"
                            :class="stateOf(h.beforeState, h.beforeAgrYn).cls"
                          >
                            {{ stateOf(h.beforeState, h.beforeAgrYn).label }}
                          </span>
                          <span class="lch-arrow" aria-hidden="true">→</span>
                          <span
                            class="status-badge"
                            :class="stateOf(h.afterState, h.afterAgrYn).cls"
                          >
                            {{ stateOf(h.afterState, h.afterAgrYn).label }}
                          </span>
                        </td>
                        <td style="text-align: center">
                          v{{ h.termsVersion }}
                        </td>
                        <td style="text-align: center">
                          {{ sourceLabel(h.agrSource) }}
                        </td>
                        <td style="text-align: center">{{ actorLabel(h) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- 파기 이력 -->
              <div class="table-wrapper subtitle-pane">
                <div class="subtitle-row">
                  <div class="subtitle">
                    <span class="subtitle-icon" aria-hidden="true">
                      <svg viewBox="0 0 24 24" width="18" height="18">
                        <path d="M4 4h16v4H4zM4 10h10v10H4z" />
                      </svg>
                    </span>
                    <span class="subtitle-text">위치정보 파기 이력</span>
                  </div>
                </div>
                <p class="hint">
                  파기 이력에는 좌표가 저장되지 않습니다. 언제·무엇을·몇 건
                  지웠는지만 남습니다.
                </p>
                <div class="table-box" style="--box-h: auto; --box-ox: auto">
                  <table class="data-grid w-full table-fixed text-sm text-left">
                    <thead>
                      <tr>
                        <th style="text-align: center; width: 20%">일시</th>
                        <th style="text-align: center; width: 14%">사유</th>
                        <th style="text-align: center; width: 9%">출퇴근</th>
                        <th style="text-align: center; width: 9%">TBM 입실</th>
                        <th style="text-align: center; width: 9%">TBM 개설</th>
                        <th style="text-align: center; width: 25%">
                          대상 기간
                        </th>
                        <th style="text-align: center; width: 14%">수행자</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="purgeHistList.length === 0">
                        <td colspan="7" class="edu-grid-empty">
                          파기 이력이 없습니다.
                        </td>
                      </tr>
                      <tr v-for="p in purgeHistList" :key="p.purgeId">
                        <td style="text-align: center">{{ p.actionDtime }}</td>
                        <td style="text-align: center">
                          {{ reasonLabel(p.purgeReasonCd) }}
                        </td>
                        <td style="text-align: right">{{ p.attdGpsRows }}</td>
                        <td style="text-align: right">
                          {{ p.tbmAttendanceRows }}
                        </td>
                        <td style="text-align: right">
                          {{ p.tbmSessionRows }}
                        </td>
                        <td style="text-align: center">{{ periodLabel(p) }}</td>
                        <td style="text-align: center">{{ actorLabel(p) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>
          </div>

          <div class="modal-footer">
            <button class="btn btn-second" type="button" @click="onClose">
              닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, nextTick } from "vue";
import axios from "@/api/axios";

const props = defineProps({
  siteCd_p: { type: String, default: "" },
  userCd_p: { type: String, default: "" },
  userNm_p: { type: String, default: "" },
});

const emit = defineEmits(["close"]);

const consentHistList = ref([]);
const purgeHistList = ref([]);
const loading = ref(false);
const overlayRef = ref(null);

const onClose = () => emit("close");

// 4-state 라벨/배지 클래스 — Location_01 과 동일한 표기·색을 쓴다.
const STATE_VIEW = {
  AGREED: { label: "동의", cls: "is-agreed" },
  SUSPENDED: { label: "일시 중지", cls: "is-suspended" },
  PENDING_REAGREE: { label: "재동의 필요", cls: "is-pending" },
  WITHDRAWN: { label: "동의 철회", cls: "is-withdrawn" },
};

// 상태 컬럼 도입 이전 행은 BEFORE/AFTER_STATE 가 NULL 이다 —
//   그 경우 AGR_YN 으로 표시한다(이력을 소급 조작하지 않는다).
const stateOf = (state, agrYn) => {
  if (state) return STATE_VIEW[state] || { label: state, cls: "" };
  if (agrYn === "Y") return STATE_VIEW.AGREED;
  if (agrYn === "N") return { label: "미동의", cls: "is-pending" };
  return { label: "-", cls: "" };
};

const SOURCE_LABEL = {
  GATE: "로그인 게이트",
  MYPAGE: "마이페이지",
  JOIN: "가입",
  SYSTEM: "시스템(약관 개정)",
  ADMIN: "관리자",
};
const sourceLabel = (src) => SOURCE_LABEL[src] || src || "-";

// 수행자 표기 — "사용자명(사용자ID)".
//   ★계정이 삭제됐거나 시스템(약관 버전업 자동 전이)·배치 실행분은 이름/ID 가 없다 →
//     사용자코드를 그대로 보인다(이력을 임의로 가공하지 않는다).
const actorLabel = (row) => {
  const nm = row?.actorUserNm || "";
  const id = row?.actorUserId || "";
  if (nm && id) return `${nm}(${id})`;
  return nm || id || row?.actorUserCd || "-";
};

const reasonLabel = (cd) =>
  cd === "RETENTION"
    ? "보존기간 경과"
    : cd === "WITHDRAW"
      ? "동의 철회"
      : cd || "-";

const fmtYmd = (ymd) =>
  ymd && ymd.length === 8
    ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
    : ymd || "-";
const periodLabel = (p) =>
  p.oldestCollected
    ? `${fmtYmd(p.oldestCollected)} ~ ${fmtYmd(p.latestCollected)}`
    : "-";

const fnLoad = async () => {
  if (!props.userCd_p) return;
  loading.value = true;
  try {
    const response = await axios.get("/webApi/location01/consent-histories", {
      params: { siteCd: props.siteCd_p, userCd: props.userCd_p },
    });
    if (response.status === 200) {
      consentHistList.value = response.data?.consentHistList ?? [];
      purgeHistList.value = response.data?.purgeHistList ?? [];
    }
  } catch (err) {
    console.error("[LocationConsentHistPop] 이력 조회 실패", err);
    consentHistList.value = [];
    purgeHistList.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await nextTick();
  overlayRef.value?.focus();
  fnLoad();
});
</script>

<style scoped>
/* 팝업 폭 — 표준 modal-content-normal(최대 1000px) 안에서 내용 폭에 맞춘다 */
.lch-pop {
  width: min(92vw, 60rem);
}

/* 본문: 메타 행 → 섹션 표 2개를 세로로 쌓는다 (SchInfoHistPop modal-body-hist 패턴) */
.lch-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

/* 대상자 메타 행 (SchInfoHistPop hist-info-row 차용) */
.hist-info-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.hist-label {
  font-weight: 500;
  color: var(--color-text-strong, #111827);
  min-width: 5rem;
}
.hist-value {
  color: var(--color-text, #374151);
}

/* 로딩 상태 문구 */
.lch-state {
  margin: 0;
  padding: var(--card-padding, 20px);
  text-align: center;
  color: var(--color-text-muted, #4b5563);
}

/* 안내 문구 (소제목 바 아래) */
.hint {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--color-text-muted, #4b5563);
  word-break: keep-all;
}

/* 전이 표기: [이전 배지] → [이후 배지] */
.lch-arrow {
  margin: 0 0.35rem;
  color: var(--color-text-muted, #4b5563);
}

/* 상태 배지 — Location_01 과 동일 */
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
