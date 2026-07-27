package com.prafta.common.cmm.file.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.prafta.common.error.file.FileErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.extern.slf4j.Slf4j;

/**
 * PDF 바이트 단위 공통 유틸 (계약서 멀티페이지 지원 T1).
 *
 * <p>PDFBox 사용처 격리 원칙(plan §4 D-P9): PDFBox 직접 호출은 본 클래스 /
 * {@code FileServiceImpl}(경로 해석 후 위임) / {@code ContractPdfBuilder}(서명본 조립)로만 한정한다.
 * 도메인 서비스는 PDFBox 를 직접 import 하지 않고 본 유틸의 바이트 API 만 사용한다.
 *
 * <p>예외 매핑: 암호 설정 PDF = {@link FileErrorCode#FILE_400_002},
 * 파싱/렌더 실패(손상 등) = {@link FileErrorCode#FILE_500_001}.
 * 도메인 계층은 이 코드를 자기 맥락의 코드로 remap 한다(plan §7).
 *
 * <p>로깅: 파일 경로/PII 는 남기지 않는다(경로는 호출부 책임, 여기서는 바이트만 취급).
 */
@Slf4j
public final class PdfSupport {

    /** PDF 매직 바이트("%PDF-") — 확장자·contentType 스푸핑 1차 방어. */
    private static final byte[] PDF_MAGIC = { '%', 'P', 'D', 'F', '-' };

