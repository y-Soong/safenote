package com.prafta.common.cmm.dailycontract;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.prafta.common.cmm.file.support.PdfSupport;

import lombok.extern.slf4j.Slf4j;

/**
 * 일용직 근로계약서 <b>서명본 PDF</b> 조립 유틸 (멀티페이지 지원 P1·P2·§5-4).
 *
 * <p>서명본은 항상 PDF 다(P1). 조립 규칙:
 * <ul>
 *   <li>원본이 PDF: 원본 문서를 <b>재렌더하지 않고 그대로 열어</b> 마지막에 서명 블록 페이지를 append 한다
 *       (원본 텍스트/서식 보존 — §5-4). 결과 = 원본 Np + 1p</li>
 *   <li>원본이 이미지: 이미지 픽셀 1px→1pt 비율의 페이지 1장(A4 강제 축소 금지 — D-P5) + 서명 블록 페이지.
 *       결과 = 2p</li>
 * </ul>
 *
 * <p>서명 블록 페이지 폭 = 직전 페이지와 동일 폭(D-P4), 높이 = 블록 비율 + 여백.
 *
 * <p>PDFBox 직접 사용은 본 클래스와 {@link PdfSupport}(+{@code FileServiceImpl})로 한정한다(D-P9).
 * 도메인 서비스는 본 클래스의 바이트 API 만 호출한다.
 *
 * <p><b>레거시 PNG 합성본은 절대 재합성/변환하지 않는다</b>(P3 — append-only 증적, 해시 무결성).
 */
@Slf4j
public final class ContractPdfBuilder {

    /** pt/inch. */
    private static final float PT_PER_INCH = 72f;

    /** 서명 블록 렌더 DPI(P6 열람 DPI 와 동일 — 확대 시 가독성). */
    private static final float SIGN_BLOCK_RENDER_DPI = 150f;

    /** 서명 블록 렌더 최소 폭(px) — ContractImageComposer.MIN_CANVAS_WIDTH 정합(D-P6). */
    private static final int MIN_BLOCK_RENDER_WIDTH_PX = 640;

    /** 서명 블록 페이지 여백(pt). */
    private static final float SIGN_PAGE_MARGIN_PT = 24f;

    /** PDF 페이지 크기 상한(pt) — PDF 스펙/뷰어 호환 한계(200 inch). 초과 시 비율 유지 축소. */
    private static final float MAX_PAGE_SIZE_PT = 14400f;

    /** PDF 확장자(점 제외 소문자). */
    private static final String EXT_PDF = "pdf";

    private ContractPdfBuilder() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 서명 블록 페이지가 가질 폭(pt)을 산출한다 — 직전 페이지 폭과 동일하게 맞춘다(D-P4).
     *
     * <p>원본 PDF = 마지막 페이지 폭(CropBox·회전 반영), 원본 이미지 = 이미지 픽셀 폭(1px→1pt, D-P5).
     * 판정 불가 시 A4 폭으로 폴백한다.
     */
    public static float resolveSignBlockPageWidthPt(byte[] originalBytes, String originalExt) {
        if (isPdf(originalExt)) {
            float[] size = PdfSupport.lastPageSize(originalBytes);
            if (size != null && size[0] > 0f) {
                return clampPageSide(size[0]);
            }
            return PDRectangle.A4.getWidth();
        }
        int[] imageSize = readImageSize(originalBytes);
        if (imageSize != null && imageSize[0] > 0) {
            return clampPageSide(imageSize[0]);
        }
        return PDRectangle.A4.getWidth();
    }

    /**
     * 서명 블록 PNG 렌더 폭(px)을 산출한다 — {@code max(640, 페이지폭pt × 150/72)}(D-P6).
     */
    public static int resolveSignBlockRenderWidthPx(float pageWidthPt) {
        int scaled = Math.round(pageWidthPt * SIGN_BLOCK_RENDER_DPI / PT_PER_INCH);
        return Math.max(MIN_BLOCK_RENDER_WIDTH_PX, scaled);
    }

