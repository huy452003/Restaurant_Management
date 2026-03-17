package com.common.models.wrapper;

import java.util.List;

import com.common.models.menu.MenuItemModel;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMenuItemRequest {
    @NotNull
    @NotEmpty
    private List<Integer> menuItemIds;

    @NotNull
    @NotEmpty
    @Valid
    private List<MenuItemModel> updates;
}
