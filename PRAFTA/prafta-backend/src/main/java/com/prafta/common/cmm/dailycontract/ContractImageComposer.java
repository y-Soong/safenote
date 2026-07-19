package com.prafta.common.cmm.dailycontract;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

/**
 * 일용직 근로계약서 합성 유틸 (R5 — 서버 합성).
 *
 * <p>계약서 원본 이미지 하단에 "계약 정보(시스템 자동 생성)" 블록(요청서 §4-3 레이아웃)을 덧붙여
 * 단일 PNG 로 합성한다. 블록 구성: 성명 / 최초 근로일(=서명일) / 계약 단위 "근로일 당일 1일" /
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
     * 계약서 원본 + 자동 계약정보 블록 + 서명 이미지를 세로로 합성해 PNG 바이트를 반환한다.
     *
     * @param contractImageBytes 계약서 원본 이미지 바이트(png/jpg — ImageIO 디코딩 가능해야 함)
     * @param signImage          서명 PNG 디코딩 결과(호출부에서 검증 완료된 BufferedImage)
     * @param userNm             성명(서명 시점 스냅샷)
     * @param firstWorkDateLabel 최초 근로일 표시 문자열(예: 2026-07-16)
     * @param signDtimeLabel     서명일시 표시 문자열(서버 시각, 예: 2026-07-16 08:12:33)
     * @return 합성본 PNG 바이트
     * @throws java.io.IOException 원본 디코딩/합성본 인코딩 실패 시(호출부에서 에러코드 매핑)
     */
    public static byte[] compose(
            byte[] contractImageBytes
            , BufferedImage signImage
            , String userNm
            , String firstWorkDateLabel
            , String signDtimeLabel) throws java.io.IOException {

        BufferedImage contract = ImageIO.read(new ByteArrayInputStream(contractImageBytes));
        if (contract == null) {
            throw new java.io.IOException("계약서 원본 이미지를 디코딩할 수 없습니다.");
        }

        int width = Math.max(contract.getWidth(), MIN_CANVAS_WIDTH);

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

        int contractDrawX = (width - contract.getWidth()) / 2;
        int totalHeight = contract.getHeight() + blockHeight;

        BufferedImage merged = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = merged.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 배경 백색 + 계약서 원본(상단, 수평 중앙).
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, totalHeight);
            g.drawImage(contract, contractDrawX, 0, null);

            // 블록 상단 구분선.
            int blockTop = contract.getHeight();
            g.setColor(new Color(0x33, 0x33, 0x33));
            g.setStroke(new BasicStroke(Math.max(1f, fontSize / 12f)));
            g.drawLine(padding, blockTop, width - padding, blockTop);

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
