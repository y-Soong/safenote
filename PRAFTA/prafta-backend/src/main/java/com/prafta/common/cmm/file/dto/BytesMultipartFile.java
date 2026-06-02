package com.prafta.common.cmm.file.dto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * In-memory {@link MultipartFile} for JSON APIs that send file bytes (e.g. Base64-decoded).
 */
public class BytesMultipartFile implements MultipartFile {

	private final String name;
	private final String originalFilename;
	private final String contentType;
	private final byte[] content;

	public BytesMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
		this.name = name != null ? name : "";
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.content = content != null ? content : new byte[0];
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public boolean isEmpty() {
		return content.length == 0;
	}

	@Override
	public long getSize() {
		return content.length;
	}

	@Override
	public byte[] getBytes() {
		return content;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(content);
	}

	@Override
	public void transferTo(File dest) throws IOException {
		java.nio.file.Files.write(dest.toPath(), content);
	}
}
