package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.common.entities.InventoryEntity;
import com.common.enums.InventoryStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Integer>, JpaSpecificationExecutor<InventoryEntity> {
    // Tìm inventory theo ingredient name
    Optional<InventoryEntity> findByIngredientName(String ingredientName);
    
    // Tìm inventories theo status
    List<InventoryEntity> findByInventoryStatus(InventoryStatus inventoryStatus);
    
    // Tìm inventories có quantity thấp (cần restock)
    @Query("SELECT i FROM InventoryEntity i WHERE i.quantity <= i.minStockLevel")
    List<InventoryEntity> findLowStockItems();
    
    // Tìm inventories cần restock (quantity <= minStockLevel)
    @Query("SELECT i FROM InventoryEntity i WHERE i.quantity <= i.minStockLevel " +
           "AND i.inventoryStatus = InventoryStatus.IN_STOCK")
    List<InventoryEntity> findItemsNeedingRestock();
    
    // Tìm out of stock items
    @Query("SELECT i FROM InventoryEntity i WHERE i.quantity = 0 " +
           "OR i.inventoryStatus = InventoryStatus.OUT_OF_STOCK")
    List<InventoryEntity> findOutOfStockItems();
    
    // Kiểm tra xem inventory có tồn tại không
    boolean existsByIngredientName(String ingredientName);
    
    // Tìm inventories theo status và sắp xếp theo name
    List<InventoryEntity> findByInventoryStatusOrderByIngredientNameAsc(InventoryStatus inventoryStatus);
}
