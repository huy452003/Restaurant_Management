package com.common.models.order;

import com.common.enums.TableStatus;
import com.common.models.BaseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableModel extends BaseModel{
    private Integer tableNumber;
    private Integer capacity;
    private TableStatus tableStatus;
    private String location;
}
