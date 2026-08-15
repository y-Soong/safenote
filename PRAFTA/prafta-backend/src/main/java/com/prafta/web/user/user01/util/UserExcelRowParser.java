package com.prafta.web.user.user01.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.dto.request.UserCreateRequest;

/**
 * PRAFTA-036 — 사용자 일괄 생성 엑셀(.xlsx) 행 파서.
 *
 * <p>{@link UserExcelTemplateBuilder} 가 생성한 양식과 1:1 대응한다(헤더 순서 동일).
 * 파서는 1행(안내) / 2행(헤더) / 3행(예시) 을 모두 skip 하고 4행(인덱스 3) 부터 데이터로 간주한다.
 * 빈 행이 나오면 시트 종료로 간주하여 더 이상 읽지 않는다.
 */
public final class UserExcelRowParser {

    private UserExcelRowParser() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 데이터 행 시작 인덱스(0-based). 1행=0, 2행=1, 3행=2, 4행=3. */
    public static final int DATA_START_ROW_INDEX = 3;

    /** 헤더 정의 — 컬럼 순서가 곧 파서 인덱스. {@link UserExcelTemplateBuilder#EXAMPLE_ROW} 와 동일 순서.
     *  F-13 확장: 계약종료일·경력인정사유유형·고용형태 컬럼 제거(단건 생성 팝업과 동일 정책 —
     *  고용형태는 REGULAR 고정, 일용직은 QR/일용직 가입 별도 경로 전용).
     *  소정-03 확장: 14번째 "주소정근로시간(시간)" 컬럼 추가(필수). 계정 생성 3경로 전부에서
     *  소정근로시간을 받는다는 지시서 확정에 따른 것으로, 값이 비면 그 행은 생성되지 않는다
     *  (구 13컬럼 양식으로 업로드하면 전 행이 "소정근로시간 누락" 사유로 실패 목록에 담긴다). */
    public static final String[] HEADERS = new String[] {
            "사용자ID(필수)"
            , "사용자명(필수)"
            , "권한코드(필수)"
            , "사업장번호(필수)"
            , "소속부서코드(필수)"
            , "휴대폰번호(필수)"
            , "이메일"
            , "성별(M/F)"
            , "생년월일(YYMMDD)"
            , "직급코드"
            , "입사일(YYYYMMDD)"
            , "경력인정개월수"
            , "상세 설명"
            , "주소정근로시간(필수,시간)"
    };

    /** 소정근로시간 컬럼 인덱스(시간 단위 입력 → 분으로 환산해 서버에 전달). */
    private static final int COL_IDX_STD_WORK_HOURS = 13;

    /** 시간 → 분 환산 계수. */
    private static final int MINUTES_PER_HOUR = 60;

    /** 주 소정근로 분을 직접 입력하는 유형(User01ServiceImpl 의 STD_WORK_TYPE_DIRECT 와 동일 규약). */
    private static final String STD_WORK_TYPE_DIRECT = "DIRECT";

    /**
     * 시트의 데이터 영역을 {@link UserCreateParam} 리스트로 파싱한다.
     * 빈 행이 나오면 시트 종료로 간주(짧은 양식 + 누락 행 혼동 방지).
     *
     * @param sheet 워크북 시트 0
     * @param tokenInfo 회사 스코프/입력자/요청자 권한 (각 행 동일)
     * @return 파싱된 UserCreateParam 리스트 (헤더/예시/빈 행 제외)
     */
    public static List<UserCreateParam> parse(Sheet sheet, TokenInfo tokenInfo) {
        return parse(sheet, tokenInfo, null);
    }

