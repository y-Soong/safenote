<template>
  <div class="viewComm">
    <!-- 탭 (재기획서 §5.3) — Attd_01 형태: 탭바를 화면명(ViewHeader) 위에 둔다. -->
    <div class="ra-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="ra-tab"
        :class="{ active: activeTab === t.key }"
        @click="fnSelectTab(t.key)"
      >
        {{ t.label }}
        <span v-if="tabCount(t.key) > 0" class="ra-tab-badge">
          {{ tabCount(t.key) }}
        </span>
      </button>
      <!-- 내 처리 이력: 현재 탭 유형에서 내가 승인/반려한 이력 팝업(읽기 전용). 기존 UI 무수정 - 버튼만 추가. -->
      <button
        type="button"
        class="ra-history-btn"
        title="현재 탭에서 내가 승인/반려 처리한 이력을 봅니다"
        @click="showHistoryPop = true"
      >
        내 처리 이력
      </button>
    </div>

    <!-- 접수함다중사업장권한확장-003: 접근 가능 사업장이 2개 이상일 때만 자체 노출(무회귀). 연차 탭 제외. -->
    <ReqInboxSiteFilter
      v-if="activeTab !== 'leave'"
      v-model="selectedSiteCd"
      :accessible-sites="accessibleSites"
      :loading="accessibleSitesLoading"
    />

    <ViewHeader
      class="commViewHeader"
      :title="props.title || '요청 승인 관리'"
      :buttons="localButtons"
      @search="fnSearch"
    />

    <div class="viewBody ra-body">
      <!-- 연차 탭: 2분할 (접수함 / 상세) -->
      <template v-if="activeTab === 'leave'">
        <!-- 접수함: 좌측 컬럼을 두 소섹션으로 분리(B안) — 기존 결재 대기(위, 무수정) + 신규 연차 변경 대기(아래, 신규) -->
        <section class="ra-inbox">
          <!-- 기존 "내 결재 대기" 블록 — wrapper 만 추가, 내부 마크업/로직 100% 동일 -->
          <div class="ra-inbox__block">
            <div class="ra-inbox__head">
              내 결재 대기 ({{ approvalList.length }})
            </div>
            <div class="ra-list">
              <div v-if="approvalList.length === 0" class="ra-empty">
                대기 중인 연차 결재가 없습니다.
              </div>
              <!-- prafta-leavemulti: 단건은 종전 그대로, 기간신청 묶음은 1행으로 접어 표시한다. -->
              <template v-for="entry in approvalEntries" :key="entry.key">
                <!-- 단건 (leaveGroupId 없음) — 마크업/동작 종전과 동일 -->
                <div
                  v-if="entry.kind === 'single'"
                  class="ra-row"
                  :class="{
                    selected: selected && selected.reqId === entry.row.reqId,
                  }"
                  @click="fnSelect(entry.row)"
                >
                  <div class="ra-row__main">
                    <span class="ra-row__name">{{
                      entry.row.requesterUserNm
                    }}</span>
                    <span class="ra-row__dept">{{ entry.row.nodeNm || "-" }}</span>
                    <span v-if="entry.row.selfYn === 'Y'" class="ra-chip self"
                      >본인</span
                    >
                    <!-- 가불표시-03: 가불(미래 연차 당겨쓰기) 포함 요청 배지 — borrowDays > 0 일 때만 -->
                    <span
                      v-if="Number(entry.row.borrowDays) > 0"
                      class="ra-chip borrow"
                      >가불</span
                    >
                  </div>
                  <div class="ra-row__sub">
                    {{ fmtDate(entry.row.workYmd) }} ·
                    {{ entry.row.unitNm || entry.row.leaveType }} ·
                    {{ Number(entry.row.leaveDays) }}일
                  </div>
                </div>

                <!-- 묶음 1행 — 클릭하면 일괄 처리, ▸ 로 펼치면 개별 처리 -->
                <div v-else class="ra-group">
                  <div
                    class="ra-row ra-row--group"
                    :class="{
                      selected:
                        selectedGroup && selectedGroup.groupId === entry.groupId,
                    }"
                    @click="fnSelectGroup(entry)"
                  >
                    <div class="ra-row__main">
                      <button
                        type="button"
                        class="ra-group__toggle"
                        :title="
                          isGroupExpanded(entry.groupId)
                            ? '접기'
                            : '펼쳐서 개별 처리'
                        "
                        @click.stop="fnToggleGroup(entry.groupId)"
                      >
                        {{ isGroupExpanded(entry.groupId) ? "▾" : "▸" }}
                      </button>
                      <span class="ra-row__name">{{ entry.requesterUserNm }}</span>
                      <span class="ra-row__dept">{{ entry.nodeNm || "-" }}</span>
                      <span class="ra-chip group">기간 {{ entry.rows.length }}건</span>
                      <span v-if="entry.anySelf" class="ra-chip self">본인</span>
                      <span v-if="entry.anyBorrow" class="ra-chip borrow">가불</span>
                    </div>
                    <div class="ra-row__sub">
                      {{ fmtDate(entry.fromYmd) }} ~ {{ fmtDate(entry.toYmd) }} ·
                      총 {{ entry.days }}일
                    </div>
                  </div>

                  <!-- 펼침: 개별 건은 단건과 동일하게 선택·처리된다(정책 ④ 개별 예외) -->
                  <div v-if="isGroupExpanded(entry.groupId)" class="ra-group__items">
                    <div
                      v-for="row in entry.rows"
                      :key="row.reqId + '-' + row.approvalStep"
                      class="ra-row ra-row--child"
                      :class="{ selected: selected && selected.reqId === row.reqId }"
                      @click="fnSelect(row)"
                    >
                      <div class="ra-row__sub">
                        {{ fmtDate(row.workYmd) }} ·
                        {{ row.unitNm || row.leaveType }} ·
                        {{ Number(row.leaveDays) }}일
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- 신규: 연차 변경(이동/삭제) 요청 대기 — 별도 배열(leavechangeList), 위 블록과 데이터 미병합 -->
          <div class="ra-inbox__block ra-inbox__block--leavechange">
            <div class="ra-inbox__head">
              연차 변경 요청 대기 ({{ leavechangeList.length }})
            </div>
            <LeaveChangeScopeFilter
              v-model:site-cd="leaveChangeSiteCd"
              v-model:node-cd="leaveChangeNodeCd"
              v-model:inc-sub-node-yn="leaveChangeIncSubNodeYn"
              :accessible-sites="accessibleSites"
              :node-list="leaveChangeNodeList"
              :node-enabled="leaveChangeNodeSelectEnabled"
              :loading="accessibleSitesLoading"
              :node-list-loading="leaveChangeNodeListLoading"
            />
            <div class="ra-list">
              <div v-if="leaveChangeNoDept" class="ra-empty">
                담당 부서가 없어 연차 변경 요청을 조회할 수 없습니다.
              </div>
              <div v-else-if="leavechangeList.length === 0" class="ra-empty">
                확인 대기 중인 연차 변경 요청이 없습니다.
              </div>
              <div
                v-for="row in leavechangeList"
                :key="row.changeReqId"
                class="ra-row"
                @click="fnSelectLeaveChange(row)"
              >
                <div class="ra-row__main">
                  <span class="ra-row__name">{{ row.targetUserNm }}</span>
                  <span class="ra-chip type">{{ leaveChangeTypeNm(row.reqType) }}</span>
                  <span class="ra-chip self">{{ leaveChangeInitiatorNm(row.initiatorType) }}</span>
                </div>
                <div class="ra-row__sub">
                  {{ fmtDate(row.targetStartDate) }}
                  <template v-if="row.reqType === 'MOVE'">
                    → {{ fmtDate(row.moveTargetDate) }}
                    <!-- 위치선택 확장(2026-08-18): 지정 파트/시각 병기(미지정이면 종전 표시 그대로) -->
                    <template v-if="leaveChangeMovePosLabel(row)">
                      · {{ leaveChangeMovePosLabel(row) }}
                    </template>
                  </template>
                  · {{ leaveChangeStatusNm(row.reqStatus) }}
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 상세 패널 (§5.8.4) -->
        <section class="ra-detail">
          <!-- prafta-leavemulti: 묶음 선택 시 — 기간 요약 + 일괄 승인/반려.
               개별 처리가 필요하면 좌측에서 묶음을 펼쳐 건별로 선택하면 기존 단건 패널이 뜬다. -->
          <template v-if="selectedGroup">
            <div class="ra-sec">
              <div class="ra-sec__title">기간 연차 신청 (묶음)</div>
              <dl class="ra-meta">
                <dt>요청자</dt>
                <dd>{{ selectedGroup.requesterUserNm }}</dd>
                <dt>소속</dt>
                <dd>{{ selectedGroup.nodeNm || "-" }}</dd>
                <dt>기간</dt>
                <dd>
                  {{ fmtDate(selectedGroup.fromYmd) }} ~
                  {{ fmtDate(selectedGroup.toYmd) }}
                </dd>
                <dt>건수 / 일수</dt>
                <dd>{{ selectedGroup.rows.length }}건 · {{ selectedGroup.days }}일</dd>
              </dl>
              <div class="ra-group__datelist">
                <span
                  v-for="row in selectedGroup.rows"
                  :key="row.reqId"
                  class="ra-group__date"
                >
                  {{ fmtDate(row.workYmd) }}
                </span>
              </div>
            </div>

            <div class="ra-sec">
              <div class="ra-sec__title">처리</div>
              <div class="ra-decision">
                <label class="ra-radio">
                  <input type="radio" value="approve" v-model="decision" />
                  <span>승인</span>
                </label>
                <label class="ra-radio">
                  <input type="radio" value="reject" v-model="decision" />
                  <span>반려</span>
                </label>
              </div>
              <textarea
                v-if="decision === 'reject'"
                v-model="rejectReason"
                class="ra-textarea"
                rows="3"
                placeholder="반려 사유를 입력하세요."
              ></textarea>
              <p class="ra-group__note">
                묶음 {{ selectedGroup.rows.length }}건을 한 번에 처리합니다. 마감 등으로
                처리할 수 없는 건이 있으면 그 건만 제외되고 사유가 안내됩니다.
              </p>
              <!-- ★.btn 기본 클래스 필수 — .btn-primary 는 배경/글자색만 준다.
                   단독 사용 시 padding·height·radius·font 가 브라우저 기본값으로 남아
                   다른 화면 버튼과 모양이 어긋난다(2026-08-15 수정).
                   정렬은 단건 패널과 같은 .ra-decide__actions 래퍼를 재사용해 우측으로 맞춘다. -->
              <div class="ra-decide__actions">
                <button
                  class="btn btn-primary"
                  :disabled="groupProcessing"
                  @click="fnProcessGroup"
                >
                  {{ decision === "approve" ? "일괄 승인" : "일괄 반려" }}
                </button>
              </div>
            </div>
          </template>

          <div v-else-if="!selected" class="ra-detail__empty">
            좌측에서 결재 건을 선택하세요.
          </div>
          <template v-else>
            <div class="ra-sec">
              <div class="ra-sec__title">요청 정보</div>
              <dl class="ra-meta">
                <dt>요청번호</dt>
                <dd>{{ selected.reqId }}</dd>
                <dt>요청자</dt>
                <dd>
                  {{ selected.requesterUserNm }} ({{
                    selected.requesterUserCd
                  }})
                  <span v-if="selected.selfYn === 'Y'" class="ra-chip self"
                    >본인 신청</span
                  >
                </dd>
                <dt>소속</dt>
                <dd>{{ selected.nodeNm || "-" }}</dd>
                <dt>대상일자</dt>
                <dd>{{ fmtDate(selected.workYmd) }}</dd>
                <dt>요청일시</dt>
                <dd>{{ (selected.reqDate || "").replace("T", " ") }}</dd>
                <dt>결재 단계</dt>
                <dd>{{ selected.approvalStep }}단계</dd>
              </dl>
            </div>

            <div class="ra-sec">
              <div class="ra-sec__title">연차 내용</div>
              <dl class="ra-meta">
                <dt>연차 타입</dt>
                <dd>{{ leaveTypeLabel(selected) }}</dd>
                <dt>사용 단위</dt>
                <dd>{{ selected.unitNm || "-" }}</dd>
                <dt>사용 일수</dt>
                <dd>{{ Number(selected.leaveDays) }}일</dd>
                <!-- 가불표시-03: 가불 포함 신청 안내 — 승인 시 발생 예정 연차에서 미리 차감됨을 인지시킨다 -->
                <dt v-if="Number(selected.borrowDays) > 0">가불</dt>
                <dd v-if="Number(selected.borrowDays) > 0">
                  가불 {{ Number(selected.borrowDays) }}일 포함 (미래 연차를
                  당겨쓰는 신청 — 승인 시 발생 예정 연차에서 미리 차감됨)
                </dd>
                <dt v-if="selected.startTime">시간대</dt>
                <dd v-if="selected.startTime">
                  {{ fmtTime(selected.startTime) }} ~
                  {{ fmtTime(selected.endTime) }} ({{
                    selected.leaveMinutes
                  }}분)
                </dd>
                <dt>사유</dt>
                <dd>{{ selected.reqReason || "-" }}</dd>
              </dl>
            </div>

            <!-- 결재 진행 현황(P3) — 재기획서 §5.7 ⑥ -->
            <div class="ra-sec">
              <div class="ra-sec__title">결재 진행 현황</div>
              <ApprovalStepTimeline
                :steps="approvalSteps"
                :loading="approvalStepsLoading"
                :error-message="approvalStepsError"
                @retry="fnLoadApprovalSteps(selected.reqId, 'leave')"
              />
            </div>

            <!-- 관리자 결정 (§5.8.4 — 연차는 라디오 2개) -->
            <div class="ra-sec ra-decide">
              <div class="ra-sec__title">결재 처리</div>
              <!-- 결정 A: "내 차례 아님" 배너(P3) — 서버 canProcess 를 그대로 신뢰(클라 자체 판정 금지) -->
              <p v-if="!canProcessCurrentStep" class="ra-decide__warn">
                현재 결재 단계 담당자가 아닙니다. {{ currentApproverUserNm }}님의 처리를 기다려야 합니다.
              </p>
              <label class="ra-radio">
                <input
                  type="radio"
                  v-model="decision"
                  value="approve"
                  :disabled="!canProcessCurrentStep"
                />
                요청대로 승인
              </label>
              <label class="ra-radio">
                <input
                  type="radio"
                  v-model="decision"
                  value="reject"
                  :disabled="!canProcessCurrentStep"
                />
                반려
              </label>
              <textarea
                v-if="decision === 'reject'"
                v-model="rejectReason"
                rows="2"
                maxlength="500"
                placeholder="반려 사유 (필수)"
              />
              <div class="ra-decide__actions">
                <button
                  class="btn btn-primary"
                  :disabled="processing || !canProcessCurrentStep"
                  @click="fnProcess"
                >
                  처리하기
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>

      <!-- 근태 보정 / 초과근무 / 스케줄 수정 / 근무타입 변경: 통합 대기요청 접수함 + 인라인 반려 -->
      <template
        v-else-if="
          activeTab === 'correction' ||
          activeTab === 'overtime' ||
          activeTab === 'schedule' ||
          activeTab === 'defaultSchChange'
        "
      >
        <!-- 접수함 -->
        <section class="ra-inbox">
          <div class="ra-inbox__head">대기 요청 ({{ reqList.length }})</div>
          <div class="ra-list">
            <div v-if="reqList.length === 0" class="ra-empty">
              대기 중인 {{ tabLabel(activeTab) }} 요청이 없습니다.
            </div>
            <div
              v-for="row in reqList"
              :key="row.reqId"
              class="ra-row"
              :class="{
                selected: reqSelected && reqSelected.reqId === row.reqId,
              }"
              @click="fnSelectReq(row)"
            >
              <div class="ra-row__main">
                <span class="ra-row__name">{{ row.userNm || row.userCd }}</span>
                <span class="ra-row__dept">{{ row.nodeNm || "-" }}</span>
                <span class="ra-chip type">{{ reqTypeNm(row.reqType) }}</span>
                <!-- 부서 정보 결측 — 웹에서 승인/반려 불가한 건임을 목록에서 미리 알린다. -->
                <span v-if="isSchedNodeMissing(row)" class="ra-chip warn"
                  >부서 정보 없음</span
                >
              </div>
              <!-- 스케줄 수정: 시각 range 주 + 코드 괄호 병기(재기획서 §5.5.2) -->
              <div v-if="activeTab === 'schedule'" class="ra-row__sub">
                {{ fmtDate(row.workYmd) }}
                <template v-if="row.workSeq"> · {{ row.workSeq }}차</template> ·
                {{ schedSummary(row) }}
              </div>
              <!-- 근무타입 변경: workYmd 없음(근무일 무관) — 근무타입 비교만 표시. -->
              <div v-else-if="activeTab === 'defaultSchChange'" class="ra-row__sub">
                {{ schedSummary(row) }}
              </div>
              <div v-else class="ra-row__sub">
                {{ fmtDate(row.workYmd || row.startDate) }} ·
                {{ fmtTime(row.startTime)
                }}<template v-if="row.endTime">
                  ~ {{ fmtTime(row.endTime) }}</template
                >
              </div>
            </div>
          </div>
        </section>

        <!-- 상세 + 반려 처리 -->
        <section class="ra-detail">
          <div v-if="!reqSelected" class="ra-detail__empty">
            좌측에서 요청 건을 선택하세요.
          </div>
          <template v-else>
            <div class="ra-sec">
              <div class="ra-sec__title">요청 정보</div>
              <dl class="ra-meta">
                <dt>요청번호</dt>
                <dd>{{ reqSelected.reqId }}</dd>
                <dt>요청자</dt>
                <dd>
                  {{ reqSelected.userNm || "-" }} ({{ reqSelected.userCd }})
                </dd>
                <dt>소속</dt>
                <dd>{{ reqSelected.nodeNm || "-" }}</dd>
                <dt>요청유형</dt>
                <dd>{{ reqTypeNm(reqSelected.reqType) }}</dd>
                <dt v-if="reqSelected.workYmd">대상일자</dt>
                <dd v-if="reqSelected.workYmd">
                  {{ fmtDate(reqSelected.workYmd) }}
                  <template v-if="reqSelected.workSeq">
                    · {{ reqSelected.workSeq }}차</template
                  >
                </dd>
                <dt v-if="reqSelected.startDate">시작</dt>
                <dd v-if="reqSelected.startDate">
                  {{ fmtDate(reqSelected.startDate) }}
                  {{ fmtTime(reqSelected.startTime) }}
                </dd>
                <dt v-if="reqSelected.endDate">종료</dt>
                <dd v-if="reqSelected.endDate">
                  {{ fmtDate(reqSelected.endDate) }}
                  {{ fmtTime(reqSelected.endTime) }}
                </dd>
                <dt>요청일시</dt>
                <dd>{{ (reqSelected.reqDate || "").replace("T", " ") }}</dd>
                <dt>사유</dt>
                <dd>{{ reqSelected.reqReason || "-" }}</dd>
              </dl>
            </div>

            <!-- 앞뒤 근무일(D-1 / D+1) 근태 구간 (겹침가드) — 근태 보정 탭 전용.
                 앱 관리자 승인 상세와 동일 정보(웹 일자상세 API 재사용, 서버 완성 표시값).
                 0건이면 섹션 자체를 렌더하지 않는다(AttdDayDetailPop 과 동일 규약). -->
            <AttdNeighborDaySegments
              v-if="
                activeTab === 'correction' &&
                (neighborLoading || neighborSegments.length > 0)
              "
              :segments="neighborSegments"
              :loading="neighborLoading"
            />

            <!-- 현재 → 요청 스케줄 비교 (스케줄 수정 탭 전용, 재기획서 §5.7 ③) -->
            <AttdSchedCompareSection
              v-if="activeTab === 'schedule' || activeTab === 'defaultSchChange'"
              :row="reqSelected"
            />

            <!-- 결재 진행 현황(P3) — 재기획서 §5.7 ⑥ -->
            <div class="ra-sec">
              <div class="ra-sec__title">결재 진행 현황</div>
              <ApprovalStepTimeline
                :steps="approvalSteps"
                :loading="approvalStepsLoading"
                :error-message="approvalStepsError"
                @retry="fnLoadApprovalSteps(reqSelected.reqId, reqTypeGroupOf(activeTab))"
              />
            </div>

            <!-- 결재 처리 — 요청대로 승인 / 반려 (연차 탭과 동일 패턴). 편의상 본 화면에서도 승인 가능. -->
            <div class="ra-sec ra-decide">
              <div class="ra-sec__title">결재 처리</div>
              <!-- 결정 A: "내 차례 아님" 배너(P3) — 서버 canProcess 를 그대로 신뢰(클라 자체 판정 금지) -->
              <p v-if="!canProcessCurrentStep" class="ra-decide__warn">
                현재 결재 단계 담당자가 아닙니다. {{ currentApproverUserNm }}님의 처리를 기다려야 합니다.
              </p>
              <label class="ra-radio">
                <input
                  type="radio"
                  v-model="reqDecision"
                  value="approve"
                  :disabled="!canProcessCurrentStep"
                />
                요청대로 승인
              </label>
              <label class="ra-radio">
                <input
                  type="radio"
                  v-model="reqDecision"
                  value="reject"
                  :disabled="!canProcessCurrentStep"
                />
                반려
              </label>
              <p v-if="reqDecision === 'approve'" class="ra-decide__note">
                {{ approveNoteText }}
              </p>
              <!-- 부서 정보 결측 건 안내 — 버튼은 활성 상태를 유지하고 클릭 시 같은 사유를 안내한다. -->
              <p v-if="schedSelectedNodeMissing" class="ra-decide__warn">
                {{ SCHED_NO_NODE_MSG }}
              </p>
              <textarea
                v-if="reqDecision === 'reject'"
                v-model="reqRejectReason"
                rows="2"
                maxlength="500"
                placeholder="반려 사유 (필수)"
              />
              <div class="ra-decide__actions">
                <button
                  class="btn btn-primary"
                  :class="{ 'btn-reject': reqDecision === 'reject' }"
                  :disabled="reqProcessing || !canProcessCurrentStep"
                  @click="fnProcessReq"
                >
                  처리하기
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>
    </div>

    <LeaveChangeConfirmPop
      v-if="showLeaveChangePop"
      :change-req-id="selectedLeaveChangeReqId"
      @close="showLeaveChangePop = false"
      @confirmed="fnAfterLeaveChangeConfirmed"
    />

    <!-- 내 처리 이력 팝업 — key 로 탭 전환 시 재조회(읽기 전용, 기존 결재 로직 무접점) -->
    <ReqProcessedHistoryPop
      v-if="showHistoryPop"
      :key="activeTab"
      :req-type-group="activeTab"
      :tab-label="activeTabLabel"
      @close="showHistoryPop = false"
    />
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  computed,
  watch,
  onMounted,
  getCurrentInstance,
  defineProps,
  defineOptions,
} from "vue";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import LeaveChangeConfirmPop from "./popup/LeaveChangeConfirmPop.vue";
import ReqProcessedHistoryPop from "./popup/ReqProcessedHistoryPop.vue";
import AttdNeighborDaySegments from "./popup/AttdNeighborDaySegments.vue";
import AttdSchedCompareSection from "./popup/AttdSchedCompareSection.vue";
import ReqInboxSiteFilter from "./popup/ReqInboxSiteFilter.vue";
import LeaveChangeScopeFilter from "./popup/LeaveChangeScopeFilter.vue";
import ApprovalStepTimeline from "@/components/common/ApprovalStepTimeline.vue";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot } from "@/utils/dateFormat";

