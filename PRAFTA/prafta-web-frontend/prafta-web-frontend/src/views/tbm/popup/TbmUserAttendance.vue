<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>사용자별 TBM 이수 이력</span>
          <div class="header-actions">
            <!-- 프린트 버튼 제거(2026-08-30 요청) — 닫기만 유지 -->
            <button class="icon-button no-print" @click="$emit('close')">
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
          <!-- 사용자 정보 -->
          <div class="user-meta">
            <span class="meta-name">{{ user.userNm || "-" }}</span>
            <span
              class="type-badge"
              :class="
                user.userTypeCd === 'DAILY' ? 'type-daily' : 'type-regular'
              "
            >
              {{ user.userTypeCd === "DAILY" ? "일용직" : "정규직" }}
            </span>
            <span class="meta-item"
              >사업장: {{ user.siteNm || user.siteCd || "-" }}</span
            >
            <span v-if="user.userTypeCd === 'DAILY'" class="meta-item">
              끝4자리: ****{{ user.mblNoLast4 || "" }}
            </span>
            <span v-else class="meta-item">소속: {{ user.deptNm || "-" }}</span>
          </div>

          <!-- 통계 요약 -->
          <div class="stat-strip">
            <div class="stat-card">
              <span class="stat-label">총 참여</span>
              <span class="stat-value">{{ summary.totalCount }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">이수</span>
              <span class="stat-value">{{ summary.completedCount }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">미이수</span>
              <span class="stat-value stat-value-danger">
                {{ summary.notCompletedCount }}
              </span>
            </div>
            <div class="stat-card">
              <span class="stat-label">이수율</span>
              <span class="stat-value">{{ summary.completionRate }}%</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">평균 참여시간</span>
              <span class="stat-value">
                {{
                  summary.avgDurationMin != null
                    ? summary.avgDurationMin + "분"
                    : "-"
                }}
              </span>
            </div>
          </div>

          <!-- 기간 필터 -->
          <div class="att-filter no-print">
            <label>종료일(시작)</label>
            <CalendarSrch v-model="startDate" />
            <label>종료일(종료)</label>
            <CalendarSrch v-model="endDate" />
            <label>이수</label>
            <select v-model.trim="completionStatusCd">
              <option value="">전체</option>
              <option value="COMPLETED">이수</option>
              <option value="NOT_COMPLETED">미이수</option>
            </select>
            <button class="btn btn-primary btn-sm" @click="fnReload">
              조회
            </button>
          </div>

          <!-- 이력 그리드 -->
          <div class="table-box" style="--box-h: auto; --box-ox: auto">
            <table class="data-grid w-full text-sm text-left">
              <thead>
                <tr>
                  <th style="width: 4%; text-align: center">No</th>
                  <th style="width: 28%">교육 제목</th>
                  <!-- PRAFTA-SUBCON-T5: 타사(연동) 세션은 사업장 대신 개최 회사를 표시한다.
                       서버가 타사 세션 행의 siteNm 을 비우고 hostCmpnyNm 을 내려준다(인접 차수 가시성). -->
                  <th style="width: 14%">사업장 / 개최 회사</th>
                  <th style="width: 12%">일자</th>
                  <th style="width: 12%">입실</th>
                  <th style="width: 12%">종료</th>
                  <th style="width: 8%">위험성평가</th>
                  <th style="width: 8%; text-align: center">이수</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!attendances || attendances.length === 0">
                  <tr>
                    <td colspan="8" class="edu-grid-empty">
                      이수 이력이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(row, idx) in attendances" :key="row.attendanceCd">
                    <td style="text-align: center">
                      {{ (page - 1) * pageSize + idx + 1 }}
                    </td>
                    <td>{{ row.sessionTitle }}</td>
                    <td>
                      <template v-if="row.hostCmpnyNm">
                        <span class="host-cmpny">{{ row.hostCmpnyNm }}</span>
                      </template>
                      <template v-else>
                        {{ row.siteNm || row.siteCd || "-" }}
                      </template>
                    </td>
                    <td>{{ row.sessionDate || "-" }}</td>
                    <td>{{ row.entryAt || "-" }}</td>
                    <td>{{ row.exitAt || "-" }}</td>
                    <td style="text-align: center">{{ row.riskCount }}건</td>
                    <td style="text-align: center">
                      <span
                        class="comp-mark"
                        :class="
                          row.completionStatusCd === 'COMPLETED'
                            ? 'comp-ok'
                            : row.completionStatusCd === 'NOT_COMPLETED'
                              ? 'comp-no'
                              : 'comp-none'
                        "
                      >
                        {{
                          row.completionStatusNm ||
                          compMark(row.completionStatusCd)
                        }}
                      </span>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <!-- 페이징 -->
          <div v-if="totalCount > 0" class="pager no-print">
            <button
              class="btn btn-second btn-sm"
              :disabled="page <= 1"
              @click="fnGoPage(page - 1)"
            >
              이전
            </button>
            <span class="pager-info">
              {{ page }} / {{ totalPages }} (총 {{ totalCount }}건)
            </span>
            <button
              class="btn btn-second btn-sm"
              :disabled="page >= totalPages"
              @click="fnGoPage(page + 1)"
            >
              다음
            </button>
          </div>
        </div>

        <div class="modal-footer no-print">
          <div class="btn-group">
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
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

const { proxy } = getCurrentInstance();

const props = defineProps({
  userCd_p: String,
  userTypeCd_p: { type: String, default: "REGULAR" }, // REGULAR / DAILY
});
defineEmits(["close"]);

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 1.7,
  verticalRatio: 5,
});

