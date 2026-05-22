"use client";

import { useCallback, useMemo, useState } from "react";
import type { MenuItemModel } from "@/lib/api/types";
import type { CartLine } from "@/lib/cart/types";

function recordToLines(map: Record<number, CartLine>): CartLine[] {
  return Object.values(map);
}

export function useServeCart() {
  const [cart, setCart] = useState<Record<number, CartLine>>({});

  const lines = useMemo(() => recordToLines(cart), [cart]);
  const lineCount = lines.length;
  const itemCount = useMemo(() => lines.reduce((s, l) => s + l.quantity, 0), [lines]);
  const total = useMemo(
    () => lines.reduce((s, l) => s + Number(l.item.price) * l.quantity, 0),
    [lines],
  );

  const addItem = useCallback((item: MenuItemModel) => {
    setCart((prev) => {
      const cur = prev[item.id];
      const quantity = (cur?.quantity ?? 0) + 1;
      return { ...prev, [item.id]: { item, quantity } };
    });
  }, []);

  const incrementItem = useCallback((id: number) => {
    setCart((prev) => {
      const cur = prev[id];
      if (!cur) return prev;
      return { ...prev, [id]: { ...cur, quantity: cur.quantity + 1 } };
    });
  }, []);

  const decrementItem = useCallback((id: number) => {
    setCart((prev) => {
      const cur = prev[id];
      if (!cur) return prev;
      if (cur.quantity <= 1) {
        const next = { ...prev };
        delete next[id];
        return next;
      }
      return { ...prev, [id]: { ...cur, quantity: cur.quantity - 1 } };
    });
  }, []);

  const clearCart = useCallback(() => {
    setCart({});
  }, []);

  return {
    lines,
    lineCount,
    itemCount,
    total,
    addItem,
    incrementItem,
    decrementItem,
    clearCart,
  };
}