    /**
     * 서명본 PDF 를 조립한다.
     *
     * @param originalBytes 계약서 양식 원본 바이트(PDF 또는 png/jpg)
     * @param originalExt   원본 확장자(점 제외 소문자)
     * @param signBlockPng  {@code ContractImageComposer.renderSignBlock} 결과 PNG
     * @return 서명본 PDF 바이트
     * @throws IOException PDF 조립/저장 실패(호출부에서 에러코드 매핑)
     * @throws com.prafta.common.exception.ApiException 원본 PDF 가 암호 설정(FILE_400_002)
     */
    public static byte[] buildSignedPdf(byte[] originalBytes, String originalExt, byte[] signBlockPng)
            throws IOException {

        if (isPdf(originalExt)) {
            // 원본 PDF: 페이지를 그대로 보존(재렌더 금지)하고 마지막에 서명 블록 페이지만 추가.
            try (PDDocument doc = Loader.loadPDF(originalBytes)) {
                float lastWidth = PDRectangle.A4.getWidth();
                int total = doc.getNumberOfPages();
                if (total > 0) {
                    PDPage last = doc.getPage(total - 1);
                    PDRectangle box = last.getCropBox();
                    int rotation = ((last.getRotation() % 360) + 360) % 360;
                    lastWidth = (rotation == 90 || rotation == 270) ? box.getHeight() : box.getWidth();
                }
                appendSignBlockPage(doc, signBlockPng, clampPageSide(lastWidth));
                return save(doc);
            }
        }

        // 원본 이미지: 이미지 비율 페이지 1장 + 서명 블록 페이지.
        try (PDDocument doc = new PDDocument()) {
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, originalBytes, "contract-original");
            float[] pageSize = imagePageSize(image.getWidth(), image.getHeight());
            PDPage page = new PDPage(new PDRectangle(pageSize[0], pageSize[1]));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(image, 0f, 0f, pageSize[0], pageSize[1]);
            }
            appendSignBlockPage(doc, signBlockPng, pageSize[0]);
            return save(doc);
        }
    }

    /** 서명 블록 페이지를 문서 마지막에 추가한다(폭 = pageWidthPt, 높이 = 블록 비율 + 여백). */
    private static void appendSignBlockPage(PDDocument doc, byte[] signBlockPng, float pageWidthPt)
            throws IOException {

        PDImageXObject block = PDImageXObject.createFromByteArray(doc, signBlockPng, "contract-sign-block");
        float contentWidth = Math.max(1f, pageWidthPt - SIGN_PAGE_MARGIN_PT * 2f);
        float scale = contentWidth / Math.max(1, block.getWidth());
        float contentHeight = Math.max(1f, block.getHeight() * scale);
        float pageHeight = clampPageSide(contentHeight + SIGN_PAGE_MARGIN_PT * 2f);

        PDPage page = new PDPage(new PDRectangle(pageWidthPt, pageHeight));
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // PDF 좌표 원점은 좌하단 — 여백만큼 띄워 블록을 배치.
            cs.drawImage(block, SIGN_PAGE_MARGIN_PT, SIGN_PAGE_MARGIN_PT, contentWidth, contentHeight);
        }
    }

    /**
     * 이미지 픽셀 크기 → 페이지 크기(pt). 1px→1pt 비율을 유지하되(D-P5) 스펙 상한(14400pt)을 넘으면
     * 비율 그대로 축소한다. 이미지 XObject 의 픽셀 데이터는 보존되므로 확대 시 정보 손실은 없다.
     */
    private static float[] imagePageSize(int widthPx, int heightPx) {
        float w = Math.max(1, widthPx);
        float h = Math.max(1, heightPx);
        float over = Math.max(w, h) / MAX_PAGE_SIZE_PT;
        if (over > 1f) {
            w = w / over;
            h = h / over;
        }
        return new float[] { w, h };
    }

    /** 페이지 한 변 길이를 스펙 상한(14400pt) 이내로 보정한다. */
    private static float clampPageSide(float sidePt) {
        if (sidePt <= 0f) {
            return PDRectangle.A4.getWidth();
        }
        return Math.min(sidePt, MAX_PAGE_SIZE_PT);
    }

    /** 이미지 헤더만 읽어 픽셀 크기를 반환한다(전체 디코딩 회피). 판정 불가면 null. */
    private static int[] readImageSize(byte[] bytes) {
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
        } catch (Exception e) {
            log.warn("계약서 원본 이미지 크기 조회 실패 - 원인={}", e.getMessage());
            return null;
        }
    }

    /** PDDocument → 바이트. */
    private static byte[] save(PDDocument doc) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /** 확장자가 PDF 인지 판정(점 제외 소문자 기준). */
    private static boolean isPdf(String ext) {
        return ext != null && EXT_PDF.equals(ext.toLowerCase());
    }
}