    private PdfSupport() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 바이트 선두가 PDF 매직("%PDF-")인지 판정한다.
     *
     * <p>선두 오프셋 0 만 인정한다(관대한 파서를 노린 prefix junk / polyglot 차단).
     *
     * @param head 파일 선두 바이트(전체 바이트를 넘겨도 무방)
     */
    public static boolean looksLikePdf(byte[] head) {
        if (head == null || head.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (head[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 암호(사용자/소유자 암호)가 설정된 PDF 인지 판정한다.
     *
     * <p>사용자 암호가 걸린 문서는 로드 자체가 실패({@code InvalidPasswordException})하므로 true,
     * 소유자 암호만 걸린 문서는 로드는 되지만 {@code isEncrypted()} 가 true 다. 둘 다 차단 대상.
     *
     * @throws ApiException 파싱 실패(FILE_500_001)
     */
    public static boolean isEncrypted(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.isEncrypted();
        } catch (InvalidPasswordException e) {
            return true;
        } catch (Exception e) {
            log.warn("PDF 암호화 여부 판정 실패 - 원인={}", e.getMessage());
            throw new ApiException(FileErrorCode.FILE_500_001);
        }
    }

    /**
     * PDF 페이지 수를 읽는다.
     *
     * @throws ApiException 암호 PDF(FILE_400_002) / 파싱 실패(FILE_500_001)
     */
    public static int readPageCount(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new ApiException(FileErrorCode.FILE_400_002);
            }
            return doc.getNumberOfPages();
        } catch (InvalidPasswordException e) {
            throw new ApiException(FileErrorCode.FILE_400_002);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.warn("PDF 페이지 수 조회 실패 - 원인={}", e.getMessage());
            throw new ApiException(FileErrorCode.FILE_500_001);
        }
    }

    /**
     * 페이지를 {@code pageStride} 간격으로 성기게 샘플해 최대 {@code maxPages} 장까지 PNG 로 렌더한다.
     *
     * <p>TBM AI 교육자료 렌더 경로가 사용하던 기존 루프 시맨틱을 그대로 옮긴 것이다
     * (동일 입력·동일 DPI 에서 결과 바이트 동일).
     *
     * @param pageStride 페이지 샘플 간격(&lt;1 이면 1로 보정)
     * @param maxPages   렌더 페이지 상한(&lt;1 이면 1로 보정)
     * @param dpi        렌더 DPI
     * @throws ApiException 암호 PDF(FILE_400_002) / 렌더 실패(FILE_500_001)
     */
    public static List<byte[]> renderPagesToPng(byte[] pdf, int pageStride, int maxPages, float dpi) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new ApiException(FileErrorCode.FILE_400_002);
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            int total = doc.getNumberOfPages();
            int stride = Math.max(1, pageStride);
            int cap = Math.max(1, maxPages);
            List<byte[]> out = new ArrayList<>();
            for (int i = 0; i < total && out.size() < cap; i += stride) {
                out.add(toPng(renderer.renderImageWithDPI(i, dpi)));
            }
            return out;
        } catch (InvalidPasswordException e) {
            throw new ApiException(FileErrorCode.FILE_400_002);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("PDF 페이지 렌더 실패 - 원인={}", e.getMessage());
            throw new ApiException(FileErrorCode.FILE_500_001);
        }
    }

    /**
     * 단일 페이지(0-base 인덱스)를 PNG 로 렌더한다.
     *
     * @return 렌더 결과 PNG 바이트. 인덱스가 문서 범위를 벗어나면 {@code null}
     *         (범위 검증·에러코드 매핑은 도메인 계층 책임 — plan §5 T1 수용기준 3)
     * @throws ApiException 암호 PDF(FILE_400_002) / 렌더 실패(FILE_500_001)
     */
    public static byte[] renderPageToPng(byte[] pdf, int pageIndex0, float dpi) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new ApiException(FileErrorCode.FILE_400_002);
            }
            if (pageIndex0 < 0 || pageIndex0 >= doc.getNumberOfPages()) {
                return null;
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            return toPng(renderer.renderImageWithDPI(pageIndex0, dpi));
        } catch (InvalidPasswordException e) {
            throw new ApiException(FileErrorCode.FILE_400_002);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("PDF 단일 페이지 렌더 실패 - 원인={}", e.getMessage());
            throw new ApiException(FileErrorCode.FILE_500_001);
        }
    }

    /**
     * 전 페이지를 순서대로 PNG 로 렌더한다(구버전 앱 폴백 세로 병합용 — plan §5 T5).
     *
     * @param maxPages 렌더 페이지 상한(자원 고갈 방지 하드 가드)
     * @throws ApiException 암호 PDF(FILE_400_002) / 렌더 실패(FILE_500_001)
     */
    public static List<byte[]> renderAllPagesToPng(byte[] pdf, float dpi, int maxPages) {
        return renderPagesToPng(pdf, 1, maxPages, dpi);
    }

    /**
     * 마지막 페이지의 표시 크기(pt)를 반환한다 — 서명 블록 페이지 폭 정합(plan §4 D-P4).
     *
     * <p>CropBox 기준이며 회전(90/270)은 폭·높이를 교환해 반환한다.
     *
     * @return {widthPt, heightPt}. 페이지가 0장이면 {@code null}
     * @throws ApiException 암호 PDF(FILE_400_002) / 파싱 실패(FILE_500_001)
     */
    public static float[] lastPageSize(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new ApiException(FileErrorCode.FILE_400_002);
            }
            int total = doc.getNumberOfPages();
            if (total <= 0) {
                return null;
            }
            PDPage page = doc.getPage(total - 1);
            PDRectangle box = page.getCropBox();
            int rotation = ((page.getRotation() % 360) + 360) % 360;
            if (rotation == 90 || rotation == 270) {
                return new float[] { box.getHeight(), box.getWidth() };
            }
            return new float[] { box.getWidth(), box.getHeight() };
        } catch (InvalidPasswordException e) {
            throw new ApiException(FileErrorCode.FILE_400_002);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.warn("PDF 페이지 크기 조회 실패 - 원인={}", e.getMessage());
            throw new ApiException(FileErrorCode.FILE_500_001);
        }
    }

    /**
     * 지정 DPI 로 렌더할 때 <b>가장 큰 페이지</b>가 차지하는 픽셀 수를 반환한다 (자원 고갈 방어 — sec SEC-1).
     *
     * <p>업로드 시점 게이트용이다. 바이트 크기·페이지 수만 검증하면 "수십 KB / 20페이지 / 페이지당
     * 14400×14400pt(PDF 스펙 상한)" 같은 입력이 통과하는데, 이를 150DPI 로 렌더하면 페이지 한 장이
     * 9억 픽셀(TYPE_INT_RGB 3.6GB)이 되어 {@code OutOfMemoryError} 가 난다. OOM 은 {@code Error} 라
     * 전역 예외 핸들러가 잡지 못하고 인스턴스 전체(전 테넌트)로 파급되므로 <b>등록 시점에</b> 막아야 한다.
     *
     * <p>CropBox 기준이며 회전(90/270)은 폭·높이를 교환해 계산한다({@link #lastPageSize} 와 동일 기준).
     *
     * @param dpi 렌더 예정 DPI
     * @return 최대 페이지의 렌더 픽셀 수. 페이지가 0장이면 0
     * @throws ApiException 암호 PDF(FILE_400_002) / 파싱 실패(FILE_500_001)
     */
    public static long maxPageRenderPixels(byte[] pdf, float dpi) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) {
                throw new ApiException(FileErrorCode.FILE_400_002);
            }
            long max = 0L;
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage page = doc.getPage(i);
                PDRectangle box = page.getCropBox();
                int rotation = ((page.getRotation() % 360) + 360) % 360;
                float widthPt = (rotation == 90 || rotation == 270) ? box.getHeight() : box.getWidth();
                float heightPt = (rotation == 90 || rotation == 270) ? box.getWidth() : box.getHeight();

                // PDFBox 렌더와 동일한 픽셀 산출식(pt / 72 * dpi).
                long widthPx = (long) Math.ceil(widthPt / 72f * dpi);
                long heightPx = (long) Math.ceil(heightPt / 72f * dpi);
                long pixels = Math.max(0L, widthPx) * Math.max(0L, heightPx);
                if (pixels > max) {
                    max = pixels;
                }
            }
            return max;
        } catch (InvalidPasswordException e) {
            throw new ApiException(FileErrorCode.FILE_400_002);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.warn("PDF 렌더 픽셀 수 산출 실패 - 원인={}", e.getMessage());
            throw new ApiException(FileErrorCode.FILE_500_001);
        }
    }

    /** BufferedImage → PNG 바이트. */
    private static byte[] toPng(BufferedImage image) throws java.io.IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