const user = reactive({
  userCd: "",
  userTypeCd: props.userTypeCd_p,
  userNm: "",
  siteCd: "",
  siteNm: "",
  deptNm: "",
  mblNoLast4: "",
});
const attendances = ref([]);
const summary = reactive({
  totalCount: 0,
  completedCount: 0,
  notCompletedCount: 0,
  completionRate: 0,
  avgDurationMin: null,
});

// 필터
const startDate = ref("");
const endDate = ref("");
const completionStatusCd = ref("");

// 페이징
const page = ref(1);
const pageSize = ref(20);
const totalCount = ref(0);

const totalPages = computed(() => {
  const pages = Math.ceil(totalCount.value / pageSize.value);
  return pages < 1 ? 1 : pages;
});

// 일용직/정규직 엔드포인트 분기
const apiUrl = computed(() =>
  props.userTypeCd_p === "DAILY"
    ? "/webApi/tbm04/daily-user-attendances"
    : "/webApi/tbm04/user-attendances"
);

onMounted(async () => {
  await fnSearch();
});

const fnSearch = async () => {
  try {
    const response = await axios.get(apiUrl.value, {
      params: {
        userCd: props.userCd_p,
        startDate: startDate.value,
        endDate: endDate.value,
        completionStatusCd: completionStatusCd.value,
        page: page.value,
        pageSize: pageSize.value,
      },
    });

    if (response.status === 200) {
      const data = response.data || {};
      const u = data.user || {};
      user.userCd = u.userCd || "";
      user.userTypeCd = u.userTypeCd || props.userTypeCd_p;
      user.userNm = u.userNm || "";
      user.siteCd = u.siteCd || "";
      user.siteNm = u.siteNm || "";
      user.deptNm = u.deptNm || "";
      user.mblNoLast4 = u.mblNoLast4 || "";

      attendances.value = data.attendances || [];
      totalCount.value = data.totalCount || 0;

      const s = data.summary || {};
      summary.totalCount = s.totalCount || 0;
      summary.completedCount = s.completedCount || 0;
      summary.notCompletedCount = s.notCompletedCount || 0;
      summary.completionRate = s.completionRate || 0;
      summary.avgDurationMin =
        s.avgDurationMin != null ? s.avgDurationMin : null;
    }
  } catch (err) {
    attendances.value = [];
    totalCount.value = 0;
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnReload = () => {
  page.value = 1;
  fnSearch();
};

const fnGoPage = (target) => {
  if (target < 1 || target > totalPages.value) return;
  page.value = target;
  fnSearch();
};

const compMark = (code) => {
  if (code === "COMPLETED") return "이수";
  if (code === "NOT_COMPLETED") return "미이수";
  return "-";
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

.user-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  padding-bottom: 0.75rem;
  margin-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.meta-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

.meta-item {
  font-size: var(--btn-font);
  color: var(--color-text);
}

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
}

.type-regular {
  background: var(--color-bg);
  color: var(--color-text-strong);
  border: 1px solid var(--color-border-strong);
}

.type-daily {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.stat-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  padding: 0.5rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--card-radius);
  background: var(--color-surface);
}

.stat-label {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.stat-value {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-strong);
}

.stat-value-danger {
  color: var(--color-danger);
}

.att-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.att-filter label {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

/* 개최 회사(PRAFTA-SUBCON-T5): 타사 연동 세션 표시 — 사업장명 대신 노출 */
.host-cmpny {
  color: var(--color-text-muted);
}

.comp-mark {
  font-weight: 700;
}

.comp-ok {
  color: var(--color-primary);
}

.comp-no {
  color: var(--color-danger);
}

.comp-none {
  color: var(--color-text-muted);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.pager-info {
  font-size: var(--btn-font);
  color: var(--color-text-muted);
}

.btn-sm {
  height: var(--btn-height-sm);
  padding: 0 var(--btn-padding-sm);
  font-size: var(--btn-font-sm);
}

@media print {
  .no-print {
    display: none !important;
  }

  .modal-overlay {
    position: static;
    background: none;
  }

  .modal-content-wide {
    position: static !important;
    width: 100% !important;
    box-shadow: none !important;
  }

  .detail-wrapper {
    height: auto;
    overflow: visible;
  }
}
</style>
