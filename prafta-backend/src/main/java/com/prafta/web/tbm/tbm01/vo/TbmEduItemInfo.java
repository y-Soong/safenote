package com.prafta.web.tbm.tbm01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TbmEduItemInfo{
	String chk;
	String mtrlItemCd;
	String mtrlCd;
	String sortIdx;
	String mtrlItemType;
	String mtrlDesc;
	String fileMgmtCd;
	String fileNm;
	String filePath;
	String fileExt;
	String url;
	String useYn;
	
	/* 데이터 초기화용 값 */
	String oriSortIdx;
	String oriMtrlItemType;
	String oriMtrlDesc;
	String oriFileMgmtCd;
	String oriFileNm;
	String oriFilePath;
	String oriFileExt;
	String oriUrl;
	String oriUseYn;
}
