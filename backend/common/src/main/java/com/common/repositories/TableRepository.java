package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.common.entities.TableEntity;
import com.common.enums.TableStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, Integer>, JpaSpecificationExecutor<TableEntity> {
    // Tìm table theo tableNumber
    Optional<TableEntity> findByTableNumber(Integer tableNumber);
    
    // Tìm tables theo status
    List<TableEntity> findByTableStatus(TableStatus tableStatus);
    
    // Tìm available tables
    List<TableEntity> findByTableStatusOrderByTableNumberAsc(TableStatus tableStatus);
    
    // Kiểm tra xem table có tồn tại không
    boolean existsByTableNumber(Integer tableNumber);
}
