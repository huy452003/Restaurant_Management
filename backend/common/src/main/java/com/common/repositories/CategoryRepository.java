package com.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.common.entities.CategoryEntity;
import java.util.Optional;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer>, JpaSpecificationExecutor<CategoryEntity> {
    // Tìm category theo name
    Optional<CategoryEntity> findByName(String name);

    // Kiểm tra xem category có tồn tại không
    boolean existsByName(String name);
    
    // Tìm tất cả categories và sắp xếp theo name
    List<CategoryEntity> findAllByOrderByNameAsc();

}