defineOptions({ name: "Attd_10" });
const props = defineProps({
  title: String,
  buttons: Object,
});
const { proxy } = getCurrentInstance();

// ★이 화면은 ViewHeader 이벤트 중 @search 만 처리한다(결재 처리는 상세 패널 버튼 소관).
//   메뉴가 넘겨준 buttons 를 그대로 쓰면 생성/저장/삭제/엑셀 버튼이 렌더되지만 받는 핸들러가 없어
//   눌러도 무반응이다 — "승인 체크 후 저장" 으로 오해할 여지가 커 노출을 끊는다(2026-08-15).
const localButtons = ref({
  ...props.buttons,
  create: "N",
  save: "N",
  delete: "N",
  excel: "N",
});
// 탭 순서는 재기획서 §5.3 표(스케줄 수정 / 근태 보정 / 초과 / 연차)를 따른다.
const tabs = [
  { key: "schedule", label: "스케줄 수정" },
  { key: "correction", label: "근태 보정" },
  { key: "overtime", label: "초과근무 상신" },
  { key: "leave", label: "연차 상신" },
  { key: "defaultSchChange", label: "근무타입 변경" },
];
const activeTab = ref("leave");
// 접수함다중사업장권한확장-003: 접근 가능 사업장 목록 + 선택값("" = 전체). 연차 탭은 필터 미적용(백엔드 스코프 밖).
const accessibleSites = ref([]);
const accessibleSitesLoading = ref(false);
const selectedSiteCd = ref("");
// 내 처리 이력 팝업 토글 — 팝업은 activeTab 을 그대로 받아 해당 유형 이력을 조회한다.
const showHistoryPop = ref(false);
const activeTabLabel = computed(
  () => tabs.find((t) => t.key === activeTab.value)?.label || ""
);
const approvalList = ref([]);
const selected = ref(null);
const decision = ref("approve");
const rejectReason = ref("");
const processing = ref(false);

