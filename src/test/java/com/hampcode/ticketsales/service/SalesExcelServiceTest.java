package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.response.SalesReportRowDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesExcelServiceTest {

    @Mock ReportService reportService;
    @InjectMocks SalesExcelService salesExcelService;

    @Test
    void generatesOneRowPerEventPlusHeaderAndTotal() throws Exception {
        var row = new SalesReportRowDTO("Festival de Rock", LocalDate.of(2026, 12, 5), "Lima", 200, 50, 10, 5, new BigDecimal("6000"));
        when(reportService.getMySalesReport()).thenReturn(List.of(row));

        byte[] excel = salesExcelService.generate();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Evento", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Festival de Rock", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals(10.0, sheet.getRow(4).getCell(5).getNumericCellValue());
            assertEquals(6000.0, sheet.getRow(4).getCell(8).getNumericCellValue());
            assertEquals("TOTAL", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals(25.0, sheet.getRow(5).getCell(7).getNumericCellValue());
        }
    }
}
