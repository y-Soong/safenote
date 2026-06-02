package com.prafta.common.cmm.file.application.model;

public record FileInfoModel(
	String itemCd
    , String fileName // 클라이언트 파일명(선택)
    
    // 일일점검 컬럼
    , String answerDesc				// 답변비고
    , String inspectValue			// 답변 (Y/N)
) {
    
}