// ── prafta-leavemulti: 연차 기간(From-To) 신청 묶음 ─────────────
//   기간신청은 날짜별 REQ N건으로 분해되므로 2주 휴가면 결재함에 14행이 뜬다.
//   같은 leaveGroupId 를 1행으로 접어 일괄 처리한다. 단일일 신청은 leaveGroupId 가 null 이라
//   기존과 동일하게 개별 행으로 보인다(무회귀).
/** 펼쳐 놓은 묶음 ID 집합 — 펼치면 개별 건을 그대로 단건 처리할 수 있다(정책 ④ 개별 예외). */
const expandedGroups = ref([]);
/** 선택된 묶음(단건 선택 시 null). 단건 선택 경로(selected)는 손대지 않는다. */
const selectedGroup = ref(null);
const groupProcessing = ref(false);

/**
 * 결재함 표시 엔트리 — 단건은 그대로, 묶음은 1행으로 접는다.
 *   { kind: 'single', row } | { kind: 'group', groupId, rows, requesterUserNm, nodeNm, fromYmd, toYmd, days, anySelf, anyBorrow }
 * 원본 approvalList 는 건드리지 않는다(다른 로직이 그대로 쓴다).
 */
const approvalEntries = computed(() => {
  const entries = [];
  const groupIdx = new Map();
  for (const row of approvalList.value) {
    const gid = row.leaveGroupId;
    if (!gid) {
      // key 는 <template v-for> 에 부여한다(Vue 3 권장) — 엔트리가 스스로 들고 있게 한다.
      entries.push({
        kind: "single",
        key: "s-" + row.reqId + "-" + row.approvalStep,
        row,
      });
      continue;
    }
    if (!groupIdx.has(gid)) {
      const g = {
        kind: "group",
        key: "g-" + gid,
        groupId: gid,
        rows: [],
        requesterUserNm: row.requesterUserNm,
        nodeNm: row.nodeNm,
      };
      groupIdx.set(gid, g);
      entries.push(g);
    }
    groupIdx.get(gid).rows.push(row);
  }
  // 묶음 요약값 산출(기간·일수·배지)
  for (const g of entries) {
    if (g.kind !== "group") continue;
    const ymds = g.rows.map((r) => r.workYmd).filter(Boolean).sort();
    g.fromYmd = ymds[0];
    g.toYmd = ymds[ymds.length - 1];
    g.days = g.rows.reduce((s, r) => s + Number(r.leaveDays || 0), 0);
    g.anySelf = g.rows.some((r) => r.selfYn === "Y");
    g.anyBorrow = g.rows.some((r) => Number(r.borrowDays) > 0);
  }
  return entries;
});

const isGroupExpanded = (gid) => expandedGroups.value.includes(gid);

const fnToggleGroup = (gid) => {
  const i = expandedGroups.value.indexOf(gid);
  if (i >= 0) expandedGroups.value.splice(i, 1);
  else expandedGroups.value.push(gid);
};

