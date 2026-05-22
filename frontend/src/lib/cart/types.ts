import type { MenuItemModel } from "@/lib/api/types";

export type CartLine = { item: MenuItemModel; quantity: number };

export type StoredCartLine = { itemId: number; item: MenuItemModel; quantity: number };
