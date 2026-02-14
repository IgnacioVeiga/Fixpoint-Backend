package com.fixpoint.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
        ticketPartRepository.deleteAll();
        ticketRepository.deleteAll();
        inventoryRepository.deleteAll();
        clientRepository.deleteAll();
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
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "deviceType": "Phone",
                                  "entryDate": "2026-02-14",
                                  "problemDescription": "Broken button",
                                  "status": "diagnosing",
                                  "needsContract": false,
                                  "contractSigned": false
                                }
                                """.formatted(clientId)))
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
}
