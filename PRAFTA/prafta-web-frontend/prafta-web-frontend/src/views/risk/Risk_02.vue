<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
    />

    <div class="viewBody">
      <!-- 5x4 위험성 추정기준 매트릭스 -->
      <div class="table-wrapper subtitle-pane">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="12">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">위험성 측정기준</span>
        </div>

        <div
          class="table-box"
          style="--box-h: auto; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid risk-matrix">
            <thead>
              <tr>
                <th rowspan="2" style="width: 12%">가능성 (빈도)</th>
                <th colspan="4" style="text-align: center">중대성(강도)</th>
              </tr>
              <tr>
                <th style="text-align: center">최대(4)</th>
                <th style="text-align: center">대(3)</th>
                <th style="text-align: center">중(2)</th>
                <th style="text-align: center">소(1)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="possibility-cell">최상(5)</td>
                <td :class="getRiskCellClass(20)">매우높음(20)</td>
                <td :class="getRiskCellClass(15)">높음(15)</td>
                <td :class="getRiskCellClass(10)">약간높음(10)</td>
                <td :class="getRiskCellClass(5)">낮음(5)</td>
              </tr>
              <tr>
                <td class="possibility-cell">상(4)</td>
                <td :class="getRiskCellClass(16)">매우높음(16)</td>
                <td :class="getRiskCellClass(12)">약간높음(12)</td>
                <td :class="getRiskCellClass(8)">보통(8)</td>
                <td :class="getRiskCellClass(4)">낮음(4)</td>
              </tr>
              <tr>
                <td class="possibility-cell">중(3)</td>
                <td :class="getRiskCellClass(12)">약간높음(12)</td>
                <td :class="getRiskCellClass(9)">약간높음(9)</td>
                <td :class="getRiskCellClass(6)">낮음(6)</td>
                <td :class="getRiskCellClass(3)">매우낮음(3)</td>
              </tr>
              <tr>
                <td class="possibility-cell">하(2)</td>
                <td :class="getRiskCellClass(8)">보통(8)</td>
                <td :class="getRiskCellClass(6)">낮음(6)</td>
                <td :class="getRiskCellClass(4)">낮음(4)</td>
                <td :class="getRiskCellClass(2)">매우낮음(2)</td>
              </tr>
              <tr>
                <td class="possibility-cell">최하(1)</td>
                <td :class="getRiskCellClass(4)">낮음(4)</td>
                <td :class="getRiskCellClass(3)">매우낮음(3)</td>
                <td :class="getRiskCellClass(2)">매우낮음(2)</td>
                <td :class="getRiskCellClass(1)">매우낮음(1)</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 관리기준 테이블 -->
      <div class="table-wrapper subtitle-pane" style="margin-top: 1.5rem">
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="10">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">관리기준</span>
        </div>

        <div
          class="table-box"
          style="--box-h: auto; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid management-table">
            <thead>
              <tr>
                <th style="width: 20%">위험성 수준</th>
                <th>관리기준</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td :class="getManagementCellClass(1, 3)">
                  1 ~ 3<br />매우낮음
                </td>
                <td>현재의 안전대책 유지</td>
              </tr>
              <tr>
                <td :class="getManagementCellClass(4, 6)">4 ~ 6<br />낮음</td>
                <td>안전정보 및 주기적 안전보건교육의 제공이 필요한 위험</td>
              </tr>
              <tr>
                <td :class="getManagementCellClass(8, 8)">7 ~ 8<br />보통</td>
                <td>
                  정비, 보수기간전에 안전보건 대책을 수립하고 개선해야 할 위험
                </td>
              </tr>
              <tr>
                <td :class="getManagementCellClass(9, 12)">
                  9 ~ 12<br />약간높음
                </td>
                <td>즉시개선</td>
              </tr>
              <tr>
                <td :class="getManagementCellClass(15, 15)">
                  13 ~ 15<br />높음
                </td>
                <td>
                  긴급 임시안전보건대책을 세운 후 작업 실시하고 정비, 보수기간
                  전에 안전보건 대책을 수립하고 개선해야 할 위험
                </td>
              </tr>
              <tr>
                <td :class="getManagementCellClass(16, 20)">
                  16 ~ 20<br />매우높음
                </td>
                <td>
                  즉시 작업중지 (작업을 지속하려면 즉시개선을 실행하는 위험)
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from 'vue';
import ViewHeader from '@/components/common/ViewHeader.vue';