    /**
     * 위 {@link #parse(Sheet, TokenInfo)} 와 동일하되, 서식 유실 보정 내역을 함께 수집한다.
     *
     * @param adjustments 보정 내역 수집처(null 허용) — 앞자리 0 복원이 일어난 행/컬럼/전후값
     */
    public static List<UserCreateParam> parse(
            Sheet sheet, TokenInfo tokenInfo, List<UserExcelValueRestorer.Adjustment> adjustments) {

        List<UserCreateParam> result = new ArrayList<>();

        int lastRowIdx = sheet.getLastRowNum();
        for (int rowIdx = DATA_START_ROW_INDEX; rowIdx <= lastRowIdx; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                // 중간 빈 행도 종료 시그널로 간주.
                break;
            }
            if (isEmptyRow(row)) {
                break;
            }

            UserCreateRequest req = new UserCreateRequest();
            req.setUserId(strAt(row, 0));
            req.setUserNm(strAt(row, 1));
            req.setAuthCd(strAt(row, 2));
            req.setSiteNo(strAt(row, 3));
            req.setNodeCd(strAt(row, 4));
            // ★서식 유실 복원 — 셀 서식이 텍스트가 아니면 엑셀이 맨 앞 0 하나를 떨어뜨린다.
            //   01077635257 → 1077635257 (휴대폰 자릿수 하한 10 을 그대로 통과해 잘못 저장됐다)
            //   050101      → 50101
            //   복원 불가한 값은 손대지 않고 넘겨 서비스 검증에서 형식 오류로 떨어뜨린다.
            //   rowNo 는 사용자가 화면에서 보는 번호(1-based).
            int rowNo = rowIdx + 1;
            req.setMblNo(UserExcelValueRestorer.restorePhone(strAt(row, 5), rowNo, adjustments));
            req.setEmail(strAt(row, 6));
            req.setGender(strAt(row, 7));
            req.setBirthDt(UserExcelValueRestorer.restoreBirth(strAt(row, 8), rowNo, adjustments));
            req.setRankCd(strAt(row, 9));
            req.setHireDate(strAt(row, 10));
            // 고용형태는 단건 생성 팝업(PRAFTA_COM_003-B 3.1.4)과 동일하게 REGULAR 고정.
            // 계약종료일·경력인정사유유형은 양식에서 제거됨 — 미설정(null) 유지.
            req.setEmploymentType("REGULAR");
            req.setCreditMonths(intAt(row, 11));
            req.setCreditReasonDetail(strAt(row, 12));

            // 소정-03: 주 소정근로시간(시간) → 분 환산. 사유 컬럼은 두지 않고(단축 사유는 기간이
            //   필수라 엑셀로 받을 수 없다) 서버가 회사 통상 기준값과 비교해 통상/단시간계약을 판정한다.
            //   값이 비어 있으면 유형을 세팅하지 않아 서비스 검증에서 행 오류(필수 누락)로 떨어진다.
            Integer stdWorkWeekMinutes = weekMinutesAt(row, COL_IDX_STD_WORK_HOURS);
            if (stdWorkWeekMinutes != null) {
                req.setStdWorkType(STD_WORK_TYPE_DIRECT);
                req.setStdWorkWeekMinutes(stdWorkWeekMinutes);
            }

            result.add(UserCreateParam.from(req, tokenInfo));
        }
        return result;
    }

    /**
     * prafta-052 — 실패 행 재업로드용 원본 행(양식 14컬럼 순서)으로 변환한다.
     * {@link #HEADERS} 순서와 1:1 일치해야 한다(시트1 재업로드 호환의 핵심).
     * creditMonths(Integer)는 문자열로, null 은 빈 문자열로 정규화한다.
     * 주 소정근로 분은 입력 단위(시간)로 되돌려 담는다(소정-03).
     * additionalSiteCdList/gv* 토큰 클레임은 양식 컬럼이 아니므로 포함하지 않는다.
     *
     * @param p 실패한 행의 생성 파라미터(null 이면 빈 리스트)
     * @return 양식 14컬럼 순서의 문자열 리스트(시트1에 그대로 펼침)
     */
    public static List<String> toSourceRow(UserCreateParam p) {
        if (p == null) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(
                nz(p.userId())            // 0  사용자ID(필수)
              , nz(p.userNm())            // 1  사용자명(필수)
              , nz(p.authCd())            // 2  권한코드(필수)
              , nz(p.siteNo())            // 3  사업장번호(필수)
              , nz(p.nodeCd())            // 4  소속부서코드(필수)
              , nz(p.mblNo())             // 5  휴대폰번호(필수)
              , nz(p.email())             // 6  이메일
              , nz(p.gender())            // 7  성별(M/F)
              , nz(p.birthDt())           // 8  생년월일(YYMMDD)
              , nz(p.rankCd())            // 9  직급코드
              , nz(p.hireDate())          // 10 입사일(YYYYMMDD)
              , p.creditMonths() == null ? "" : String.valueOf(p.creditMonths()) // 11 경력인정개월수
              , nz(p.creditReasonDetail())// 12 상세 설명(경력인정)
              , hoursText(p.stdWorkWeekMinutes()) // 13 주소정근로시간(시간)
        );
    }

    /**
     * 소정-03 — 주 소정근로 분을 양식 입력 단위(시간) 문자열로 되돌린다.
     * 정수 시간이면 소수점을 붙이지 않는다(40 / 20.5).
     */
    private static String hoursText(Integer weekStdMinutes) {
        if (weekStdMinutes == null) {
            return "";
        }
        if (weekStdMinutes % MINUTES_PER_HOUR == 0) {
            return Integer.toString(weekStdMinutes / MINUTES_PER_HOUR);
        }
        return Double.toString(weekStdMinutes / (double) MINUTES_PER_HOUR);
    }

    /** null 을 빈 문자열로 정규화. */
    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** 행 전체가 비어있는지(모든 셀이 null/blank) 판정. */
    private static boolean isEmptyRow(Row row) {
        short last = row.getLastCellNum();
        if (last <= 0) return true;
        for (int c = 0; c < last; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String v = readCellAsString(cell);
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** 셀 값을 문자열로 읽되 null/blank 면 null 반환(전체 trim). */
    private static String strAt(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        String v = readCellAsString(cell);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    /** 셀 값을 Integer 로 읽되 빈 값/파싱 실패 시 null 반환(서비스 검증에서 잡힘). */
    private static Integer intAt(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            String v = readCellAsString(cell);
            if (v == null || v.isBlank()) return null;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 소정-03 — 주 소정근로시간(시간 단위) 셀을 읽어 <b>분</b>으로 환산한다.
     *
     * <p>0.5시간 단위 계약(예: 37.5시간)을 지원하기 위해 소수 입력을 허용하고, 분 미만은
     * 반올림한다. 빈 값/숫자 아님/0 이하는 null 을 반환해 서비스 검증(필수 누락·값 범위)이
     * 행 오류로 처리하게 둔다 — 파서가 임의 기본값을 채우면 잘못된 계약량이 조용히 저장된다.
     */
    private static Integer weekMinutesAt(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        try {
            double hours;
            if (cell.getCellType() == CellType.NUMERIC) {
                hours = cell.getNumericCellValue();
            } else {
                String v = readCellAsString(cell);
                if (v == null || v.isBlank()) return null;
                hours = Double.parseDouble(v.trim());
            }
            if (!(hours > 0)) return null;
            return (int) Math.round(hours * MINUTES_PER_HOUR);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 셀 타입에 맞춰 문자열로 읽는다.
     * - NUMERIC: 날짜면 8자리(YYYYMMDD), 정수면 소수점 제거.
     * - BOOLEAN/FORMULA/STRING/BLANK 모두 안전 처리.
     */
    private static String readCellAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date d = cell.getDateCellValue();
                    if (d == null) return null;
                    return new java.text.SimpleDateFormat("yyyyMMdd").format(d);
                }
                double n = cell.getNumericCellValue();
                if (n == Math.floor(n) && !Double.isInfinite(n)) {
                    return Long.toString((long) n);
                }
                return Double.toString(n);
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    try {
                        double v = cell.getNumericCellValue();
                        if (v == Math.floor(v)) return Long.toString((long) v);
                        return Double.toString(v);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return null;
        }
    }
}
