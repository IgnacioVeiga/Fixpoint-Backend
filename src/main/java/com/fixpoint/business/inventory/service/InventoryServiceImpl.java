package com.fixpoint.business.inventory.service;

import com.fixpoint.business.inventory.dto.InventoryDTO;
import com.fixpoint.business.inventory.entity.Inventory;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.service.TicketPartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<InventoryDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public InventoryDTO findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException(TicketPartService.INVENTORY_ITEM_NOT_FOUND));
    }

    @Override
    public InventoryDTO save(InventoryDTO dto) {
        Inventory entity = toEntity(dto);
        return toDto(repository.save(entity));
    }

    @Override
    public InventoryDTO update(Long id, InventoryDTO dto) {
        Inventory existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(TicketPartService.INVENTORY_ITEM_NOT_FOUND));
        existing.setName(dto.getName());
        existing.setComponentType(dto.getComponentType());
        existing.setDescription(dto.getDescription());
        existing.setCondition(dto.getCondition());
        existing.setSource(dto.getSource());
        existing.setQuantity(dto.getQuantity());
        existing.setLocation(dto.getLocation());
        return toDto(repository.save(existing));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
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
                .addedAt(i.getAddedAt().format(formatter))
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
