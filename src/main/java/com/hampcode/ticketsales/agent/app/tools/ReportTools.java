package com.hampcode.ticketsales.agent.app.tools;

import com.hampcode.ticketsales.agent.app.dto.ChartData;
import com.hampcode.ticketsales.agent.app.dto.FileInfo;
import com.hampcode.ticketsales.agent.app.dto.ResponseData;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.dto.response.EventSalesDTO;
import com.hampcode.ticketsales.service.EventService;
import com.hampcode.ticketsales.service.ReportService;
import com.hampcode.ticketsales.service.SalesExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportTools {

    private final EventService eventService;
    private final ReportService reportService;
    private final SalesExcelService salesExcelService;

    @Tool(description = "Lista los eventos del organizador que inició sesión. Úsala para \"mis eventos\". Sin filtros devuelve todos.")
    public List<EventResponseDTO> myEvents(
            @ToolParam(description = "Ciudad, solo si la menciona", required = false) String city,
            @ToolParam(description = "Nombre del evento tal como lo escribió el usuario", required = false) String name,
            ToolContext context) {
        var events = eventService.searchMyEvents(name, city);
        ResponseData.of(context).setEvents(events);
        return events;
    }

    @Tool(description = "Ventas de TODOS los eventos del organizador: entradas pagadas y dinero recaudado por evento. Prepara un gráfico de barras.")
    public List<EventSalesDTO> sales(ToolContext context) {
        var sales = reportService.getMySalesByEvent();
        ResponseData.of(context).setChart(new ChartData("bar", "Ingresos por evento (S/)", summary(sales),
                sales.stream().map(EventSalesDTO::eventName).toList(),
                sales.stream().map(eventSales -> (Number) eventSales.revenue()).toList()));
        return sales;
    }

    @Tool(description = "Genera el reporte de ventas del organizador en Excel para descargar. Úsala cuando pida el reporte, Excel o descargar las ventas.")
    public String exportSales(ToolContext context) {
        ResponseData.of(context).setFile(new FileInfo("reporte-ventas.xlsx", salesExcelService.generate()));
        return "Reporte listo para descargar.";
    }

    private static String summary(List<EventSalesDTO> sales) {
        var total = sales.stream().map(EventSalesDTO::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) return "Todavía no hay ingresos registrados.";
        var top = sales.stream().max(Comparator.comparing(EventSalesDTO::revenue)).get();
        var lowest = sales.stream().min(Comparator.comparing(EventSalesDTO::revenue)).get();
        if (sales.size() == 1) return "%s ha generado S/ %s.".formatted(top.eventName(), top.revenue());
        long share = Math.round(top.revenue().doubleValue() * 100 / total.doubleValue());
        return "%s lidera con S/ %s (%d%% del total de S/ %s); %s es el más bajo con S/ %s."
                .formatted(top.eventName(), top.revenue(), share, total, lowest.eventName(), lowest.revenue());
    }
}
