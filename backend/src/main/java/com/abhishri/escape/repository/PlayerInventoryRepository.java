package com.abhishri.escape.repository;

import com.abhishri.escape.domain.PlayerInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerInventoryRepository extends JpaRepository<PlayerInventory, UUID> {
}
