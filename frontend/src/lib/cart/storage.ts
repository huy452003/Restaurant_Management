import type { CartLine, StoredCartLine } from "@/lib/cart/types";

const CART_KEY_PREFIX = "restaurantCart:";

function cartKey(userId: number) {
  return `${CART_KEY_PREFIX}${userId}`;
}

export function readCartFromStorage(userId: number): CartLine[] {
  if (typeof window === "undefined") return [];
  const raw = window.localStorage.getItem(cartKey(userId));
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as StoredCartLine[];
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((l) => l?.item && typeof l.quantity === "number" && l.quantity > 0)
      .map((l) => ({ item: l.item, quantity: l.quantity }));
  } catch {
    return [];
  }
}

export function writeCartToStorage(userId: number, lines: CartLine[]) {
  if (typeof window === "undefined") return;
  const payload: StoredCartLine[] = lines.map((l) => ({
    itemId: l.item.id,
    item: l.item,
    quantity: l.quantity,
  }));
  if (payload.length === 0) {
    window.localStorage.removeItem(cartKey(userId));
    return;
  }
  window.localStorage.setItem(cartKey(userId), JSON.stringify(payload));
}

/** Chỉ xóa giỏ của một user (không dùng khi logout — giữ lại để đăng nhập lại). */
export function clearCartForUser(userId: number) {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(cartKey(userId));
}
