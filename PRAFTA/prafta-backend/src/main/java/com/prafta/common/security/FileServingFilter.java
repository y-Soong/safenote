package com.prafta.common.security;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code /uploads/**} 업로드 파일 서빙 보안 필터.
 *
 * <p>역할:
 * <ul>
 *   <li>서명 URL(exp/sig) 검증. 회사코드는 경로 세그먼트({@code /uploads/{cmpnyCd}/...})에서 추출.</li>
 *   <li>경로 traversal({@code ..}/절대경로/널바이트) 차단 — 보안상 enforce 무관 <b>항상</b> 차단(400).</li>
 *   <li>응답 보안 헤더: {@code X-Content-Type-Options: nosniff} 항상,
 *       확장자→MIME 화이트리스트(이미지/영상/PDF) 항목은 {@code Content-Type} +
 *       이미지/영상=inline · 문서(pdf 등)=attachment {@code Content-Disposition}.
 *       <b>화이트리스트 외 확장자(.html/.svg 등 active-content 포함)는 inline 렌더링(저장형 XSS)을 막기 위해
 *       {@code application/octet-stream} + {@code Content-Disposition: attachment} 강제(다운로드 처리).</b>
 *       서빙 자체는 차단하지 않음(관대모드 호환) — ResourceHandler 가 Content-Type 을 덮어써도
 *       attachment 와 nosniff 는 유지되어 inline 실행이 차단된다.</li>
 * </ul>
 *
 * <p>모드(관대/강제): {@code file.sign.enforce}(기본 false).
 * <ul>
 *   <li>false(관대): 미서명/서명실패도 통과시키되 경고 로그(미전환 모듈 호환).</li>
 *   <li>true(강제): 서명 검증 실패 시 403.</li>
 * </ul>
 * <b>이번 라운드는 false 유지</b>(다른 모듈 미전환).
 *
 * <p>서빙 바디는 통과 후 기존 정적 ResourceHandler 가 처리한다. 본 필터는 검증/헤더만 담당한다.
 * (ResourceHandler 가 Content-Type 을 확장자 기반으로 재설정할 수 있으나, 화이트리스트 확장자에 대해서는
 *  동일 MIME 이며 nosniff·Content-Disposition 은 유지된다. sniffing 위험은 nosniff 로 차단된다.)
 */
@Slf4j
@RequiredArgsConstructor
public class FileServingFilter extends OncePerRequestFilter {

    private final FileUrlSigner fileUrlSigner;

    /** 강제 모드 여부(기본 false=관대). 이번 라운드는 false. (config 에서 file.sign.enforce 주입) */
    private final boolean enforce;

    /** 확장자 → MIME 화이트리스트(소문자, 점 제외). 목록 외 확장자는 octet-stream + attachment 강제(inline XSS 차단). */
    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("webp", "image/webp"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("webm", "video/webm"),
            Map.entry("m4v", "video/x-m4v"),
            Map.entry("pdf", "application/pdf"));

    /** inline 표시 허용(이미지/영상). 그 외(pdf 등 문서)는 attachment 강제. */
    private static final Set<String> INLINE_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "mp4", "mov", "avi", "mkv", "webm", "m4v");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 컨텍스트 패스 제거한 서빙 상대경로(/uploads/...).
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String relPath = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx))
                ? uri.substring(ctx.length())
                : uri;

        // 1) traversal/널바이트 차단(enforce 무관 항상). 디코드 후에도 재검사.
        String decoded;
        try {
            decoded = URLDecoder.decode(relPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = relPath;
        }
        if (isTraversal(relPath) || isTraversal(decoded)) {
            log.warn("[파일서빙] 경로 traversal 차단 - path={}", relPath);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 2) 보안 응답 헤더 — nosniff 는 항상(전 모듈 안전).
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 3) 서명 검증.
        boolean signed = verifySignature(request, relPath);
        if (!signed) {
            if (enforce) {
                log.warn("[파일서빙] 서명 검증 실패(강제모드) - 403, path={}", relPath);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // 관대모드: 통과시키되 경고 로그(TBM 외 미전환 모듈 호환).
            log.warn("[파일서빙] 미서명/서명실패 요청 통과(관대모드) - path={}", relPath);
        }

        // 4) 확장자 화이트리스트 기반 Content-Type / Content-Disposition.
        //    화이트리스트(이미지/영상/PDF) 항목: 명시 MIME + 이미지/영상 inline · 문서 attachment.
        //    화이트리스트 외 확장자(.html/.svg 등 active-content 포함): 차단하지 않되 inline 렌더링(저장형 XSS)을
        //    막기 위해 octet-stream + attachment 강제(ResourceHandler 가 Content-Type 을 덮어써도 attachment·nosniff 유지).
        String ext = extensionOf(relPath);
        String mime = ext != null ? MIME.get(ext) : null;
        String fileName = fileNameOf(relPath);
        if (mime != null) {
            response.setContentType(mime);
            String disposition = INLINE_EXTS.contains(ext) ? "inline" : "attachment";
            response.setHeader("Content-Disposition",
                    disposition + "; filename=\"" + sanitizeFileName(fileName) + "\"");
        } else {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + sanitizeFileName(fileName) + "\"");
        }

        // 서빙 바디는 ResourceHandler 가 처리.
        chain.doFilter(request, response);
    }

    /** /uploads/ 이외 경로(정적 서빙 매핑 밖)는 필터 건너뜀. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
        return !path.startsWith("/uploads/");
    }

    private boolean verifySignature(HttpServletRequest request, String relPath) {
        String expStr = request.getParameter("exp");
        String sig = request.getParameter("sig");
        if (!StringUtils.hasText(expStr) || !StringUtils.hasText(sig)) {
            return false;
        }
        long exp;
        try {
            exp = Long.parseLong(expStr);
        } catch (NumberFormatException e) {
            return false;
        }
        String cmpnyCd = fileUrlSigner.extractCmpnyCd(relPath);
        return fileUrlSigner.verify(relPath, exp, sig, cmpnyCd);
    }

    /** {@code ..} 세그먼트/널바이트/백슬래시 traversal 차단. */
    private boolean isTraversal(String path) {
        if (path == null) {
            return true;
        }
        if (path.indexOf('\0') >= 0) {
            return true;
        }
        String p = path.replace('\\', '/');
        if (p.contains("/../") || p.endsWith("/..") || p.startsWith("../") || p.equals("..")) {
            return true;
        }
        return false;
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        if (dot < 0 || dot < slash || dot == path.length() - 1) {
            return null;
        }
        return path.substring(dot + 1).toLowerCase();
    }

    private String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** Content-Disposition 헤더 인젝션 방지(따옴표/제어문자 제거). */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return "file";
        }
        return name.replaceAll("[\"\\r\\n]", "");
    }
}