/** 묶음 선택 — 단건 선택(selected)과 상호배타. */
const fnSelectGroup = (group) => {
  selectedGroup.value = group;
  selected.value = null;
  decision.value = "approve";
  rejectReason.value = "";
};

/**
 * 묶음 일괄 승인/반려.
 *   서버는 부분 성공을 반환한다(1건이 마감 등으로 막혀도 나머지는 확정) —
 *   실패 건은 사유와 함께 안내하고 목록을 재조회한다.
 */
const fnProcessGroup = async () => {
  const g = selectedGroup.value;
  if (!g || !g.rows.length) return;

  if (decision.value === "reject" && !(rejectReason.value || "").trim()) {
    return proxy.$alert("반려 사유를 입력해주세요.");
  }
  const isApprove = decision.value === "approve";
  const ok = await proxy.$confirm(
    `${g.rows.length}건을 ${isApprove ? "승인" : "반려"} 처리하시겠습니까?`
  );
  if (!ok) return;

  groupProcessing.value = true;
  try {
    const url = isApprove
      ? "/webApi/leaveflow/approve-bulk"
      : "/webApi/leaveflow/reject-bulk";
    const { data } = await axios.post(url, {
      items: g.rows.map((r) => ({
        reqId: r.reqId,
        approvalStep: r.approvalStep,
      })),
      comment: isApprove ? "" : rejectReason.value,
    });

    const failed = Array.isArray(data?.failedList) ? data.failedList : [];
    if (failed.length === 0) {
      await proxy.$alert(`${data?.successCount ?? g.rows.length}건 처리되었습니다.`);
    } else {
      // 부분 성공 — 성공분은 확정됐고 실패분만 사유와 함께 알린다.
      const lines = failed
        .slice(0, 10)
        .map((f) => `· ${f.reqId}: ${f.reason || "처리 실패"}`)
        .join("\n");
      const more = failed.length > 10 ? `\n외 ${failed.length - 10}건` : "";
      await proxy.$alert(
        `${data?.successCount ?? 0}건 처리 · ${failed.length}건 제외\n${lines}${more}`
      );
    }
    selectedGroup.value = null;
    await fnLoad();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(
      resolveApiErrorMessage(e, "일괄 결재 처리 중 오류가 발생했습니다.")
    );
  } finally {
    groupProcessing.value = false;
  }
};

// 근태 보정 / 초과근무 통합 대기요청 접수함
const reqList = ref([]);
const reqSelected = ref(null);
const reqDecision = ref("approve"); // approve | reject
const reqRejectReason = ref("");
const reqProcessing = ref(false);
// 앞뒤 근무일(D-1/D+1) 근태 구간 — 근태 보정 승인 판단 보조(앱 승인 상세와 동일 정보).
const neighborSegments = ref([]);
const neighborLoading = ref(false);

// ── 결재 진행 타임라인(P3) ─────────────────────────────────────────
//   근태보정/초과/스케줄 3탭(reqSelected)과 연차 탭(selected)이 공용으로 쓰는 1세트.
//   두 상세 패널은 동시에 열리지 않으므로 공유해도 무방하나, 선택이 바뀔 때마다
//   fnLoadApprovalSteps 진입 시점에 초기화해 다른 건의 잔상이 보이지 않게 한다.
const approvalSteps = ref([]);
const approvalStepsLoading = ref(false);
const approvalStepsError = ref("");
const canProcessCurrentStep = ref(true); // 서버 응답 전 기본값 true(무차단) — 응답 도착 후 서버값으로 교체
const currentApproverUserNm = ref("");

// GET /webApi/reqinbox/approval-line 조회 → approvalSteps/canProcessCurrentStep 등 갱신.
// reqTypeGroup 은 correction/overtime/schedule(reqTypeGroupOf(activeTab.value)) 또는 'leave' 고정.
// stale-response 가드(ApprovalLineDetailSheet.vue app 패턴과 동일): fetch 시작 시점의 reqId 를
//   캡처해두고, 응답 도착 시점에 현재 선택된 건(연차 탭=selected, 그 외 3탭=reqSelected)이
//   여전히 이 reqId 와 일치할 때만 상태를 반영한다 — 빠른 재선택 시 늦게 온 이전 응답이
//   최신 선택 상태를 덮어쓰지 않도록 방지한다.
const fnLoadApprovalSteps = async (reqId, reqTypeGroup) => {
  // 선택이 바뀔 때마다 이전 건의 잔상이 남지 않도록 즉시 초기화한다.
  approvalSteps.value = [];
  approvalStepsError.value = "";
  canProcessCurrentStep.value = true;
  currentApproverUserNm.value = "";
  if (!reqId) return;

  const isCurrent = () =>
    reqTypeGroup === "leave"
      ? selected.value?.reqId === reqId
      : reqSelected.value?.reqId === reqId;

  approvalStepsLoading.value = true;
  try {
    const r = await axios.get("/webApi/reqinbox/approval-line", {
      params: { reqId, reqTypeGroup },
    });
    if (!isCurrent()) return; // stale 응답 폐기
    approvalSteps.value = Array.isArray(r.data?.steps) ? r.data.steps : [];
    canProcessCurrentStep.value = r.data?.canProcess !== false;
    currentApproverUserNm.value = r.data?.currentApproverUserNm ?? "";
  } catch (e) {
    console.warn("[Attd_10] 결재선 조회 실패", e);
    if (!isCurrent()) return; // stale 응답 폐기
    approvalSteps.value = [];
    approvalStepsError.value = resolveApiErrorMessage(
      e,
      "결재선을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
    );
  } finally {
    if (isCurrent()) approvalStepsLoading.value = false;
  }
};

// 탭별 대기 건수 (배지용) — 활성 탭과 무관하게 유지
const leaveCount = ref(0);
const correctionCount = ref(0);
const overtimeCount = ref(0);
const scheduleCount = ref(0);
const defaultSchChangeCount = ref(0);

// "연차 상신" 탭 내 "연차 변경 요청 대기" 소섹션(B안) — approvalList 와 별개 배열, 데이터 병합 없음.
//   상세/확인/반려는 기존 LeaveChangeConfirmPop 재사용, 중복 컴포넌트 금지.
const leavechangeList = ref([]);
const leavechangeCount = ref(0);
const showLeaveChangePop = ref(false);
const selectedLeaveChangeReqId = ref("");
// 노드 관리자인데 부서 필터(leaveChangeNodeCd)가 비어 조회를 생략한 상태 — 인라인 안내만 표시
//   (F-1, alert 금지). 접수함연차변경다중사업장확장-004: 판정 기준을 "세션 gv_nodeCd 존재 여부"에서
//   "선택된 leaveChangeNodeCd 존재 여부"로 갱신(최초 마운트 시 gv_nodeCd 로 프리필하므로 초기 동작은 동일).
const leaveChangeNoDept = ref(false);

// 권한 스코프(F-1) — Attd_13.vue 의 프리필 규칙과 동형: master/hr 는 전사, 그 외는 담당 부서 강제.
//   이 화면엔 부서 셀렉터가 없으므로 세션값을 그대로 프리필 파라미터로 사용한다.
const isMasterOrHr = computed(() => {
  const a = sessionStorage.getItem("gv_authCd");
  return a === "master" || a === "hr";
});

// 접수함연차변경다중사업장확장-003/004: 소섹션 전용 사업장/부서 필터 상태.
//   accessibleSites/accessibleSitesLoading 은 상단 3탭 필터와 공유(이미 로드된 것을 재사용 — 재조회 없음).
const leaveChangeSiteCd = ref("");
const leaveChangeNodeCd = ref("");
const leaveChangeIncSubNodeYn = ref("Y"); // 기존 고정값(Attd_13 기본=포함)과 동일 — 무회귀 기본값
const leaveChangeNodeList = ref([]);
const leaveChangeNodeListLoading = ref(false);

// "사업장이 정확히 1곳으로 좁혀졌는지" 판정(§0-5 설계 결정) — 접근 가능 사업장이 1곳뿐이면
//   (leaveChangeSiteCd 가 비어 있어도) 사실상 그 1곳으로 확정된 것이므로 부서 필터를 활성화한다
//   (단일 사업장 관리자 무회귀). 그 외엔 leaveChangeSiteCd 가 비어있지 않을 때만 활성.
const leaveChangeNodeSelectEnabled = computed(() => {
  return accessibleSites.value.length === 1 || !!leaveChangeSiteCd.value;
});

// 연차 변경 요청 코드 → 라벨 매핑(TB_LEAVE_CHANGE_REQUEST 전용 — SYS032 와 무관, 재사용 금지)
const LEAVE_CHANGE_TYPE_NM = { MOVE: "이동", DELETE: "삭제" };
const LEAVE_CHANGE_INITIATOR_NM = { ADMIN: "관리자발의", WORKER: "근로자발의" };
const LEAVE_CHANGE_STATUS_NM = {
  REQUESTED: "요청(응답대기)",
  AGREED: "동의(확인대기)",
  REJECTED: "거부",
  CONFIRMED: "확정",
  CLOSED: "종료",
};
const leaveChangeTypeNm = (t) => LEAVE_CHANGE_TYPE_NM[t] || t || "-";
const leaveChangeInitiatorNm = (t) => LEAVE_CHANGE_INITIATOR_NM[t] || t || "-";
const leaveChangeStatusNm = (s) => LEAVE_CHANGE_STATUS_NM[s] || s || "-";

