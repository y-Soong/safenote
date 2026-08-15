package com.prafta.web.user.user01.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 엑셀 서식 유실 복원/검증 규칙 테스트 (통합테스트 결함 2026-08-15).
 *
 * <p>핵심 회귀 방지 지점:
 * <ul>
 *   <li>이동전화 전제 규칙(10자리면 0 붙이기)로 되돌아가지 않을 것 — 유선번호에서 깨진다</li>
 *   <li>0 으로 시작하지 않는 대표번호에 0 을 붙이지 않을 것</li>
 *   <li>한글/영문이 섞인 값을 숫자만 남겨 "멀쩡한 번호"로 둔갑시키지 않을 것</li>
 * </ul>
 */
class UserExcelValueRestorerTest {

    private List<UserExcelValueRestorer.Adjustment> sink() {
        return new ArrayList<>();
    }

    // ── 휴대폰 복원 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("앞자리 0 이 떨어진 이동전화를 복원한다")
    void restorePhone_mobile() {
        List<UserExcelValueRestorer.Adjustment> adj = sink();

        assertThat(UserExcelValueRestorer.restorePhone("1077635257", 4, adj))
                .isEqualTo("01077635257");

        assertThat(adj).hasSize(1);
        assertThat(adj.get(0).rowNo()).isEqualTo(4);
        assertThat(adj.get(0).columnNm()).isEqualTo("휴대폰번호");
        assertThat(adj.get(0).before()).isEqualTo("1077635257");
        assertThat(adj.get(0).after()).isEqualTo("01077635257");
    }

    @Test
    @DisplayName("이미 정상인 번호는 손대지 않고 보정 기록도 남기지 않는다")
    void restorePhone_alreadyValid() {
        List<UserExcelValueRestorer.Adjustment> adj = sink();

        assertThat(UserExcelValueRestorer.restorePhone("01077635257", 4, adj))
                .isEqualTo("01077635257");
        assertThat(adj).isEmpty();
    }

    @Test
    @DisplayName("하이픈은 제거하되 보정으로 기록하지 않는다")
    void restorePhone_stripsSeparators() {
        List<UserExcelValueRestorer.Adjustment> adj = sink();

        assertThat(UserExcelValueRestorer.restorePhone("010-7763-5257", 4, adj))
                .isEqualTo("01077635257");
        assertThat(adj).isEmpty();
    }

    @Test
    @DisplayName("유선번호(서울·지역)도 같은 규칙으로 복원된다 — 이동전화 전제 규칙이면 깨지는 지점")
    void restorePhone_landline() {
        assertThat(UserExcelValueRestorer.restorePhone("212345678", 4, sink()))
                .isEqualTo("0212345678");   // 02-1234-5678
        assertThat(UserExcelValueRestorer.restorePhone("315551234", 4, sink()))
                .isEqualTo("0315551234");   // 031-555-1234
    }

    @Test
    @DisplayName("0 으로 시작하지 않는 대표번호에는 0 을 붙이지 않는다")
    void restorePhone_serviceNumberUntouched() {
        assertThat(UserExcelValueRestorer.restorePhone("15887000", 4, sink()))
                .isEqualTo("15887000");
    }

    @Test
    @DisplayName("한글·영문이 섞이면 복원하지 않고 원본을 넘긴다(숫자만 남겨 둔갑시키지 않는다)")
    void restorePhone_nonDigitKept() {
        assertThat(UserExcelValueRestorer.restorePhone("김철수01012345678", 4, sink()))
                .isEqualTo("김철수01012345678");
        assertThat(UserExcelValueRestorer.restorePhone("abc", 4, sink()))
                .isEqualTo("abc");
    }

    @Test
    @DisplayName("빈 값은 null 로 돌려준다(필수 검증이 판정)")
    void restorePhone_blank() {
        assertThat(UserExcelValueRestorer.restorePhone(null, 4, sink())).isNull();
        assertThat(UserExcelValueRestorer.restorePhone("   ", 4, sink())).isNull();
    }

    // ── 이동전화 판정 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("이동전화만 통과시킨다 — 자릿수만 보던 종전 검증의 구멍을 막는다")
    void isKrMobile() {
        assertThat(UserExcelValueRestorer.isKrMobile("01077635257")).isTrue();
        assertThat(UserExcelValueRestorer.isKrMobile("0111234567")).isTrue();

        // 앞자리 0 이 빠진 값 — 10자리라 종전 자릿수 검증(10~11)은 통과했었다.
        assertThat(UserExcelValueRestorer.isKrMobile("1077635257")).isFalse();
        // 유선번호는 형식은 유효하지만 이동전화가 아니므로 거부 대상.
        assertThat(UserExcelValueRestorer.isKrMobile("0212345678")).isFalse();
        assertThat(UserExcelValueRestorer.isKrMobile("0315551234")).isFalse();
        assertThat(UserExcelValueRestorer.isKrMobile(null)).isFalse();
    }

    // ── 생년월일 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("6자리 미만 생년월일의 앞자리 0 을 복원한다")
    void restoreBirth_pads() {
        List<UserExcelValueRestorer.Adjustment> adj = sink();

        assertThat(UserExcelValueRestorer.restoreBirth("50101", 5, adj)).isEqualTo("050101");
        assertThat(adj).hasSize(1);
        assertThat(adj.get(0).columnNm()).isEqualTo("생년월일");
    }

    @Test
    @DisplayName("정상 6자리는 손대지 않는다")
    void restoreBirth_untouched() {
        List<UserExcelValueRestorer.Adjustment> adj = sink();

        assertThat(UserExcelValueRestorer.restoreBirth("900101", 5, adj)).isEqualTo("900101");
        assertThat(adj).isEmpty();
    }

    @Test
    @DisplayName("실제 달력상 존재하는 날짜만 통과시킨다")
    void isValidBirth() {
        assertThat(UserExcelValueRestorer.isValidBirth("900101")).isTrue();
        assertThat(UserExcelValueRestorer.isValidBirth("19900101")).isTrue();
        // 2/29 는 세기에 따라 갈리므로 19YY·20YY 중 하나라도 유효하면 통과.
        assertThat(UserExcelValueRestorer.isValidBirth("000229")).isTrue();   // 2000-02-29

        assertThat(UserExcelValueRestorer.isValidBirth("260231")).isFalse();  // 2월 31일
        assertThat(UserExcelValueRestorer.isValidBirth("901301")).isFalse();  // 13월
        assertThat(UserExcelValueRestorer.isValidBirth("9001")).isFalse();    // 자릿수 부족
        assertThat(UserExcelValueRestorer.isValidBirth("abc")).isFalse();
        assertThat(UserExcelValueRestorer.isValidBirth(null)).isFalse();
    }
}
