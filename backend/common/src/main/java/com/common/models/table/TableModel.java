package com.common.models.table;

import com.common.enums.TableStatus;
import com.common.models.BaseModel;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableModel extends BaseModel{
    private Integer tableNumber;
    private Integer capacity;
    @Enumerated(EnumType.STRING)
    private TableStatus tableStatus;
    private String location;
}