// ── 위치선택 확장(2026-08-18): 연차 이동 요청 카드에 지정 파트/시각 병기 ──
//   미지정(null/구서버 미수신)이면 빈 값 → 종전 표시 바이트 그대로(무회귀).
//   반차 파트는 대상일 경계 조회 없이 "시작 기준(늦게 출근)/종료 기준(일찍 퇴근)" 고정 표기
//   (오전/오후 환산 금지 — plan §4 사용자 확정). 시간차 종료는 시작+원 분량(leaveMinutes) 클라 파생(표시 전용).
const LEAVE_CHANGE_HALF_PART_NM = {
  START: "시작 기준(늦게 출근)",
  END: "종료 기준(일찍 퇴근)",
};
const lcHhmmToMin = (hhmm) => {
  const v = String(hhmm ?? "");
  if (v.length !== 4) return null;
  const h = parseInt(v.slice(0, 2), 10);
  const m = parseInt(v.slice(2, 4), 10);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  if (h < 0 || h > 23 || m < 0 || m > 59) return null;
  return h * 60 + m;
};
const leaveChangeMovePosLabel = (row) => {
  if (row?.moveTargetHalfPart)
    return LEAVE_CHANGE_HALF_PART_NM[row.moveTargetHalfPart] || "";
  const s = lcHhmmToMin(row?.moveTargetStartTime);
  if (s == null) return "";
  const dur = Number(row?.leaveMinutes);
  if (!Number.isFinite(dur) || dur <= 0) return fmtTime(row.moveTargetStartTime);
  // 자정 넘김(END<START)은 익일 저장 규약 — 시각만 모듈러 표기
  const e = (s + dur) % 1440;
  const pad = (n) => String(n).padStart(2, "0");
  return `${fmtTime(row.moveTargetStartTime)}~${pad(Math.floor(e / 60))}:${pad(e % 60)}`;
};

const tabLabel = (key) => tabs.find((t) => t.key === key)?.label ?? "";

// 탭별 대기 건수 (배지용). 0이면 배지 미표시.
const tabCount = (key) => {
  if (key === "leave") return leaveCount.value;
  if (key === "correction") return correctionCount.value;
  if (key === "overtime") return overtimeCount.value;
  if (key === "schedule") return scheduleCount.value;
  if (key === "defaultSchChange") return defaultSchChangeCount.value;
  return 0;
};

// REQ_TYPE(SYS032) 표시명 — 본 화면에서 다루는 값만.
const reqTypeNm = (t) =>
  ({
    "01": "근태 생성",
    "02": "근태 수정",
    "03": "초과근무 생성",
    "10": "스케줄 수정",
    "14": "근무타입 변경",
  }[t] ||
  t ||
  "-");

// 승인을 처리하는 원 화면명 (반려 안내용).
const approveScreenNm = (tab) =>
  tab === "overtime" ? "초과근무 관리" : "근태 관리";

// "요청대로 승인" 선택 시 안내 문구 — 탭별로 승인 효과가 다르다.
//   correction/overtime 은 기존 문구를 문자 단위로 유지하고, schedule 만 별도 문구를 쓴다.
const approveNoteText = computed(() => {
  if (activeTab.value === "schedule") {
    return "승인 시 해당 일자 근무계획(스케줄)이 요청 스케줄로 변경되고 요청이 승인됩니다.";
  }
  if (activeTab.value === "defaultSchChange") {
    return "승인 시 명일부터 연말까지 근무계획이 자동 생성·갱신되고 요청이 승인됩니다.";
  }
  return `${approveScreenNm(activeTab.value)}와 동일하게 처리됩니다(근태/초과근무 기록 반영 + 요청 승인).`;
});

// 탭 → reqTypeGroup 매핑 (백엔드 /reqinbox/pending).
//   삼항식은 schedule 을 correction 으로 오분류하므로 명시 매핑을 쓴다.
//   (correction/overtime 반환값은 종전과 동일. leave 탭은 이 함수를 호출하지 않는다.)
const REQ_TYPE_GROUP_BY_TAB = {
  correction: "correction",
  overtime: "overtime",
  schedule: "schedule",
  defaultSchChange: "defaultSchChange",
};
const reqTypeGroupOf = (tab) => REQ_TYPE_GROUP_BY_TAB[tab] ?? "correction";

// 표시용 날짜 포맷은 dateFormat 단일 출처에 위임(점). 빈값/형식불충분은 "-".
const fmtDate = (ymd) => {
  if (!ymd || ymd.length < 8) return ymd || "-";
  return formatYmdDot(ymd);
};
const fmtTime = (hhmm) => {
  if (!hhmm || hhmm.length < 4) return hhmm || "";
  return `${hhmm.slice(0, 2)}:${hhmm.slice(2, 4)}`;
};

// 스케줄 1차 구간 시각 range("HH:MM-HH:MM"). 시작/종료 어느 하나라도 없으면 "".
const schedRange = (strTime, endTime) => {
  const s = fmtTime(strTime);
  const e = fmtTime(endTime);
  if (!s || !e) return "";
  return `${s}-${e}`;
};

// 목록 행 요약(재기획서 §5.5.2) — 시각 range 를 주로, 스케줄 코드는 괄호 병기.
//   예) "정규 09:00-18:00 → 야간 18:00-04:00 (00011→00022)"
//   목록은 1차 구간만 축약 표기하며, 2차·고정연장은 상세 비교 섹션에서 본다.
const schedSummary = (row) => {
  if (!row) return "";
  const cur = [row.curSchNo, schedRange(row.curFstStrTime, row.curFstEndTime)]
    .filter(Boolean)
    .join(" ");
  const req = [row.reqSchNo, schedRange(row.reqFstStrTime, row.reqFstEndTime)]
    .filter(Boolean)
    .join(" ");
  const head = `${cur || "현재 없음"} → ${req || "요청 스케줄 확인 불가"}`;
  if (!row.curSchCd && !row.reqSchCd) return head;
  const codeText =
    row.curSchCd && row.reqSchCd
      ? `${row.curSchCd}→${row.reqSchCd}`
      : row.curSchCd || row.reqSchCd;
  return `${head} (${codeText})`;
};

// 스케줄 탭 승인/반려 차단 사유 — REQ 원본 NODE_CD 결측.
//   서버 DTO 가 nodeCd 를 @NotBlank 로 요구하고 REQ row 와 일치까지 검증하므로,
//   결측 건은 웹에서 처리 불가하다(앱 관리자 승인 경로 이용). 버튼은 비활성화하지 않고
//   활성 상태를 유지한 뒤 클릭 시 사유를 안내한다(비활성 버튼은 사유에 도달할 수 없어 오인된다).
const SCHED_NO_NODE_MSG =
  "이 요청에 부서 정보가 없어 승인/반려할 수 없습니다. 관리자에게 문의하세요.";
const isSchedNodeMissing = (row) =>
  activeTab.value === "schedule" && !!row && !row.nodeCd;
const schedSelectedNodeMissing = computed(() =>
  isSchedNodeMissing(reqSelected.value)
);

// 연차 타입 표시 — 연차명(연차번호) 형태. 일부 누락 시 가능한 값만, 모두 없으면 leaveCd → "-".
const leaveTypeLabel = (row) => {
  if (!row) return "-";
  const nm = row.leaveNm;
  const no = row.leaveNo;
  if (nm && no) return `${nm}(${no})`;
  return nm || no || row.leaveCd || "-";
};

const fnSelectTab = (key) => {
  activeTab.value = key;
  fnLoad();
};

// 탭 전환/내부 재조회 진입점 — 활성 탭 데이터 로드.
const fnLoad = async () => {
  if (activeTab.value === "leave") {
    // B안: 결재 대기 + 연차 변경 대기 두 소섹션을 동시 로드(데이터 병합 없음, 별개 배열 유지)
    return Promise.all([fnLoadApprovals(), fnLoadLeaveChanges()]);
  }
  if (
    activeTab.value === "correction" ||
    activeTab.value === "overtime" ||
    activeTab.value === "schedule"
  ) {
    return fnLoadReqInbox();
  }
};

// 조회 버튼 진입점 — 활성 탭 목록 + 전 탭(근태보정/초과근무/연차) 카운트 배지를 함께 갱신.
const fnSearch = async () => {
  await Promise.all([fnLoad(), fnLoadCounts()]);
};

const fnLoadApprovals = async () => {
  try {
    const r = await axios.get("/webApi/leaveflow/my-approvals", {});
    approvalList.value = r.data?.approvalList ?? [];
    // 선택 유지 또는 해제
    if (
      selected.value &&
      !approvalList.value.some((x) => x.reqId === selected.value.reqId)
    ) {
      selected.value = null;
    }
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "결재 목록 조회 오류."));
  }
};

// 접수함연차변경다중사업장확장-004: 소섹션 부서 목록 조회(Attd_13.vue fnLoadNodeList 패턴 재사용
//   — 신규 API 아님). siteCd 미지정이면 목록을 비운다(부서 select 는 nodeEnabled 로 이미 disabled).
const fnLoadLeaveChangeNodeList = async (siteCd) => {
  if (!siteCd) {
    leaveChangeNodeList.value = [];
    return;
  }
  leaveChangeNodeListLoading.value = true;
  try {
    const r = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: { cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"), siteCd },
    });
    leaveChangeNodeList.value = (r.data?.siteNodeInfoList ?? []).map((n) => ({
      nodeCd: n.nodeCd,
      nodeNm: n.nodeNm,
    }));
  } catch (e) {
    console.warn("[Attd_10] 연차 변경 부서 목록 조회 실패", e);
    leaveChangeNodeList.value = [];
  } finally {
    leaveChangeNodeListLoading.value = false;
  }
};

