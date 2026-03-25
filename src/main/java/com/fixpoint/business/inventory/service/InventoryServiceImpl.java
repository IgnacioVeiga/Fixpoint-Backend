package com.fixpoint.business.inventory.service;

import com.fixpoint.business.inventory.dto.InventoryDTO;
import com.fixpoint.business.inventory.entity.Inventory;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.ticketparts.service.TicketPartService;
import com.fixpoint.config.cache.CacheInvalidationService;
import com.fixpoint.config.cache.CacheNames;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final TicketPartRepository ticketPartRepository;
    private final CacheInvalidationService cacheInvalidationService;

    @Override
    @Cacheable(CacheNames.INVENTORY_ALL)
    public List<InventoryDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.INVENTORY_BY_ID, key = "#id")
    public InventoryDTO findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException(TicketPartService.INVENTORY_ITEM_NOT_FOUND));
    }

    @Override
    public InventoryDTO save(InventoryDTO dto) {
        Inventory entity = toEntity(dto);
        InventoryDTO savedInventory = toDto(repository.save(entity));
        evictInventoryCaches();
        return savedInventory;
    }

    @Override
    public InventoryDTO update(Long id, InventoryDTO dto) {
        Inventory existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(TicketPartService.INVENTORY_ITEM_NOT_FOUND));
        existing.setName(dto.getName());
        existing.setComponentType(dto.getComponentType());
        existing.setDescription(dto.getDescription());
        existing.setCondition(dto.getCondition());
        existing.setSource(dto.getSource());
        existing.setQuantity(dto.getQuantity());
        existing.setLocation(dto.getLocation());
        InventoryDTO updatedInventory = toDto(repository.save(existing));
        evictInventoryCaches();
        return updatedInventory;
    }

    @Override
    public void delete(Long id) {
        Inventory inventory = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(TicketPartService.INVENTORY_ITEM_NOT_FOUND));

        if (ticketPartRepository.existsByInventoryId(id)) {
            throw new IllegalStateException("Cannot delete inventory item because it is linked to ticket parts");
        }

        repository.delete(inventory);
        evictInventoryCaches();
    }

    private void evictInventoryCaches() {
        if (cacheInvalidationService == null) {
            return;
        }

        cacheInvalidationService.evictInventory();
        cacheInvalidationService.evictDashboard();
    }

    private InventoryDTO toDto(Inventory i) {
        return InventoryDTO.builder()
                .id(i.getId())
                .name(i.getName())
                .componentType(i.getComponentType())
                .description(i.getDescription())
                .condition(i.getCondition())
                .source(i.getSource())
                .quantity(i.getQuantity())
                .location(i.getLocation())
                .addedAt(i.getAddedAt())
                .build();
    }

    private Inventory toEntity(InventoryDTO dto) {
        return Inventory.builder()
                .id(dto.getId())
                .name(dto.getName())
                .componentType(dto.getComponentType())
                .description(dto.getDescription())
                .condition(dto.getCondition())
                .source(dto.getSource())
                .quantity(dto.getQuantity())
                .location(dto.getLocation())
                .build();
    }
}
