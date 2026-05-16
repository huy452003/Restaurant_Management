import type { OrderType } from "@/lib/api/types";

export const ORDER_TYPES: OrderType[] = ["DINE_IN", "DELIVERY"];

export function orderRequiresTable(orderType: OrderType): boolean {
  return orderType === "DINE_IN";
}