defineOptions({ name: 'Risk_02' });

const props = defineProps({
  title: String,
  buttons: Object,
});

const { proxy } = getCurrentInstance();

const localButtons = ref({ ...props.buttons });

onMounted(() => {
  fnButtonControll();
});

const fnButtonControll = () => {
  localButtons.value.search = 'N';
  localButtons.value.save = 'N';
  localButtons.value.create = 'N';
  localButtons.value.delete = 'N';
  localButtons.value.excel = 'N';
};

// 위험도 점수에 따른 셀 클래스 반환
const getRiskCellClass = (score) => {
  if (score >= 16) return 'risk-cell risk-very-high';
  if (score >= 15) return 'risk-cell risk-high';
  if (score >= 9) return 'risk-cell risk-slightly-high';
  if (score >= 8) return 'risk-cell risk-normal';
  if (score >= 4) return 'risk-cell risk-low';
  return 'risk-cell risk-very-low';
};

// 관리기준 테이블의 위험성 수준 셀 클래스 반환
const getManagementCellClass = (min, max) => {
  if (max >= 16) return 'management-cell risk-very-high';
  if (max >= 15) return 'management-cell risk-high';
  if (max >= 9) return 'management-cell risk-slightly-high';
  if (max >= 8) return 'management-cell risk-normal';
  if (max >= 4) return 'management-cell risk-low';
  return 'management-cell risk-very-low';
};
</script>

<style scoped>
/* 테이블 wrapper 크기 조정 */
.table-wrapper {
  width: 70%;
}

.risk-matrix {
  width: 100%;
  table-layout: fixed;
}

.risk-matrix th,
.risk-matrix td {
  text-align: center;
  padding: 0.75rem 0.5rem;
  font-weight: 500;
}

.possibility-cell {
  background-color: var(--thead-bg);
  font-weight: 600;
}

.risk-cell {
  font-weight: 600;
  color: #1f1e1e;
}

/* 위험도별 색상 */
.risk-very-high {
  background-color: #ff4444; /* 빨간색 */
  color: #fff;
}

.risk-high {
  background-color: #ff8800; /* 주황색 */
  color: #fff;
}

.risk-slightly-high {
  background-color: #ffaa00; /* 노란색-주황색 */
  color: #1f1e1e;
}

.risk-normal {
  background-color: #ffd700; /* 노란색 */
  color: #1f1e1e;
}

.risk-low {
  background-color: #90ee90; /* 연한 초록색 */
  color: #1f1e1e;
}

.risk-very-low {
  background-color: #228b22; /* 진한 초록색 */
  color: #fff;
}

.management-table {
  width: 100%;
  table-layout: fixed;
}

.management-table th,
.management-table td {
  padding: 0.25rem 0.5rem;
  vertical-align: middle;
}

.management-table td:first-child {
  text-align: center;
  font-weight: 600;
  white-space: normal;
  word-break: break-word;
}

.management-table td:last-child {
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
}

.management-cell {
  color: #1f1e1e;
}

.management-cell.risk-very-high {
  background-color: #ff4444;
  color: #fff;
}

.management-cell.risk-high {
  background-color: #ff8800;
  color: #fff;
}

.management-cell.risk-slightly-high {
  background-color: #ffaa00;
  color: #1f1e1e;
}

.management-cell.risk-normal {
  background-color: #ffd700;
  color: #1f1e1e;
}

.management-cell.risk-low {
  background-color: #90ee90;
  color: #1f1e1e;
}

.management-cell.risk-very-low {
  background-color: #228b22;
  color: #fff;
}

/* 테이블 박스 높이 자동 조정 */
.table-box[style*='--box-h: auto'] {
  height: auto;
  min-height: auto;
}
</style>
