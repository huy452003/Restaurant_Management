package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import com.common.entities.TableEntity;
import com.common.enums.TableStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, Integer>, JpaSpecificationExecutor<TableEntity> {
    Optional<TableEntity> findByTableNumber(Integer tableNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TableEntity> findByTableNumberAndTableStatus(Integer tableNumber, TableStatus tableStatus);

    boolean existsByTableNumber(Integer tableNumber);

    List<TableEntity> findByTableStatus(TableStatus tableStatus);
}
