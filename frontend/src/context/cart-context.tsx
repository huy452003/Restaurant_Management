"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useAuth } from "@/context/auth-context";
import type { MenuItemModel } from "@/lib/api/types";
import type { CartLine } from "@/lib/cart/types";
import { readCartFromStorage, writeCartToStorage } from "@/lib/cart/storage";

type CartContextValue = {
  lines: CartLine[];
  lineCount: number;
  itemCount: number;
  total: number;
  cartReady: boolean;
  addItem: (item: MenuItemModel) => void;
  incrementItem: (id: number) => void;
  decrementItem: (id: number) => void;
  clearCart: () => void;
};

const CartContext = createContext<CartContextValue | null>(null);

function linesToRecord(lines: CartLine[]): Record<number, CartLine> {
  const map: Record<number, CartLine> = {};
  for (const line of lines) {
    map[line.item.id] = line;
  }
  return map;
}

function recordToLines(map: Record<number, CartLine>): CartLine[] {
  return Object.values(map);
}

export function CartProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [cart, setCart] = useState<Record<number, CartLine>>({});
  const [cartReady, setCartReady] = useState(false);
  const activeUserIdRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const userId = user?.id;
    activeUserIdRef.current = userId;

    if (!userId) {
      setCart({});
      setCartReady(false);
      return;
    }

    setCartReady(false);
    setCart(linesToRecord(readCartFromStorage(userId)));
    setCartReady(true);
  }, [user?.id]);

  const persistForActiveUser = useCallback((next: Record<number, CartLine>) => {
    const userId = activeUserIdRef.current;
    setCart(next);
    if (userId) {
      writeCartToStorage(userId, recordToLines(next));
    }
  }, []);

  const updateCart = useCallback(
    (updater: (prev: Record<number, CartLine>) => Record<number, CartLine>) => {
      setCart((prev) => {
        const userId = activeUserIdRef.current;
        if (!userId) return prev;
        const next = updater(prev);
        writeCartToStorage(userId, recordToLines(next));
        return next;
      });
    },
    [],
  );

  const addItem = useCallback(
    (item: MenuItemModel) => {
      if (!activeUserIdRef.current) return;
      updateCart((prev) => {
        const cur = prev[item.id];
        const quantity = (cur?.quantity ?? 0) + 1;
        return { ...prev, [item.id]: { item, quantity } };
      });
    },
    [updateCart],
  );

  const incrementItem = useCallback(
    (id: number) => {
      if (!activeUserIdRef.current) return;
      updateCart((prev) => {
        const cur = prev[id];
        if (!cur) return prev;
        return { ...prev, [id]: { ...cur, quantity: cur.quantity + 1 } };
      });
    },
    [updateCart],
  );

  const decrementItem = useCallback(
    (id: number) => {
      if (!activeUserIdRef.current) return;
      updateCart((prev) => {
        const cur = prev[id];
        if (!cur) return prev;
        if (cur.quantity <= 1) {
          const next = { ...prev };
          delete next[id];
          return next;
        }
        return { ...prev, [id]: { ...cur, quantity: cur.quantity - 1 } };
      });
    },
    [updateCart],
  );

  const clearCart = useCallback(() => {
    persistForActiveUser({});
  }, [persistForActiveUser]);

  const lines = useMemo(() => {
    if (!user?.id || !cartReady) return [];
    return recordToLines(cart);
  }, [cart, user?.id, cartReady]);

  const lineCount = lines.length;
  const itemCount = useMemo(() => lines.reduce((s, l) => s + l.quantity, 0), [lines]);
  const total = useMemo(
    () => lines.reduce((s, l) => s + Number(l.item.price) * l.quantity, 0),
    [lines],
  );

  const value = useMemo(
    () => ({
      lines,
      lineCount,
      itemCount,
      total,
      cartReady,
      addItem,
      incrementItem,
      decrementItem,
      clearCart,
    }),
    [lines, lineCount, itemCount, total, cartReady, addItem, incrementItem, decrementItem, clearCart],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within CartProvider");
  return ctx;
}

export function useCartOptional() {
  return useContext(CartContext);
}