// 접수함연차변경다중사업장확장-004: 연차 변경 소섹션 스코프 파라미터 조립 — 목록(fnLoadLeaveChanges)과
//   카운트(fnLoadCounts) 가 동일 로직을 공유한다(항상 같은 스코프를 보장 — 배지·리스트 불일치 방지).
//   null 반환 = 노드 관리자인데 부서 필터가 비어 서버 호출 자체를 생략해야 함을 의미한다(ATTD_400_130 회피).
const buildLeaveChangeScopeParams = () => {
  if (!isMasterOrHr.value && !leaveChangeNodeCd.value) {
    return null;
  }
  const params = { REQ_STATUS: "AGREED" };
  if (leaveChangeSiteCd.value) {
    params.SITE_CD = leaveChangeSiteCd.value;
  } else if (accessibleSites.value.length === 1) {
    // 접근 가능 사업장이 1곳뿐이면 명시 선택 없이도 그 1곳으로 확정(§0-5) — 서버 nodeSiteCd 판정과 정합.
    params.SITE_CD = accessibleSites.value[0].siteCd;
  }
  if (leaveChangeNodeCd.value) {
    params.NODE_CD = leaveChangeNodeCd.value;
    params.INC_SUB_NODE_YN = leaveChangeIncSubNodeYn.value;
  }
  return params;
};

// "연차 상신" 탭 내 "연차 변경 요청 대기" 소섹션 목록 조회(B안) — 확인 대기(AGREED) 건만 노출.
//   접수함연차변경다중사업장확장-004: 세션 프리필 대신 LeaveChangeScopeFilter 가 노출하는
//   leaveChangeSiteCd/leaveChangeNodeCd/leaveChangeIncSubNodeYn ref 값을 그대로 params 에 싣는다
//   (빈 문자열 = 서버가 "전체"로 해석 — ChangeRequestListParam 계약, 접수함연차변경다중사업장확장-001/002).
//   fnLoadApprovals 와 독립적으로 실패를 격리해, 한쪽이 실패해도 다른 소섹션은 정상 표시되게 한다.
const fnLoadLeaveChanges = async () => {
  leaveChangeNoDept.value = false;
  const params = buildLeaveChangeScopeParams();
  if (!params) {
    // 노드 관리자인데 부서 필터가 비어 있음(예: 사업장을 "전체"로 전환하는 cascade 로 함께 초기화된
    //   경우) — 호출을 생략하고 조용히 비운다(alert 금지, 기존 관례 유지).
    leavechangeList.value = [];
    leavechangeCount.value = 0;
    leaveChangeNoDept.value = true;
    return;
  }
  try {
    const r = await axios.get("/webApi/attd13/change-requests", { params });
    leavechangeList.value = r.data?.list ?? [];
    leavechangeCount.value = r.data?.totalCnt ?? leavechangeList.value.length;
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "연차 변경 요청 조회 오류."));
  }
};

// 소섹션 행 클릭 → 상세/확인/반려 팝업 오픈(UI 토글 — 로직 없음, 기존 LeaveChangeConfirmPop 재사용)
const fnSelectLeaveChange = (row) => {
  selectedLeaveChangeReqId.value = row.changeReqId;
  showLeaveChangePop.value = true;
};

// 팝업에서 확인/반려 완료 시 연차변경 소섹션·탭 배지만 재조회(approvalList 는 영향 없음)
const fnAfterLeaveChangeConfirmed = () => {
  showLeaveChangePop.value = false;
  fnLoadLeaveChanges();
  fnLoadCounts();
};

// 접수함다중사업장권한확장-002: 관리자가 접근 가능한 사업장 목록 조회(셀렉터 옵션용).
//   실패해도 화면 흐름은 막지 않는다(카운트 로드와 동일한 보조 정보 취급) — 셀렉터가
//   자체 v-if(accessibleSites.length > 1)로 숨어 단일 사업장 관리자와 동일하게 무회귀.
const fnLoadAccessibleSites = async () => {
  accessibleSitesLoading.value = true;
  try {
    const r = await axios.get("/webApi/reqinbox/accessible-sites");
    accessibleSites.value = r.data?.accessibleSites ?? [];
  } catch (e) {
    console.warn("[Attd_10] 접근 가능 사업장 목록 조회 실패", e);
  } finally {
    accessibleSitesLoading.value = false;
  }
};

const fnLoadReqInbox = async () => {
  try {
    const r = await axios.get("/webApi/reqinbox/pending", {
      params: {
        reqTypeGroup: reqTypeGroupOf(activeTab.value),
        siteCd: selectedSiteCd.value || undefined,
      },
    });
    reqList.value = r.data?.pendingList ?? [];
    if (
      reqSelected.value &&
      !reqList.value.some((x) => x.reqId === reqSelected.value.reqId)
    ) {
      reqSelected.value = null;
    }
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "대기 요청 조회 오류."));
  }
};

// 탭 배지용 대기 건수 로드 — 활성 탭과 무관하게 연차(결재+변경 합산)/근태보정/초과근무 갱신.
// 배지 카운트는 보조 정보이므로 실패해도 화면 흐름은 막지 않는다.
const fnLoadCounts = async () => {
  try {
    const [leaveRes, corrRes, otRes] = await Promise.all([
      axios.get("/webApi/leaveflow/my-approvals"),
      axios.get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "correction", siteCd: selectedSiteCd.value || undefined },
      }),
      axios.get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "overtime", siteCd: selectedSiteCd.value || undefined },
      }),
    ]);
    const approvalCnt = (leaveRes.data?.approvalList ?? []).length;
    correctionCount.value = (corrRes.data?.pendingList ?? []).length;
    overtimeCount.value = (otRes.data?.pendingList ?? []).length;

    // 연차 변경 요청 대기 건수는 별도 try/catch 로 격리(노드 관리자 400 등 실패해도 위 3개 배지 갱신은 막지 않는다).
    //   접수함연차변경다중사업장확장-004: buildLeaveChangeScopeParams 로 fnLoadLeaveChanges 와 동일
    //   스코프를 실어 보낸다(종전엔 SITE_CD/NODE_CD 를 전혀 싣지 않아 노드 관리자는 이 호출이 늘 실패했다).
    try {
      const changeParams = buildLeaveChangeScopeParams();
      if (changeParams) {
        const changeRes = await axios.get("/webApi/attd13/change-requests", {
          params: changeParams,
        });
        leavechangeCount.value =
          changeRes.data?.totalCnt ?? (changeRes.data?.list ?? []).length;
      } else {
        leavechangeCount.value = 0;
      }
    } catch (e) {
      console.warn("[Attd_10] 연차 변경 요청 카운트 로드 실패", e);
    }

    // 스케줄 수정 대기 건수도 별도 try/catch 로 격리한다. 위 Promise.all 에 넣으면 스케줄 조회가
    //   실패할 때 기존 3개 배지가 함께 죽는다(무회귀 최우선).
    try {
      const schedRes = await axios.get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "schedule", siteCd: selectedSiteCd.value || undefined },
      });
      scheduleCount.value = (schedRes.data?.pendingList ?? []).length;
    } catch (e) {
      console.warn("[Attd_10] 스케줄 수정 요청 카운트 로드 실패", e);
    }

    // 근무타입 변경 대기 건수도 별도 try/catch 로 격리(실패해도 다른 배지 갱신을 막지 않는다).
    try {
      const dscRes = await axios.get("/webApi/reqinbox/pending", {
        params: { reqTypeGroup: "defaultSchChange", siteCd: selectedSiteCd.value || undefined },
      });
      defaultSchChangeCount.value = (dscRes.data?.pendingList ?? []).length;
    } catch (e) {
      console.warn("[Attd_10] 근무타입 변경 요청 카운트 로드 실패", e);
    }

    // B안 배지 합산 결정(상세설명 5번 근거): 탭 배지는 "이 탭에 처리할 게 총 몇 건" 을 보여줘야 하므로
    //   결재 대기 + 연차 변경 대기를 합산한다. 소섹션별 개별 건수는 각 블록 헤더에서 별도 표시.
    leaveCount.value = approvalCnt + leavechangeCount.value;
  } catch (e) {
    console.warn("[Attd_10] 탭 카운트 로드 실패", e);
  }
};

const fnSelectReq = (row) => {
  reqSelected.value = row;
  reqDecision.value = "approve";
  reqRejectReason.value = "";
  fnLoadNeighborSegments(row);
  fnLoadApprovalSteps(row.reqId, reqTypeGroupOf(activeTab.value));
};

// 앞뒤 근무일 근태 구간 로드 — 일자상세 API(daily-attd-details)의 neighborAttdSegmentList 만 취한다
// (서버 완성 표시값 — 프론트 재판정 금지). 보조 정보이므로 실패해도 승인 흐름을 막지 않는다(빈 목록 유지).
const fnLoadNeighborSegments = async (row) => {
  neighborSegments.value = [];
  if (activeTab.value !== "correction" || !row?.workYmd) return;
  neighborLoading.value = true;
  try {
    const r = await axios.get("/webApi/attd07/daily-attd-details", {
      params: {
        siteCd: row.siteCd,
        userCd: row.userCd,
        workYmd: row.workYmd,
        nodeCd: row.nodeCd || "",
      },
    });
    // 응답 도착 전 다른 행을 선택했으면 늦게 온 응답은 버린다(경합 방지).
    if (reqSelected.value?.reqId === row.reqId) {
      neighborSegments.value = r.data?.neighborAttdSegmentList ?? [];
    }
  } catch (e) {
    console.warn("[Attd_10] 앞뒤 근무일 근태 조회 실패", e);
  } finally {
    neighborLoading.value = false;
  }
};

