package com.fixpoint.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixpoint.business.attachments.repository.AttachmentRepository;
import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    private static final Path TEST_UPLOAD_DIR = Path.of("target", "test-uploads");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketPartRepository ticketPartRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @BeforeEach
    void cleanDatabase() {
        attachmentRepository.deleteAll();
        ticketPartRepository.deleteAll();
        ticketRepository.deleteAll();
        inventoryRepository.deleteAll();
        clientRepository.deleteAll();
        cleanUploadDirectory();
    }

    @Test
    void shouldCreateTicketForExistingClient() throws Exception {
        long clientId = createClient("Alice");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "deviceType": "Laptop",
                                  "entryDate": "2026-02-14",
                                  "problemDescription": "No power",
                                  "status": "diagnosing",
                                  "needsContract": false,
                                  "contractSigned": false
                                }
                                """.formatted(clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.deviceType").value("Laptop"))
                .andExpect(jsonPath("$.status").value("diagnosing"));
    }

    @Test
    void shouldReturnBadRequestWhenInventoryQuantityIsInvalid() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Display 7in",
                                  "componentType": "display",
                                  "condition": "new",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("quantity")));
    }

    @Test
    void shouldReturnConflictWhenPartQuantityExceedsStock() throws Exception {
        long clientId = createClient("Bob");
        long ticketId = createTicket(clientId);
        long inventoryId = createInventory("Battery X", 1);

        mockMvc.perform(post("/api/tickets/{ticketId}/parts", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inventoryId": %d,
                                  "quantity": 2,
                                  "note": "Need replacement"
                                }
                                """.formatted(inventoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    @Test
    void shouldReturnConflictWhenDeletingLinkedInventoryItem() throws Exception {
        long clientId = createClient("Carla");
        long ticketId = createTicket(clientId);
        long inventoryId = createInventory("Fan C", 4);
        addPart(ticketId, inventoryId, 1);

        mockMvc.perform(delete("/api/inventory/{id}", inventoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("linked to ticket parts")));
    }

    @Test
    void shouldReturnNotFoundForMissingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"));
    }

    @Test
    void shouldRejectInvalidTicketStatusTransition() throws Exception {
        long clientId = createClient("Erica");
        long ticketId = createTicket(clientId);

        mockMvc.perform(put("/api/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "deviceType": "Phone",
                                  "entryDate": "2026-02-14",
                                  "problemDescription": "Broken button",
                                  "status": "repaired",
                                  "needsContract": false,
                                  "contractSigned": false
                                }
                                """.formatted(clientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Invalid ticket status transition")));
    }

    @Test
    void shouldRejectLogCreationForClosedTicket() throws Exception {
        long clientId = createClient("Frank");
        long ticketId = createTicket(clientId, "returned");

        mockMvc.perform(post("/api/tickets/{ticketId}/logs", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Final review note",
                                  "author": "tech"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot add logs to a closed ticket"));
    }

    @Test
    void shouldUploadListDownloadAndDeleteAttachment() throws Exception {
        long clientId = createClient("Diana");
        long ticketId = createTicket(clientId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "diagnostic-note.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "diagnostic-content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/attachments/upload/ticket/{ticketId}", ticketId)
                        .file(file)
                        .param("fileType", "other"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.filename").value("diagnostic-note.txt"))
                .andExpect(jsonPath("$.fileType").value("other"))
                .andReturn();

        long attachmentId = readId(uploadResult);

        mockMvc.perform(get("/api/attachments/ticket/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attachmentId))
                .andExpect(jsonPath("$[0].filename").value("diagnostic-note.txt"));

        mockMvc.perform(get("/api/attachments/download/{id}", attachmentId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("diagnostic-note.txt")))
                .andExpect(content().bytes("diagnostic-content".getBytes()));

        mockMvc.perform(delete("/api/attachments/{id}", attachmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/attachments/{id}", attachmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Attachment not found"));
    }

    private long createClient(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "dni": "12345678"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();

        return readId(result);
    }

    private long createTicket(long clientId) throws Exception {
        return createTicket(clientId, "diagnosing");
    }

    private long createTicket(long clientId, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "deviceType": "Phone",
                                  "entryDate": "2026-02-14",
                                  "problemDescription": "Broken button",
                                  "status": "%s",
                                  "needsContract": false,
                                  "contractSigned": false
                                }
                                """.formatted(clientId, status)))
                .andExpect(status().isOk())
                .andReturn();

        return readId(result);
    }

    private long createInventory(String name, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "componentType": "generic",
                                  "condition": "new",
                                  "quantity": %d
                                }
                                """.formatted(name, quantity)))
                .andExpect(status().isOk())
                .andReturn();

        return readId(result);
    }

    private void addPart(long ticketId, long inventoryId, int quantity) throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/parts", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inventoryId": %d,
                                  "quantity": %d,
                                  "note": "Added by integration test"
                                }
                                """.formatted(inventoryId, quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("id").asLong();
    }

    private void cleanUploadDirectory() {
        if (!Files.exists(TEST_UPLOAD_DIR)) {
            return;
        }

        try {
            Files.walk(TEST_UPLOAD_DIR)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to clean test upload directory", ex);
                        }
                    });
            Files.createDirectories(TEST_UPLOAD_DIR);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to clean test upload directory", ex);
        }
    }
}
