package com.fixpoint.business.attachments.service;

import com.fixpoint.business.attachments.entity.Attachment;
import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.repository.AttachmentRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
import com.fixpoint.config.cache.CacheInvalidationService;
import com.fixpoint.config.cache.CacheNames;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final String ATTACHMENT_NOT_FOUND = "Attachment not found";
    public static final String ORIGINAL_FILENAME_MUST_NOT_BE_NULL = "Original filename must not be null";
    private static final Map<String, String> SUPPORTED_FORMATS = Map.ofEntries(
            Map.entry("jpg", "image"),
            Map.entry("jpeg", "image"),
            Map.entry("png", "image"),
            Map.entry("webp", "image"),
            Map.entry("gif", "image"),
            Map.entry("bmp", "image"),
            Map.entry("tif", "image"),
            Map.entry("tiff", "image"),
            Map.entry("svg", "image"),
            Map.entry("pdf", "document"),
            Map.entry("doc", "document"),
            Map.entry("docx", "document"),
            Map.entry("odt", "document"),
            Map.entry("rtf", "document"),
            Map.entry("txt", "document"),
            Map.entry("xls", "spreadsheet"),
            Map.entry("xlsx", "spreadsheet"),
            Map.entry("csv", "spreadsheet"),
            Map.entry("ods", "spreadsheet"),
            Map.entry("zip", "archive"),
            Map.entry("rar", "archive"),
            Map.entry("7z", "archive")
    );

    private final AttachmentRepository attachmentRepo;
    private final TicketRepository ticketRepo;
    private final FileStorageService fileStorageService;
    private final CacheInvalidationService cacheInvalidationService;

    @Override
    @Cacheable(cacheNames = CacheNames.ATTACHMENTS_BY_TICKET, key = "#ticketId")
    public List<AttachmentDTO> findByTicketId(Long ticketId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));

        return attachmentRepo.findByTicket(ticket).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.RECENT_ATTACHMENTS, key = "#limit")
    public List<AttachmentDTO> findRecent(int limit) {
        return attachmentRepo.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "uploadedAt")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ATTACHMENT_BY_ID, key = "#id")
    public AttachmentDTO findById(Long id) {
        return attachmentRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
    }

    @Override
    public AttachmentDTO save(AttachmentDTO dto) {
        Ticket ticket = ticketRepo.findById(dto.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .filename(dto.getFilename())
                .filepath(dto.getFilepath())
                .fileType(dto.getFileType())
                .fileFormat(dto.getFileFormat())
                .fileSizeBytes(normalizeFileSize(dto.getFileSizeBytes()))
                .tag(normalizeTag(dto.getTag()))
                .build();

        AttachmentDTO savedAttachment = toDto(attachmentRepo.save(attachment));
        evictAttachmentCaches();
        return savedAttachment;
    }

    @Override
    public AttachmentDTO uploadFile(Long ticketId, MultipartFile file, String tag) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));
        ensureTicketIsOpen(ticket, "upload attachments");

        ResolvedAttachmentMetadata metadata = resolveMetadata(file);
        String storedFileName = fileStorageService.storeFile(file);

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .filename(metadata.originalFilename())
                .filepath(storedFileName)
                .fileType(metadata.fileType())
                .fileFormat(metadata.fileFormat())
                .fileSizeBytes(file.getSize())
                .tag(normalizeTag(tag))
                .build();

        AttachmentDTO savedAttachment = toDto(attachmentRepo.save(attachment));
        evictAttachmentCaches();
        return savedAttachment;
    }

    @Override
    public Resource downloadFile(Long id) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
        return fileStorageService.loadFileAsResource(attachment.getFilepath());
    }

    @Override
    public void delete(Long id) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
        ensureTicketIsOpen(attachment.getTicket(), "delete attachments");

        fileStorageService.deleteFile(attachment.getFilepath());
        attachmentRepo.delete(attachment);
        evictAttachmentCaches();
    }

    @Override
    public AttachmentDTO replaceFile(Long id, MultipartFile file, String tag) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
        ensureTicketIsOpen(attachment.getTicket(), "replace attachments");

        fileStorageService.deleteFile(attachment.getFilepath());
        ResolvedAttachmentMetadata metadata = resolveMetadata(file);
        String storedFileName = fileStorageService.storeFile(file);

        attachment.setFilename(metadata.originalFilename());
        attachment.setFilepath(storedFileName);
        attachment.setFileType(metadata.fileType());
        attachment.setFileFormat(metadata.fileFormat());
        attachment.setFileSizeBytes(file.getSize());
        if (tag != null) {
            attachment.setTag(normalizeTag(tag));
        }

        AttachmentDTO updatedAttachment = toDto(attachmentRepo.save(attachment));
        evictAttachmentCaches();
        return updatedAttachment;
    }

    private void ensureTicketIsOpen(Ticket ticket, String action) {
        TicketStatus status = TicketStatus.parse(ticket.getStatus());
        if (status.isClosed()) {
            throw new IllegalStateException("Cannot " + action + " for a closed ticket");
        }
    }

    private AttachmentDTO toDto(Attachment attachment) {
        return AttachmentDTO.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .filename(attachment.getFilename())
                .filepath(attachment.getFilepath())
                .fileType(attachment.getFileType())
                .fileFormat(attachment.getFileFormat())
                .fileSizeBytes(normalizeFileSize(attachment.getFileSizeBytes()))
                .tag(attachment.getTag())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    private ResolvedAttachmentMetadata resolveMetadata(MultipartFile file) {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), ORIGINAL_FILENAME_MUST_NOT_BE_NULL);
        String fileFormat = resolveFileFormat(originalFilename);
        String fileType = SUPPORTED_FORMATS.get(fileFormat);

        if (fileType == null) {
            throw new IllegalArgumentException(
                    "Unsupported file format '%s'. Allowed formats: %s".formatted(
                            fileFormat,
                            String.join(", ", SUPPORTED_FORMATS.keySet())
                    )
            );
        }

        return new ResolvedAttachmentMetadata(originalFilename, fileType, fileFormat);
    }

    private String resolveFileFormat(String filename) {
        int extensionSeparator = filename.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == filename.length() - 1) {
            throw new IllegalArgumentException("The uploaded file must include a supported extension");
        }
        return filename.substring(extensionSeparator + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }

        String normalized = tag.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void evictAttachmentCaches() {
        if (cacheInvalidationService == null) {
            return;
        }

        cacheInvalidationService.evictAttachments();
        cacheInvalidationService.evictDashboard();
    }

    private long normalizeFileSize(Long fileSizeBytes) {
        return fileSizeBytes == null ? 0L : Math.max(0L, fileSizeBytes);
    }

    private record ResolvedAttachmentMetadata(String originalFilename, String fileType, String fileFormat) {
    }
}
