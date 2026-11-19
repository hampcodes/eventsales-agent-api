package com.hampcode.ticketsales.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SalesExcelService {

    private static final int HEADER_ROW = 3;
    private static final int[] COLUMN_WIDTHS = {34, 14, 16, 12, 12, 13, 13, 12, 16};
    private static final byte[] NAVY = {0x1E, 0x3A, 0x5F};
    private static final byte[] LIGHT_GRAY = {(byte) 0xE8, (byte) 0xED, (byte) 0xF3};

    private final ReportService reportService;

    public byte[] generate() {
        var rows = reportService.getMySalesReport();
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Ventas por evento");
            sheet.setDisplayGridlines(false);
            addLogo(workbook, sheet);
            addTitle(workbook, sheet);

            var header = style(workbook, null, true, NAVY, true);
            var headerStyles = new XSSFCellStyle[COLUMN_WIDTHS.length];
            java.util.Arrays.fill(headerStyles, header);
            writeRow(sheet.createRow(HEADER_ROW), headerStyles, "Evento", "Fecha", "Ciudad", "Capacidad",
                    "Pagadas", "Pendientes", "Canceladas", "Ocupación", "Ingresos (S/)");

            var body = columnStyles(workbook, false);
            int rowNumber = HEADER_ROW + 1;
            long capacity = 0, paid = 0, pending = 0, cancelled = 0;
            var revenue = BigDecimal.ZERO;
            for (var row : rows) {
                writeRow(sheet.createRow(rowNumber++), body, row.eventName(), row.date().toString(), row.city(), row.capacityMax(),
                        row.ticketsPaid(), row.ticketsPending(), row.ticketsCancelled(), occupancy(row.ticketsPaid(), row.capacityMax()), row.revenue());
                capacity += row.capacityMax();
                paid += row.ticketsPaid();
                pending += row.ticketsPending();
                cancelled += row.ticketsCancelled();
                revenue = revenue.add(row.revenue());
            }
            writeRow(sheet.createRow(rowNumber), columnStyles(workbook, true),
                    "TOTAL", "", "", capacity, paid, pending, cancelled, occupancy(paid, capacity), revenue);

            for (int i = 0; i < COLUMN_WIDTHS.length; i++) sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            sheet.createFreezePane(0, HEADER_ROW + 1);
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, rowNumber - 1, 0, COLUMN_WIDTHS.length - 1));
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long occupancy(long ticketsPaid, long capacity) {
        return capacity == 0 ? 0 : Math.round(ticketsPaid * 100.0 / capacity);
    }

    private void addLogo(XSSFWorkbook workbook, XSSFSheet sheet) throws IOException {
        sheet.createRow(0).setHeightInPoints(24);
        sheet.createRow(1).setHeightInPoints(18);
        try (var logo = new ClassPathResource("images/logo.png").getInputStream()) {
            int pictureIndex = workbook.addPicture(logo.readAllBytes(), Workbook.PICTURE_TYPE_PNG);
            var anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(0);
            anchor.setRow1(0);
            anchor.setCol2(1);
            anchor.setRow2(2);
            sheet.createDrawingPatriarch().createPicture(anchor, pictureIndex);
        }
    }

    private void addTitle(XSSFWorkbook workbook, XSSFSheet sheet) {
        var font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(NAVY, null));
        var titleStyle = workbook.createCellStyle();
        titleStyle.setFont(font);
        var title = sheet.getRow(0).createCell(2);
        title.setCellValue("Reporte de ventas");
        title.setCellStyle(titleStyle);
        sheet.getRow(1).createCell(2).setCellValue("Generado el " + LocalDate.now());
    }

    private XSSFCellStyle[] columnStyles(XSSFWorkbook workbook, boolean total) {
        var fill = total ? LIGHT_GRAY : null;
        var text = style(workbook, null, total, fill, false);
        var integer = style(workbook, "#,##0", total, fill, false);
        var percent = style(workbook, "0\"%\"", total, fill, false);
        var money = style(workbook, "#,##0.00", total, fill, false);
        return new XSSFCellStyle[]{text, text, text, integer, integer, integer, integer, percent, money};
    }

    private XSSFCellStyle style(XSSFWorkbook workbook, String format, boolean bold, byte[] fill, boolean whiteText) {
        var style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(bold);
        if (whiteText) font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        if (format != null) style.setDataFormat(workbook.createDataFormat().getFormat(format));
        if (fill != null) {
            style.setFillForegroundColor(new XSSFColor(fill, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (whiteText) style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static void writeRow(Row row, XSSFCellStyle[] styles, Object... values) {
        for (int i = 0; i < values.length; i++) {
            var cell = row.createCell(i);
            if (values[i] instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(String.valueOf(values[i]));
            cell.setCellStyle(styles[i]);
        }
    }
}