// 근태보정/초과근무/스케줄 수정 처리 진입점 — 라디오 선택에 따라 승인 또는 반려로 분기.
const fnProcessReq = () => {
  // 스케줄 탭 전용 가드: 부서 정보가 없으면 서버가 400/ATTD_400_005 로만 응답해 원인 파악이 어렵다.
  //   버튼은 활성 상태로 두고 클릭 시 사유를 안내한다(correction/overtime 에는 적용하지 않는다).
  if (schedSelectedNodeMissing.value) {
    return proxy.$alert(SCHED_NO_NODE_MSG);
  }
  if (reqDecision.value === "reject") return fnRejectReq();
  return fnApproveReq();
};

// 근태보정/초과근무 요청을 "요청대로 승인" — 근무 관리(Attd_07)와 동일한 승인 엔드포인트를
// 호출해 근태/초과근무 기록을 반영하고 요청 상태를 승인('02')으로 전이한다.
// (역할은 근무 관리 화면과 중복되지만, 편의상 본 화면에서도 처리 가능하도록 제공.)
const fnApproveReq = async () => {
  const r = reqSelected.value;
  if (!r) return;
  const ok = await proxy.$confirm("승인 처리하시겠습니까?");
  if (!ok) return;

  const isOvertime = activeTab.value === "overtime";
  const isSchedule = activeTab.value === "schedule";
  const isDefaultSchChange = activeTab.value === "defaultSchChange";
  let url;
  let payload;
  if (isSchedule) {
    // 스케줄 수정 승인: 목표 스케줄 코드(SCH_CD)는 body 로 보내지 않는다.
    //   서버가 REQ row 의 SCH_CD 를 권위값으로 써서 work_plan 의 WORK_PLAN_CD 를 갱신한다.
    //   키 필드는 목록 응답 값을 그대로 전달한다(임의 폴백 금지 — 서버가 REQ row 와 일치를 요구).
    url = "/webApi/attd07/approve-sched-modify-requests";
    payload = {
      reqId: r.reqId,
      siteCd: r.siteCd,
      userCd: r.userCd,
      workYmd: r.workYmd,
      workSeq: String(r.workSeq),
      nodeCd: r.nodeCd,
    };
  } else if (isDefaultSchChange) {
    // 근무타입 변경 승인: 앱 트랙이 이미 만든 API 를 그대로 호출(웹은 신규 승인 백엔드 작성 없음).
    //   workYmd/workSeq/nodeCd 불필요(이 요청 유형은 근무일 무관, API 계약).
    url = "/webApi/attd07/approve-default-sch-requests";
    payload = { reqId: r.reqId, siteCd: r.siteCd, userCd: r.userCd };
  } else if (isOvertime) {
    // 초과근무 승인: 요청 구간 그대로 OT 등록 + 연결 요청 승인 마감.
    url = "/webApi/attd07/update-user-overtime-requests";
    payload = {
      reqId: r.reqId,
      userCd: r.userCd,
      siteCd: r.siteCd,
      nodeCd: r.nodeCd || "",
      workYmd: r.workYmd,
      reqReason: r.reqReason || "",
      overtimes: [
        {
          // prafta-043: 초과근무 유형(otType) 전면 파기 — payload 에서 제거.
          startDate: r.startDate || r.workYmd,
          startTime: r.startTime,
          endDate: r.endDate || r.workYmd,
          endTime: r.endTime,
        },
      ],
    };
  } else {
    // 근태 보정 승인: 요청자가 적은 출퇴근 값을 그대로 근태 기록에 반영 + 요청 승인 마감.
    url = "/webApi/attd07/update-user-attd-requests";
    payload = {
      reqId: r.reqId,
      siteCd: r.siteCd,
      userCd: r.userCd,
      workYmd: r.workYmd,
      workSeq: String(r.workSeq),
      nodeCd: r.nodeCd,
      checkInDate: r.startDate || r.workYmd,
      checkInTime: r.startTime || "",
      checkInMethod: "02",
      checkOutDate: r.endDate || r.workYmd,
      checkOutTime: r.endTime || "",
      checkOutMethod: "02",
      processComment: "요청 승인 관리 승인",
    };
  }

  reqProcessing.value = true;
  try {
    await axios.post(url, payload);
    await proxy.$alert("승인되었습니다.");
    reqSelected.value = null;
    await fnLoadReqInbox();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(
      resolveApiErrorMessage(e, "승인 처리 중 오류가 발생했습니다.")
    );
  } finally {
    reqProcessing.value = false;
  }
};

const fnRejectReq = async () => {
  if (!reqSelected.value) return;
  if (!(reqRejectReason.value || "").trim()) {
    return proxy.$alert("반려 사유를 입력해주세요.");
  }
  const ok = await proxy.$confirm("반려 처리하시겠습니까?");
  if (!ok) return;

  const r = reqSelected.value;
  const isOvertime = activeTab.value === "overtime";
  const isSchedule = activeTab.value === "schedule";
  const isDefaultSchChange = activeTab.value === "defaultSchChange";
  // 반려 엔드포인트는 서버 보관 REQ 와 키필드 일치를 요구한다(변조 방지).
  //   스케줄 수정 반려는 근태 반려와 동일한 body 형태를 쓰되 전용 엔드포인트를 호출한다.
  let url;
  if (isSchedule) {
    url = "/webApi/attd07/reject-sched-modify-requests";
  } else if (isDefaultSchChange) {
    // 근무타입 변경 반려: 앱 트랙이 이미 만든 API 를 그대로 호출.
    url = "/webApi/attd07/reject-default-sch-requests";
  } else if (isOvertime) {
    url = "/webApi/attd07/reject-user-overtime-requests";
  } else {
    url = "/webApi/attd07/reject-user-attd-requests";
  }
  const payload =
    isOvertime || isDefaultSchChange
      ? {
          reqId: r.reqId,
          siteCd: r.siteCd,
          userCd: r.userCd,
          rejectReason: reqRejectReason.value,
        }
      : {
          reqId: r.reqId,
          siteCd: r.siteCd,
          userCd: r.userCd,
          workYmd: r.workYmd,
          workSeq: String(r.workSeq),
          nodeCd: r.nodeCd,
          rejectReason: reqRejectReason.value,
        };

  reqProcessing.value = true;
  try {
    await axios.post(url, payload);
    await proxy.$alert("반려되었습니다.");
    reqSelected.value = null;
    await fnLoadReqInbox();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "반려 처리 중 오류가 발생했습니다."));
  } finally {
    reqProcessing.value = false;
  }
};

const fnSelect = (row) => {
  selected.value = row;
  selectedGroup.value = null;   // 단건 선택 시 묶음 선택 해제(상호배타)
  decision.value = "approve";
  rejectReason.value = "";
  fnLoadApprovalSteps(row.reqId, "leave");
};

const fnProcess = async () => {
  if (!selected.value) return;
  const payload = {
    reqId: selected.value.reqId,
    approvalStep: selected.value.approvalStep,
    comment: decision.value === "reject" ? rejectReason.value : "",
  };

  if (decision.value === "reject") {
    if (!(rejectReason.value || "").trim()) {
      return proxy.$alert("반려 사유를 입력해주세요.");
    }
  }

  const ok = await proxy.$confirm(
    decision.value === "approve" ? "승인 처리하시겠습니까?" : "반려 처리하시겠습니까?"
  );
  if (!ok) return;

  processing.value = true;
  try {
    const url =
      decision.value === "approve"
        ? "/webApi/leaveflow/approve"
        : "/webApi/leaveflow/reject";
    await axios.post(url, payload);
    await proxy.$alert("처리되었습니다.");
    selected.value = null;
    await fnLoad();
    await fnLoadCounts();
  } catch (e) {
    await proxy.$alert(resolveApiErrorMessage(e, "결재 처리 중 오류가 발생했습니다."));
  } finally {
    processing.value = false;
  }
};

// 사업장 선택 변경 시 활성 탭 목록 + 배지 카운트 재조회(연차 탭은 필터 자체가 숨어있어 변경 불가).
watch(selectedSiteCd, () => {
  fnSearch();
});

// 접수함연차변경다중사업장확장-004: onMounted 프리필 중 아래 leaveChangeSiteCd watcher 의 중복
//   재조회를 억제한다(Attd_13.vue initializing 관례 미러 — 프리셋 부서값 클로버 방지).
const leaveChangeInitializing = ref(true);
// 사업장 watcher 가 cascade 로 nodeCd/incSubNodeYn 을 리셋할 때, 아래 두 번째 watcher 가 같은
//   재조회를 중복 트리거하지 않도록 억제하는 플래그(템플릿에 노출되지 않는 순수 제어값 — ref 불필요).
let suppressLeaveChangeReload = false;

// 사업장 필터 변경 시: 부서 목록 재조회 + 부서/하위부서 표시값 cascade 초기화 + 목록 재조회.
//   User_01.vue focusKill(사업장 값이 비면 nodeDisabled=true 로 부서도 함께 초기화)·Attd_13.vue
//   watch(siteCd)(사업장 변경 시 nodeCd 무조건 리셋) 패턴을 미러한다 — "전체 사업장"으로 되돌아갈 때뿐
//   아니라 다른 사업장으로 바뀔 때도 이전 사업장의 NODE_CD 를 들고 있으면 안 된다(§0-5 핵심 근거:
//   NODE_CD 는 사업장마다 재사용되는 코드라 사업장이 바뀌면 그 값 자체가 무의미해진다).
//   부서 select/하위부서 체크박스의 disabled 전환은 leaveChangeNodeSelectEnabled computed 가 자동 반영.
watch(leaveChangeSiteCd, async (siteCd) => {
  if (leaveChangeInitializing.value) return;
  suppressLeaveChangeReload = true;
  leaveChangeNodeCd.value = "";
  leaveChangeIncSubNodeYn.value = "Y"; // 기존 고정값(포함)으로 되돌림 — disabled 상태의 stale 값 방지
  await fnLoadLeaveChangeNodeList(siteCd);
  suppressLeaveChangeReload = false;
  await fnLoadLeaveChanges();
});

