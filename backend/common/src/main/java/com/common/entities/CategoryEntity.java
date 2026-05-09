package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.CategoryStatus;

import java.util.List;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEntity extends BaseEntity {
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "image", nullable = false)
    private String image;
    @Column(name = "category_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CategoryStatus categoryStatus;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<MenuItemEntity> menuItems;
}
