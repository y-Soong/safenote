package com.prafta.common.cmm.dailycontract;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;

/**
 * 일용직 근로계약서 <b>서명 블록</b> 렌더 유틸 (R5 — 서버 합성).
 *
 * <p>멀티페이지(PDF) 지원 개편(P2)으로 역할을 <b>블록 PNG 생성기</b>로 축소했다. 기존 전체 세로 합성
 * ({@code compose})은 제거되고, 생성된 블록 PNG 는 {@link ContractPdfBuilder} 가 서명본 PDF 의
 * 마지막 페이지로 삽입한다. 블록 문안/순서/폰트/여백 계산은 기존과 100% 동일하게 유지한다.
 *
 * <p>블록 구성: 성명 / 최초 근로일(=서명일) / 계약 단위 "근로일 당일 1일" /
 * 서명일시(서버 NOW) / 서명 이미지. 미래 종료일은 어디에도 기재하지 않는다(D1).
 *
 * <p>한글 렌더링 폰트: 번들 Pretendard TTF({@code resources/fonts/}, OFL-1.1)를 {@code Font.createFont}
 * 로 로드한다 — 과거 논리 폰트 {@code SansSerif} 방식은 운영 Linux 에 한글 시스템 폰트가 없어
 * 서명 블록의 한글이 전탈락하는 결함을 냈다(2026-08-10 실기기 확인). 번들 로드 실패 시에만
 * {@code SansSerif} 폴백(로컬 Windows 는 기본 매핑으로 한글 렌더 가능).
 *
 * <p>도메인(일용직 계약서) 종속 유틸이므로 common.util 이 아닌 본 모듈에 둔다.
 */
@Slf4j
public final class ContractImageComposer {

    /** 합성 기준 최소 폭(px) — 원본이 지나치게 좁아도 블록 텍스트 가독성을 보장. */
    private static final int MIN_CANVAS_WIDTH = 640;

    /** 계약 단위 고정 문구 (D1 — 근로일 당일 1일 단위 계약). */
    private static final String CONTRACT_UNIT_LABEL = "근로일 당일 1일";

    /** 번들 본문 폰트(Pretendard Regular). 로드 실패 시 null → 논리 폰트 SansSerif 폴백. */
    private static final Font BUNDLED_REGULAR = loadBundledFont("/fonts/Pretendard-Regular.ttf");

    /** 번들 제목 폰트(Pretendard Bold — 자체 볼드 글리프라 derive 시 스타일은 PLAIN 유지). */
    private static final Font BUNDLED_BOLD = loadBundledFont("/fonts/Pretendard-Bold.ttf");

    private ContractImageComposer() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 클래스패스에서 TTF 를 로드한다. 실패해도 서명 흐름을 죽이지 않기 위해 null 을 반환하고
     * 렌더 시 논리 폰트로 폴백한다(운영 Linux 폴백은 한글 탈락 가능성이 있으므로 로드 실패는 로그로 남긴다).
     */
    private static Font loadBundledFont(String resourcePath) {
        try (java.io.InputStream in = ContractImageComposer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                log.error("계약서 서명 블록 번들 폰트 리소스를 찾지 못했습니다 - 경로={}", resourcePath);
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            log.error("계약서 서명 블록 번들 폰트 로드 실패 - 경로={}, 원인={}", resourcePath, e.toString());
            return null;
        }
    }

