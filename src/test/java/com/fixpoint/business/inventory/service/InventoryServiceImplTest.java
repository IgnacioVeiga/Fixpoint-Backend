package com.fixpoint.business.inventory.service;

import com.fixpoint.business.inventory.entity.Inventory;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.ticketparts.service.TicketPartService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository repository;

    @Mock
    private TicketPartRepository ticketPartRepository;

    @InjectMocks
    private InventoryServiceImpl service;

    @Test
    void deleteShouldThrowWhenInventoryIsLinkedToTicketParts() {
        Long inventoryId = 1L;
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .name("Motherboard Z")
                .condition("used")
                .quantity(2)
                .build();

        when(repository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(ticketPartRepository.existsByInventoryId(inventoryId)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(inventoryId));

        assertEquals("Cannot delete inventory item because it is linked to ticket parts", ex.getMessage());
        verify(repository, never()).delete(inventory);
    }

    @Test
    void deleteShouldRemoveInventoryWhenNoTicketPartReferencesExist() {
        Long inventoryId = 2L;
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .name("Keyboard K")
                .condition("new")
                .quantity(5)
                .build();

        when(repository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(ticketPartRepository.existsByInventoryId(inventoryId)).thenReturn(false);

        service.delete(inventoryId);

        verify(repository).delete(inventory);
    }

    @Test
    void findByIdShouldThrowWhenInventoryIsMissing() {
        Long inventoryId = 3L;
        when(repository.findById(inventoryId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.findById(inventoryId));

        assertEquals(TicketPartService.INVENTORY_ITEM_NOT_FOUND, ex.getMessage());
    }
}
