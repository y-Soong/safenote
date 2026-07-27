package com.prafta.common.cmm.dailycontract;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

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
 * <p>한글 렌더링 폰트: 논리 폰트 {@code SansSerif} 를 사용한다 — Windows 는 기본 매핑으로 한글이
 * 렌더되고, Linux 서버는 한글 폰트 패키지(예: fonts-nanum / noto-cjk) 설치가 전제된다.
 * 프로젝트 내 번들 폰트 리소스·기존 텍스트 렌더 전례가 없어(전수 grep 확인) 논리 폰트를 채택했다.
 *
 * <p>도메인(일용직 계약서) 종속 유틸이므로 common.util 이 아닌 본 모듈에 둔다.
 */
public final class ContractImageComposer {

    /** 합성 기준 최소 폭(px) — 원본이 지나치게 좁아도 블록 텍스트 가독성을 보장. */
    private static final int MIN_CANVAS_WIDTH = 640;

    /** 계약 단위 고정 문구 (D1 — 근로일 당일 1일 단위 계약). */
    private static final String CONTRACT_UNIT_LABEL = "근로일 당일 1일";

    private ContractImageComposer() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
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

        Font titleFont = new Font("SansSerif", Font.BOLD, fontSize);
        Font bodyFont = new Font("SansSerif", Font.PLAIN, fontSize);

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