    /** 제목/본문 폰트 해석 — 번들 폰트 우선, 실패 시 논리 폰트 SansSerif. */
    private static Font resolveFont(boolean bold, int fontSize) {
        Font bundled = bold ? BUNDLED_BOLD : BUNDLED_REGULAR;
        if (bundled != null) {
            // Bold TTF 는 서체 자체가 볼드 → 알고리즘 볼드 중복 방지 위해 PLAIN 으로 derive.
            return bundled.deriveFont(Font.PLAIN, (float) fontSize);
        }
        return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, fontSize);
    }

    /**
     * 자동 계약정보 블록 + 서명 이미지를 단독 PNG 로 렌더한다(서명본 PDF 마지막 페이지 재료 — P2).
     *
     * <p>문안/순서/폰트/여백 계산은 기존 전체 합성({@code compose})의 블록 영역과 문자 단위로 동일하다.
     * 렌더 폭은 삽입될 PDF 페이지 폭에 맞춰 호출부가 결정한다
     * ({@code ContractPdfBuilder.resolveSignBlockRenderWidthPx} — 150DPI 정합, 최소 640px).
     *
     * @param targetWidthPx      블록 렌더 폭(px). 640 미만이면 640 으로 보정
     * @param signImage          서명 PNG 디코딩 결과(호출부에서 검증 완료된 BufferedImage)
     * @param userNm             성명(서명 시점 스냅샷)
     * @param firstWorkDateLabel 최초 근로일 표시 문자열(예: 2026-07-16)
     * @param signDtimeLabel     서명일시 표시 문자열(서버 시각, 예: 2026-07-16 08:12:33)
     * @return 블록 PNG 바이트
     * @throws java.io.IOException PNG 인코딩 실패 시(호출부에서 에러코드 매핑)
     */
    public static byte[] renderSignBlock(
            int targetWidthPx
            , BufferedImage signImage
            , String userNm
            , String firstWorkDateLabel
            , String signDtimeLabel) throws java.io.IOException {

        int width = Math.max(targetWidthPx, MIN_CANVAS_WIDTH);

        // 폭 비례 스케일 — 기준폭 640px 에서 본문 16px.
        int fontSize = Math.max(14, width / 40);
        int padding = fontSize;
        int lineGap = Math.round(fontSize * 1.7f);

        Font titleFont = resolveFont(true, fontSize);
        Font bodyFont = resolveFont(false, fontSize);

        // 서명 이미지 표시 크기 — 폭의 1/3 상한, 비율 유지.
        int signMaxW = width / 3;
        int signW = Math.min(signImage.getWidth(), signMaxW);
        int signH = Math.round((float) signImage.getHeight() * signW / Math.max(1, signImage.getWidth()));

        // 블록 높이 = 상단 여백 + 제목 + 본문 3행 + 서명 라벨 행 + 서명 이미지 + 하단 여백.
        int blockHeight = padding + lineGap /* 제목 */
                + lineGap * 3               /* 성명/최초 근로일+계약 단위/서명일시 */
                + lineGap                   /* 서명 라벨 */
                + signH + padding;

        BufferedImage merged = new BufferedImage(width, blockHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = merged.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 배경 백색.
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, blockHeight);

            // 블록 상단 구분선(기존 합성본의 원본/블록 경계선을 페이지 상단선으로 계승).
            int blockTop = 0;
            g.setColor(new Color(0x33, 0x33, 0x33));
            float strokeWidth = Math.max(1f, fontSize / 12f);
            g.setStroke(new BasicStroke(strokeWidth));
            int lineY = Math.max(1, Math.round(strokeWidth / 2f));
            g.drawLine(padding, lineY, width - padding, lineY);

            int textX = padding;
            int y = blockTop + padding + fontSize;

            // 제목.
            g.setFont(titleFont);
            g.setColor(Color.BLACK);
            g.drawString("▣ 계약 정보 (시스템 자동 생성)", textX, y);
            y += lineGap;

            // 본문.
            g.setFont(bodyFont);
            g.drawString("성명: " + safe(userNm), textX, y);
            y += lineGap;
            g.drawString("최초 근로일: " + safe(firstWorkDateLabel) + "    계약 단위: " + CONTRACT_UNIT_LABEL, textX, y);
            y += lineGap;
            g.drawString("서명일시: " + safe(signDtimeLabel), textX, y);
            y += lineGap;

            // 서명 라벨 + 서명 이미지.
            g.drawString("서명:", textX, y);
            int signY = y - fontSize + Math.round(fontSize * 0.3f);
            int signX = textX + g.getFontMetrics().stringWidth("서명:  ");
            g.drawImage(signImage, signX, signY, signW, signH, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(merged, "png", baos);
        return baos.toByteArray();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
