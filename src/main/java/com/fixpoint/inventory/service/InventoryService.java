package com.fixpoint.inventory.service;

import com.fixpoint.inventory.dto.InventoryDTO;

import java.util.List;

public interface InventoryService {
    List<InventoryDTO> findAll();
    InventoryDTO findById(Long id);
    InventoryDTO save(InventoryDTO dto);
    InventoryDTO update(Long id, InventoryDTO dto);
    void delete(Long id);
}