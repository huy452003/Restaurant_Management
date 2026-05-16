"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { TableNumberSelect } from "@/components/TableNumberSelect";
import { useAuth } from "@/context/auth-context";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { MenuItemModel, OrderModel, OrderType, PaginatedResponse } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";
import { orderRequiresTable } from "@/lib/orders/order-type";

type CartLine = { item: MenuItemModel; quantity: number };

export default function MenuPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  const [items, setItems] = useState<MenuItemModel[]>([]);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [loadingMenu, setLoadingMenu] = useState(true);
  const [cart, setCart] = useState<Record<number, CartLine>>({});
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const [orderType, setOrderType] = useState<OrderType>("DINE_IN");
  const needsTable = orderRequiresTable(orderType);
  const { tables, loading: tablesLoading, error: tablesError, reload: reloadTables } = useRestaurantTables({
    enabled: !!user && needsTable,
    tableStatus: "AVAILABLE",
    excludeTablesWithPendingOrder: true,
  });
  const [notes, setNotes] = useState("");
  const [checkoutMsg, setCheckoutMsg] = useState<string | null>(null);
  const [checkoutPending, setCheckoutPending] = useState(false);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/menu");
      return;
    }
    let cancelled = false;
    (async () => {
      setLoadingMenu(true);
      setFetchError(null);
      try {
        const qs = buildPageParams(0, 100, { menuItemStatus: "AVAILABLE" });
        const res = await apiFetch<PaginatedResponse<MenuItemModel>>(`/menu-items/filters?${qs}`);
        if (!cancelled) setItems(res.data.content ?? []);
      } catch (e) {
        if (!cancelled)
          setFetchError(e instanceof ApiError ? e.message : "Không tải được thực đơn");
      } finally {
        if (!cancelled) setLoadingMenu(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user, authLoading, router]);

  useEffect(() => {
    if (!needsTable) {
      setTableNumber("");
      return;
    }
    if (tables.length === 0) return;
    if (tableNumber === "" || !tables.some((t) => t.tableNumber === tableNumber)) {
      setTableNumber(tables[0].tableNumber);
    }
  }, [tables, tableNumber, needsTable]);

  const addToCart = useCallback((item: MenuItemModel) => {
    setCart((prev) => {
      const cur = prev[item.id];
      const quantity = (cur?.quantity ?? 0) + 1;
      return { ...prev, [item.id]: { item, quantity } };
    });
  }, []);

  const decFromCart = useCallback((id: number) => {
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

  const cartLines = useMemo(() => Object.values(cart), [cart]);
  const cartTotal = useMemo(() => {
    let sum = 0;
    for (const line of cartLines) {
      sum += Number(line.item.price) * line.quantity;
    }
    return sum;
  }, [cartLines]);

  async function checkout() {
    if (!user || cartLines.length === 0) return;
    if (needsTable && (tableNumber === "" || tables.length === 0)) return;
    setCheckoutMsg(null);
    setCheckoutPending(true);
    try {
      const orderRes = await apiFetch<OrderModel>("/orders", {
        method: "POST",
        body: JSON.stringify({
          ...(needsTable ? { tableNumber } : {}),
          orderType,
          notes: notes.trim() || undefined,
        }),
      });
      const orderNumber = orderRes.data.orderNumber;
      const payloads = cartLines.map((line) => ({
        orderNumber,
        menuItemName: line.item.name,
        quantity: line.quantity,
      }));
      await apiFetch<unknown[]>("/order-items", {
        method: "POST",
        body: JSON.stringify(payloads),
      });
      setCart({});
      setNotes("");
      setCheckoutMsg(`Đã tạo đơn ${orderNumber}. Bạn có thể gửi bếp từ trang Đơn hàng.`);
      void reloadTables();
    } catch (e) {
      setCheckoutMsg(e instanceof ApiError ? e.message : "Thanh toán giỏ thất bại");
      void reloadTables();
    } finally {
      setCheckoutPending(false);
    }
  }

  if (authLoading || !user) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted">
        Đang kiểm tra phiên đăng nhập…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:grid lg:grid-cols-[1fr_340px] lg:gap-10">
      <div>
        <h1
          className="font-serif text-3xl font-semibold text-brand-900"
          style={{ fontFamily: "var(--font-cormorant), serif" }}
        >
          Thực đơn
        </h1>

        {fetchError ? (
          <p className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-800 ring-1 ring-red-100">{fetchError}</p>
        ) : null}

        {loadingMenu ? (
          <p className="mt-10 text-muted">Đang tải món…</p>
        ) : (
          <ul className="mt-8 grid gap-6 sm:grid-cols-2">
            {items.map((item) => (
              <li
                key={item.id}
                className="flex gap-4 overflow-hidden rounded-2xl border border-stone-200 bg-surface p-4 shadow-sm transition hover:shadow-md"
              >
                <div className="relative h-28 w-28 shrink-0 overflow-hidden rounded-xl bg-stone-100">
                  {item.image?.startsWith("http") ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={item.image} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center text-xs text-muted">Ảnh</div>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium uppercase tracking-wide text-accent">{item.categoryName}</p>
                  <h2 className="mt-0.5 font-semibold text-stone-900">{item.name}</h2>
                  {item.description ? (
                    <p className="mt-1 line-clamp-2 text-sm text-muted">{item.description}</p>
                  ) : null}
                  <p className="mt-2 text-lg font-semibold text-brand-800">{formatVnd(item.price)}</p>
                  <button
                    type="button"
                    onClick={() => addToCart(item)}
                    className="mt-2 rounded-lg bg-brand-800 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-900"
                  >
                    Thêm vào giỏ
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <aside className="mt-10 lg:mt-0">
        <div className="sticky top-24 space-y-4 rounded-2xl border border-stone-200 bg-surface p-5 shadow-sm">
          <h2 className="font-serif text-xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
            Giỏ hàng
          </h2>
          {cartLines.length === 0 ? (
            <p className="text-sm text-muted">Chưa có món. Hãy thêm từ danh sách bên trái.</p>
          ) : (
            <ul className="max-h-64 space-y-3 overflow-y-auto text-sm">
              {cartLines.map((line) => (
                <li key={line.item.id} className="flex items-center justify-between gap-2 border-b border-stone-100 pb-2">
                  <span className="min-w-0 truncate font-medium">{line.item.name}</span>
                  <span className="flex shrink-0 items-center gap-1">
                    <button
                      type="button"
                      className="h-7 w-7 rounded border border-stone-200 text-stone-600 hover:bg-stone-50"
                      onClick={() => decFromCart(line.item.id)}
                    >
                      −
                    </button>
                    <span className="w-6 text-center">{line.quantity}</span>
                    <button
                      type="button"
                      className="h-7 w-7 rounded border border-stone-200 text-stone-600 hover:bg-stone-50"
                      onClick={() => addToCart(line.item)}
                    >
                      +
                    </button>
                  </span>
                </li>
              ))}
            </ul>
          )}

          <p className="text-sm font-semibold text-stone-800">
            Tạm tính: <span className="text-brand-800">{formatVnd(cartTotal)}</span>
          </p>

          <div className="space-y-2 border-t border-stone-100 pt-4">
            <label className="block text-xs font-medium text-stone-600">Loại đơn</label>
            <select
              value={orderType}
              onChange={(e) => setOrderType(e.target.value as OrderType)}
              className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
            >
              <option value="DINE_IN">Tại chỗ</option>
              <option value="DELIVERY">Giao hàng</option>
            </select>
            {needsTable ? (
              <TableNumberSelect
                id="cart-table"
                value={tableNumber}
                onChange={setTableNumber}
                tables={tables}
                loading={tablesLoading}
                error={tablesError}
                emptyHint="Không còn bàn trống. Vui lòng quay lại sau ít phút nữa."
              />
            ) : (
              <p className="text-xs text-stone-500">Đơn giao hàng không cần chọn bàn.</p>
            )}
            <label className="block text-xs font-medium text-stone-600">Ghi chú</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              maxLength={300}
              className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
              placeholder="Ít cay, không hành…"
            />
          </div>

          {checkoutMsg ? (
            <p
              className={`rounded-lg px-3 py-2 text-sm ${
                checkoutMsg.startsWith("Đã tạo")
                  ? "bg-emerald-50 text-emerald-900 ring-1 ring-emerald-100"
                  : "bg-red-50 text-red-800 ring-1 ring-red-100"
              }`}
            >
              {checkoutMsg}{" "}
              {checkoutMsg.startsWith("Đã tạo") ? (
                <Link href="/orders" className="font-semibold underline">
                  Xem đơn
                </Link>
              ) : null}
            </p>
          ) : null}

          <button
            type="button"
            disabled={
              cartLines.length === 0 ||
              checkoutPending ||
              (needsTable && (tableNumber === "" || tables.length === 0 || tablesLoading))
            }
            onClick={() => void checkout()}
            className="w-full rounded-xl bg-brand-800 py-3 text-sm font-semibold text-white shadow transition hover:bg-brand-900 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {checkoutPending ? "Đang tạo đơn…" : "Đặt món"}
          </button>
        </div>
      </aside>
    </div>
  );
}
