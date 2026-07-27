package com.prafta.common.cmm.file.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * PNG 페이지 이미지를 세로로 이어붙이는 유틸 (구버전 앱 폴백 — 계약서 멀티페이지 T5/P7).
 *
 * <p>도메인 비종속 이미지 유틸이며 {@link PdfSupport} 의 협력 클래스로 같은 패키지에 둔다.
 *
 * <p>메모리 전략(D-P8): 1패스에서 <b>PNG 헤더만</b> 읽어 각 페이지 크기를 구해 캔버스 크기를 확정하고,
 * 2패스에서 페이지를 <b>한 장씩</b> 디코딩해 캔버스에 그린 뒤 즉시 해제한다. 총 픽셀 상한을 초과하면
 * 비율을 유지해 축소한다 → 힙 피크 = 캔버스 + 페이지 1장.
 */
@Slf4j
public final class PageStackComposer {

    private PageStackComposer() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 페이지 PNG 목록을 세로로 이어붙여 단일 PNG 로 반환한다.
     *
     * @param pagePngs  페이지 PNG 바이트 목록(위→아래 순서)
     * @param maxPixels 결과 이미지 총 픽셀 상한(초과 시 비율 유지 축소). 0 이하면 무제한
     * @return 병합 PNG 바이트. 입력이 비었으면 null
     * @throws java.io.IOException 디코딩/인코딩 실패
     */
    public static byte[] stackVertically(List<byte[]> pagePngs, long maxPixels) throws java.io.IOException {
        if (pagePngs == null || pagePngs.isEmpty()) {
            return null;
        }
        if (pagePngs.size() == 1) {
            // 1페이지면 병합 불필요 — 렌더 결과를 그대로 반환(불필요한 재인코딩 회피).
            return pagePngs.get(0);
        }

        // 1패스: 헤더만 읽어 원본 크기 수집.
        List<int[]> sizes = new ArrayList<>(pagePngs.size());
        long rawWidth = 0;
        long rawHeight = 0;
        for (byte[] png : pagePngs) {
            int[] size = readImageSize(png);
            if (size == null) {
                throw new java.io.IOException("페이지 이미지 크기를 읽을 수 없습니다.");
            }
            sizes.add(size);
            rawWidth = Math.max(rawWidth, size[0]);
            rawHeight += size[1];
        }
        if (rawWidth <= 0 || rawHeight <= 0) {
            return null;
        }

        // 총 픽셀 상한 가드 — 초과 시 비율 유지 축소(OOM 방지).
        double scale = 1.0d;
        if (maxPixels > 0 && rawWidth * rawHeight > maxPixels) {
            scale = Math.sqrt((double) maxPixels / ((double) rawWidth * (double) rawHeight));
            log.info("페이지 병합 픽셀 상한 초과 — 축소 적용(scale={}), pages={}",
                    String.format("%.3f", scale), pagePngs.size());
        }

        int canvasWidth = 0;
        int canvasHeight = 0;
        List<int[]> scaled = new ArrayList<>(sizes.size());
        for (int[] size : sizes) {
            int w = Math.max(1, (int) Math.round(size[0] * scale));
            int h = Math.max(1, (int) Math.round(size[1] * scale));
            scaled.add(new int[] { w, h });
            canvasWidth = Math.max(canvasWidth, w);
            canvasHeight += h;
        }

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, canvasWidth, canvasHeight);

            // 2패스: 한 장씩 디코딩 → 그리기 → 즉시 해제.
            int y = 0;
            for (int i = 0; i < pagePngs.size(); i++) {
                int[] size = scaled.get(i);
                BufferedImage page = ImageIO.read(new ByteArrayInputStream(pagePngs.get(i)));
                if (page == null) {
                    throw new java.io.IOException("페이지 이미지를 디코딩할 수 없습니다.");
                }
                try {
                    int x = (canvasWidth - size[0]) / 2;   // 폭이 다른 페이지는 수평 중앙 정렬
                    g.drawImage(page, x, y, size[0], size[1], null);
                } finally {
                    page.flush();
                }
                y += size[1];
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", baos);
        canvas.flush();
        return baos.toByteArray();
    }

    /** 이미지 헤더만 읽어 픽셀 크기를 반환한다(전체 디코딩 회피). 판정 불가면 null. */
    private static int[] readImageSize(byte[] bytes) throws java.io.IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new int[] { reader.getWidth(0), reader.getHeight(0) };
            } finally {
                reader.dispose();
            }
        }
    }
}
