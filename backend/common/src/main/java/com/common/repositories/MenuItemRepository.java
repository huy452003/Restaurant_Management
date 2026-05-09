package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.common.entities.MenuItemEntity;
import com.common.enums.MenuItemStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItemEntity, Integer>, JpaSpecificationExecutor<MenuItemEntity> {
    // Tìm menu items theo status
    List<MenuItemEntity> findByMenuItemStatus(MenuItemStatus menuItemStatus);
    
    // Tìm menu items available
    List<MenuItemEntity> findByMenuItemStatusOrderByNameAsc(MenuItemStatus menuItemStatus);
    
    // Tìm menu items theo category
    List<MenuItemEntity> findByCategoryId(Integer categoryId);
    
    // Tìm menu items available theo category
    List<MenuItemEntity> findByCategoryIdAndMenuItemStatus(Integer categoryId, MenuItemStatus menuItemStatus);
    
    // Kiểm tra xem menu item có tồn tại không theo tên
    Optional<MenuItemEntity> findByName(String name);
    boolean existsByName(String name);
}
