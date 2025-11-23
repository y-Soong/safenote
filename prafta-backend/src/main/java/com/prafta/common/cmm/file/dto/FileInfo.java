package com.prafta.common.cmm.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    String itemCd;
    String fileName; // 클라이언트 파일명(선택)
    
    // 일일점검 컬럼
    String answerDesc;				// 답변비고
    String inspectValue;			// 답변 (Y/N)
}