package com.ai.system.model.dto.token;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class TokenUsageExportDO {

    @ExcelProperty("企业名称")
    @ColumnWidth(20)
    private String businessName;

    @ExcelProperty("API Key")
    @ColumnWidth(35)
    private String apikey;

    @ExcelProperty("总Token用量")
    @ColumnWidth(15)
    private Long tokens;

    @ExcelProperty("输入Token")
    @ColumnWidth(15)
    private Long inputTokens;

    @ExcelProperty("输出Token")
    @ColumnWidth(15)
    private Long outputTokens;

    @ExcelProperty("请求次数")
    @ColumnWidth(12)
    private Long request;

    @ExcelProperty("总时长(ms)")
    @ColumnWidth(15)
    private Long totalDuration;

    @ExcelProperty("总张数")
    @ColumnWidth(12)
    private Long totalAmount;

    @ExcelProperty("日期")
    @ColumnWidth(14)
    private String recordDate;
}
