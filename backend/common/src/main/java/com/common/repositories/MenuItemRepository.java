package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.common.entities.MenuItemEntity;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItemEntity, Integer>, JpaSpecificationExecutor<MenuItemEntity> {
    Optional<MenuItemEntity> findByName(String name);
    boolean existsByName(String name);
}