// 부서/하위부서 포함 변경 시 목록만 재조회(부서 목록 재조회 불필요). 사업장 watcher 의 cascade
//   리셋으로 인한 재호출은 suppressLeaveChangeReload 로 걸러(사업장 watcher 가 마지막에 1회만 재조회).
watch([leaveChangeNodeCd, leaveChangeIncSubNodeYn], () => {
  if (leaveChangeInitializing.value || suppressLeaveChangeReload) return;
  fnLoadLeaveChanges();
});

onMounted(async () => {
  // 접수함연차변경다중사업장확장-004: leaveChangeSiteCd/leaveChangeNodeCd 초기 프리필
  //   (master/hr: 빈 값 유지="전체" / 노드 관리자: gv_siteCd, gv_nodeCd 세션값 — 기존 동작과 동일
  //   결과가 나오도록). 프리필이 끝날 때까지 leaveChangeInitializing 을 유지해 위 watcher 들의
  //   중복 재조회를 막는다(최초 로드는 아래 fnLoad() 단일 경로로만 수행).
  if (!isMasterOrHr.value) {
    leaveChangeSiteCd.value = sessionStorage.getItem("gv_siteCd") ?? "";
    leaveChangeNodeCd.value = sessionStorage.getItem("gv_nodeCd") ?? "";
  }
  if (leaveChangeSiteCd.value) {
    await fnLoadLeaveChangeNodeList(leaveChangeSiteCd.value);
  }
  leaveChangeInitializing.value = false;

  fnLoad();
  fnLoadCounts();
  fnLoadAccessibleSites();
});
</script>

<style scoped>
/* 탭바 표준(Attd_01 .attd01-tab-bar/.attd01-tab-btn 스펙 준수 — 밑줄형 14px) */
.ra-tabs {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0 0;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
/* 내 처리 이력 버튼 — 탭바 우측 끝. 탭과 혼동되지 않게 외곽선 칩 형태. */
.ra-history-btn {
  margin-left: auto;
  border: 1px solid var(--color-border, #e5e7eb);
  background: none;
  color: var(--color-text-muted, #6b7280);
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-size: 12px;
  cursor: pointer;
}
.ra-history-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.ra-tab {
  padding: 0.5rem 1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
}
.ra-tab:hover {
  color: var(--color-text, #374151);
}
.ra-tab.active {
  font-weight: 600;
  color: var(--color-primary, #16a34a);
  border-bottom-color: var(--color-primary);
}
.ra-tab-badge {
  display: inline-block;
  min-width: 1.2rem;
  padding: 0 0.3rem;
  margin-left: 0.2rem;
  border-radius: 0.6rem;
  background: var(--color-primary, #30796a);
  color: #fff;
  font-size: 0.72rem;
}
.ra-body {
  display: flex;
  gap: 1rem;
  padding: 0.75rem;
  align-items: stretch;
}
.ra-inbox {
  flex: 0 0 58%;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  display: flex;
  flex-direction: column;
  min-height: 360px;
}
.ra-detail {
  flex: 1;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  padding: 0.75rem;
  background: var(--color-surface, #fff);
}
.ra-inbox__head {
  padding: 0.6rem 0.75rem;
  font-weight: 600;
  font-size: 0.9rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}
/* "연차 상신" 탭 ra-inbox 컬럼을 두 소섹션으로 세로 분할(B안).
   두 블록 모두 flex-basis 0으로 동일 비율 분배 → 고정 높이로 정확히 반반씩 차지(사용자 요청). */
.ra-inbox__block {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  min-height: 0;
}
.ra-inbox__block--leavechange {
  border-top: 1px solid var(--color-border, #e5e7eb);
}
.ra-list {
  flex: 1;
  overflow-y: auto;
}
.ra-row {
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  border-left: 4px solid transparent;
  cursor: pointer;
}
.ra-row:hover {
  background: var(--color-surface-hover, #f9fafb);
}
.ra-row.selected {
  border-left-color: var(--color-primary, #30796a);
  background: var(--color-primary-tint, #dcfce7);
}
.ra-row__main {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.ra-row__name {
  font-size: 0.9rem;
  color: var(--color-text, #111827);
}
.ra-row__dept {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
}
.ra-row__sub {
  font-size: 0.8rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 0.2rem;
}
.ra-chip.self {
  font-size: 0.7rem;
  background: var(--color-amber-tint, #fef3c7);
  color: var(--color-amber, #b45309);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
}
.ra-chip.type {
  font-size: 0.7rem;
  background: var(--color-primary-tint, #dcfce7);
  color: var(--color-primary, #30796a);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
}
/* 가불표시-03: 가불 배지 — 경고(warning) 시맨틱 토큰(tokens.css) 재사용, 신규 토큰 발행 없음 */
.ra-chip.borrow {
  font-size: 0.7rem;
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
}
/* prafta-leavemulti: 기간(From-To) 신청 묶음 배지 — 종류 배지(type)와 같은 primary 계열.
   "이 행은 여러 건이 접혀 있다"를 알리는 용도라 개수를 함께 표기한다. */
.ra-chip.group {
  font-size: 0.7rem;
  background: var(--color-primary-tint, #dcfce7);
  color: var(--color-primary, #30796a);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
  font-weight: 700;
}
/* 묶음 행 — 좌측 강조선으로 단건과 구분한다. */
.ra-row--group {
  border-left: 3px solid var(--color-primary, #30796a);
}
/* 펼치기 토글 — 행 선택과 충돌하지 않도록 클릭 전파를 막는다(@click.stop). */
.ra-group__toggle {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0 0.2rem;
  font-size: 0.8rem;
  color: var(--color-text-muted, #6b7280);
  font-family: inherit;
}
/* 펼친 개별 건 — 들여쓰기로 소속을 드러낸다. 동작은 단건과 완전히 동일. */
.ra-group__items {
  padding-left: 1.1rem;
}
.ra-row--child {
  border-left: 1px dashed var(--color-border, #e5e7eb);
}
/* 묶음 상세 — 대상 날짜 나열 */
.ra-group__datelist {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  margin-top: 0.4rem;
}
.ra-group__date {
  font-size: 0.72rem;
  padding: 0.08rem 0.35rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.3rem;
  color: var(--color-text-muted, #6b7280);
  white-space: nowrap;
}
.ra-group__note {
  font-size: 0.76rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0.4rem 0;
  line-height: 1.5;
}
/* 스케줄 수정 탭 — 부서 정보 결측(웹 처리 불가) 배지. 경고 시맨틱 토큰 재사용. */
.ra-chip.warn {
  font-size: 0.7rem;
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  border-radius: 0.3rem;
  padding: 0.05rem 0.3rem;
}
.ra-empty,
.ra-detail__empty {
  color: var(--color-text-muted, #9ca3af);
  font-size: 0.85rem;
  text-align: center;
  padding: 2rem 0;
}
.ra-sec {
  margin-bottom: 0.9rem;
}
.ra-sec__title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text, #111827);
  margin-bottom: 0.4rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
}
.ra-meta {
  display: grid;
  grid-template-columns: 5.5rem 1fr;
  gap: 0.3rem 0.5rem;
  font-size: 0.84rem;
  margin: 0;
}
.ra-meta dt {
  color: var(--color-text-muted, #6b7280);
}
.ra-meta dd {
  margin: 0;
  color: var(--color-text, #111827);
}
.ra-decide {
  border-top: 1px solid var(--color-border, #e5e7eb);
  padding-top: 0.6rem;
}
.ra-radio {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.3rem;
  cursor: pointer;
}
.ra-decide textarea {
  width: 100%;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: 0.35rem;
  padding: 0.35rem 0.5rem;
  font-size: 0.85rem;
  margin: 0.3rem 0;
}
.ra-decide__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.4rem;
}
/* 처리 버튼은 전역 디자인 시스템(button.css)의 .btn + .btn-primary 를 쓴다.
   구 .btn-process 는 이 화면 전용 커스텀이라 다른 화면과 크기·폰트·radius 가 어긋났다(2026-08-15 제거).
   ★.btn-primary 는 배경/글자색만 준다 — 크기·padding·radius 는 .btn 에 있으므로 항상 함께 붙인다. */

/* 반려 선택 시 위험색 변형. .btn-primary 는 hover/active 상태 규칙까지 배경을 지정하므로
   같은 상태 선택자로 맞춰 덮지 않으면 마우스를 올리는 순간 초록으로 되돌아간다. */
.btn-reject,
.btn-reject:hover:not(:disabled),
.btn-reject:active:not(:disabled) {
  background: var(--color-danger, #dc2626);
}
.ra-decide__note {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0 0 0.4rem;
  line-height: 1.4;
}
.ra-decide__warn {
  font-size: 0.78rem;
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #b45309);
  border-radius: 0.3rem;
  padding: 0.3rem 0.4rem;
  margin: 0 0 0.4rem;
  line-height: 1.4;
}
</style>
