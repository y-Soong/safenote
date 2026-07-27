package com.prafta.common.cmm.file.support;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * 이미지 헤더 전용 조회 유틸 (자원 고갈 방어 — sec SEC-1).
 *
 * <p>{@code ImageIO.read} 는 전체 픽셀을 디코딩하므로 "크기를 알기 위해 디코딩한다"는 순서 자체가
 * 자기모순이다. 1200dpi 스캔 A4 PNG(9920×14030 = 1.39억 px)는 흰 배경이면 10MB 이내로 압축되어
 * 바이트 상한을 통과하지만, 디코딩 시점에 557MB(4 byte/px)를 요구해 업로드 요청만으로 힙을 고갈시킨다.
 *
 * <p>따라서 <b>디코딩 전에</b> 헤더만 읽어 픽셀 수를 판정한다. 판정 불가(미지원 포맷·손상)는 여기서
 * 차단하지 않고 {@code -1} 을 반환한다 — 형식 유효성은 호출부의 실디코딩 검증이 담당한다.
 *
 * <p>로깅: 파일명·경로·PII 를 남기지 않는다(바이트만 취급).
 */
@Slf4j
public final class ImageProbe {

    /** 픽셀 수 판정 불가(미지원 포맷·손상 헤더). */
    public static final long UNKNOWN = -1L;

    private ImageProbe() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 이미지 헤더만 읽어 총 픽셀 수(폭 × 높이)를 반환한다. 전체 디코딩을 하지 않는다.
     *
     * @return 총 픽셀 수. 판정 불가 시 {@link #UNKNOWN}
     */
    public static long pixelCount(byte[] bytes) {
        int[] size = size(bytes);
        if (size == null) {
            return UNKNOWN;
        }
        return (long) size[0] * (long) size[1];
    }

    /**
     * 이미지 헤더만 읽어 픽셀 크기를 반환한다.
     *
     * @return {width, height}. 판정 불가 시 {@code null}
     */
    public static int[] size(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
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
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    return null;
                }
                return new int[] { width, height };
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            log.info("이미지 헤더 크기 판정 실패 — 원인={}", e.getMessage());
            return null;
        }
    }
}
