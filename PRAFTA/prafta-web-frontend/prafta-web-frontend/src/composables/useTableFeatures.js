import { ref, computed } from "vue";

/**
 * 테이블 정렬 상태 관리
 *
 * 클릭 순서: 비정렬 → 오름차순 → 내림차순 → 비정렬 (반복)
 *
 * @param {Ref<Array>} dataRef - 정렬 대상 원본 배열 ref
 * @returns {{ sortKey, sortOrder, sortedData, onSort }}
 */
export function useTableSort(dataRef) {
  const sortKey = ref(null);
  const sortOrder = ref(null); // null | 'asc' | 'desc'

  const sortedData = computed(() => {
    if (!sortKey.value || !sortOrder.value) return dataRef.value;
    return [...dataRef.value].sort((a, b) => {
      const va = String(a[sortKey.value] ?? "");
      const vb = String(b[sortKey.value] ?? "");
      const cmp = va.localeCompare(vb, "ko", {
        numeric: true,
        sensitivity: "base",
      });
      return sortOrder.value === "asc" ? cmp : -cmp;
    });
  });

  const onSort = (key) => {
    if (sortKey.value !== key) {
      sortKey.value = key;
      sortOrder.value = "asc";
    } else if (sortOrder.value === "asc") {
      sortOrder.value = "desc";
    } else {
      sortKey.value = null;
      sortOrder.value = null;
    }
  };

  return { sortKey, sortOrder, sortedData, onSort };
}

/**
 * 컬럼 너비 리사이즈 상태 관리
 *
 * @param {Object} initialWidths - 컬럼 키: 초기 너비(px) 맵  ex) { userId: 110, userNm: 120 }
 * @returns {{ colWidths, onResize }}
 */
export function useColumnResize(initialWidths = {}) {
  const colWidths = ref({ ...initialWidths });

  const onResize = ({ key, width }) => {
    colWidths.value = { ...colWidths.value, [key]: width };
  };

  return { colWidths, onResize };
}
