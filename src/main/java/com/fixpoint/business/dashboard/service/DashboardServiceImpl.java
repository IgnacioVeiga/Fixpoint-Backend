package com.fixpoint.business.dashboard.service;

import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.entity.Attachment;
import com.fixpoint.business.attachments.repository.AttachmentRepository;
import com.fixpoint.business.attachments.service.FileStorageService;
import com.fixpoint.business.clients.entity.Client;
import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.business.dashboard.dto.DashboardMonthlyCountDTO;
import com.fixpoint.business.dashboard.dto.DashboardMonthlyTotalDTO;
import com.fixpoint.business.dashboard.dto.DashboardPartUsageDTO;
import com.fixpoint.business.dashboard.dto.DashboardStorageDTO;
import com.fixpoint.business.dashboard.dto.DashboardSummaryDTO;
import com.fixpoint.business.dashboard.dto.DashboardTopClientDTO;
import com.fixpoint.config.cache.CacheNames;
import com.fixpoint.business.ticketparts.entity.TicketPart;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int MONTHS_WINDOW = 6;
    private static final int TOP_CLIENTS_LIMIT = 5;
    private static final int TOP_PARTS_LIMIT = 5;
    private static final int RECENT_FILES_LIMIT = 18;
    private static final Locale DASHBOARD_LOCALE = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("LLL", DASHBOARD_LOCALE);

    private final TicketRepository ticketRepository;
    private final ClientRepository clientRepository;
    private final TicketPartRepository ticketPartRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.file.storage-limit-bytes:0}")
    private long configuredStorageLimitBytes;

    @Override
    @Cacheable(CacheNames.DASHBOARD_SUMMARY)
    public DashboardSummaryDTO getSummary() {
        List<Ticket> tickets = ticketRepository.findAll();
        List<Client> clients = clientRepository.findAll();
        List<TicketPart> ticketParts = ticketPartRepository.findAll();
        List<Attachment> recentAttachments = attachmentRepository.findAll(
                PageRequest.of(0, RECENT_FILES_LIMIT, Sort.by(Sort.Direction.DESC, "uploadedAt"))
        ).getContent();
        DashboardStorageDTO storage = getStorageSummary();

        return DashboardSummaryDTO.builder()
                .ticketsByStatus(buildTicketsByStatus(tickets))
                .ticketsByMonth(buildTicketsByMonth(tickets))
                .clientGrowth(buildClientGrowth(clients))
                .partsUsage(buildPartsUsage(ticketParts))
                .topClients(buildTopClients(tickets))
                .storage(storage)
                .recentFiles(recentAttachments.stream().map(this::toAttachmentDto).toList())
                .build();
    }

    @Override
    @Cacheable(CacheNames.DASHBOARD_STORAGE)
    public DashboardStorageDTO getStorageSummary() {
        long usedBytes = Math.max(0L, attachmentRepository.sumFileSizeBytes());
        long fileCount = attachmentRepository.count();

        if (configuredStorageLimitBytes > 0) {
            long availableBytes = Math.max(0L, configuredStorageLimitBytes - usedBytes);
            return DashboardStorageDTO.builder()
                    .usedBytes(usedBytes)
                    .fileCount(fileCount)
                    .totalBytes(configuredStorageLimitBytes)
                    .availableBytes(availableBytes)
                    .usagePercent(calculateUsagePercent(usedBytes, configuredStorageLimitBytes))
                    .source("configured")
                    .build();
        }

        return fileStorageService.resolveStorageCapacity()
                .map(capacity -> DashboardStorageDTO.builder()
                        .usedBytes(usedBytes)
                        .fileCount(fileCount)
                        .totalBytes(capacity.totalBytes())
                        .availableBytes(capacity.availableBytes())
                        .usagePercent(calculateUsagePercent(usedBytes, capacity.totalBytes()))
                        .source("filesystem")
                        .build())
                .orElseGet(() -> DashboardStorageDTO.builder()
                        .usedBytes(usedBytes)
                        .fileCount(fileCount)
                        .totalBytes(null)
                        .availableBytes(null)
                        .usagePercent(null)
                        .source("logical")
                        .build());
    }

    private Map<String, Long> buildTicketsByStatus(List<Ticket> tickets) {
        Map<String, Long> counts = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getStatus, LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> ordered = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            ordered.put(status.value(), counts.getOrDefault(status.value(), 0L));
        }
        return ordered;
    }

    private List<DashboardMonthlyCountDTO> buildTicketsByMonth(List<Ticket> tickets) {
        Map<YearMonth, Long> countsByMonth = tickets.stream()
                .filter(ticket -> ticket.getEntryDate() != null)
                .collect(Collectors.groupingBy(
                        ticket -> YearMonth.from(ticket.getEntryDate()),
                        Collectors.counting()
                ));

        return buildRecentMonths().stream()
                .map(month -> DashboardMonthlyCountDTO.builder()
                        .month(formatMonth(month))
                        .count(countsByMonth.getOrDefault(month, 0L))
                        .build())
                .toList();
    }

    private List<DashboardMonthlyTotalDTO> buildClientGrowth(List<Client> clients) {
        List<YearMonth> months = buildRecentMonths();

        return months.stream()
                .map(month -> {
                    LocalDate monthEndExclusive = month.plusMonths(1).atDay(1);
                    long total = clients.stream()
                            .filter(client -> client.getCreatedAt() != null)
                            .filter(client -> client.getCreatedAt().toLocalDate().isBefore(monthEndExclusive))
                            .count();

                    return DashboardMonthlyTotalDTO.builder()
                            .month(formatMonth(month))
                            .total(total)
                            .build();
                })
                .toList();
    }

    private List<DashboardPartUsageDTO> buildPartsUsage(List<TicketPart> ticketParts) {
        return ticketParts.stream()
                .collect(Collectors.groupingBy(
                        part -> part.getInventory().getName(),
                        Collectors.summingLong(part -> part.getQuantity() == null ? 0 : part.getQuantity())
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_PARTS_LIMIT)
                .map(entry -> DashboardPartUsageDTO.builder()
                        .name(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<DashboardTopClientDTO> buildTopClients(List<Ticket> tickets) {
        return tickets.stream()
                .filter(ticket -> ticket.getClient() != null)
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getClient().getName(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_CLIENTS_LIMIT)
                .map(entry -> DashboardTopClientDTO.builder()
                        .name(entry.getKey())
                        .ticketCount(entry.getValue())
                        .build())
                .toList();
    }

    private List<YearMonth> buildRecentMonths() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(MONTHS_WINDOW - 1L);

        return IntStream.range(0, MONTHS_WINDOW)
                .mapToObj(firstMonth::plusMonths)
                .toList();
    }

    private String formatMonth(YearMonth month) {
        String rawLabel = month.format(MONTH_LABEL_FORMATTER);
        String normalized = rawLabel.replace(".", "");
        return normalized.substring(0, 1).toUpperCase(DASHBOARD_LOCALE) + normalized.substring(1);
    }

    private AttachmentDTO toAttachmentDto(Attachment attachment) {
        return AttachmentDTO.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .filename(attachment.getFilename())
                .filepath(attachment.getFilepath())
                .fileType(attachment.getFileType())
                .fileFormat(attachment.getFileFormat())
                .fileSizeBytes(attachment.getFileSizeBytes())
                .tag(attachment.getTag())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    private Double calculateUsagePercent(long usedBytes, long totalBytes) {
        if (totalBytes <= 0) {
            return null;
        }

        return (usedBytes * 100.0d) / totalBytes;
    }
}
