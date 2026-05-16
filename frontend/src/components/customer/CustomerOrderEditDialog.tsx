"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { TableNumberSelect } from "@/components/TableNumberSelect";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type {
  MenuItemModel,
  OrderItemModel,
  OrderModel,
  OrderType,
  PaginatedResponse,
  TableModel,
} from "@/lib/api/types";
import { formatVnd } from "@/lib/money";
import { ORDER_TYPE_LABEL } from "@/lib/orders/order-labels";
import { ORDER_TYPES, orderRequiresTable } from "@/lib/orders/order-type";

type Props = {
  open: boolean;
  order: OrderModel | null;
  onClose: () => void;
  onSaved: () => void;
};

type ItemDraft = {
  quantity: number;
  specialInstructions: string;
};

export function CustomerOrderEditDialog({ open, order, onClose, onSaved }: Props) {
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const [orderType, setOrderType] = useState<OrderType>("DINE_IN");
  const [notes, setNotes] = useState("");
  const [items, setItems] = useState<OrderItemModel[]>([]);
  const [itemDrafts, setItemDrafts] = useState<Record<number, ItemDraft>>({});
  const [menuItems, setMenuItems] = useState<MenuItemModel[]>([]);
  const [addMenuItemName, setAddMenuItemName] = useState("");
  const [addQty, setAddQty] = useState(1);

  const [loadingItems, setLoadingItems] = useState(false);
  const [loadingMenu, setLoadingMenu] = useState(false);
  const [savingMeta, setSavingMeta] = useState(false);
  const [savingItemId, setSavingItemId] = useState<number | null>(null);
  const [addingItem, setAddingItem] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const needsTable = orderRequiresTable(orderType);

  const { tables: availableTables, loading: tablesLoading, error: tablesError } =
    useRestaurantTables({
      enabled: open && !!order && needsTable,
      tableStatus: "AVAILABLE",
      excludeTablesWithPendingOrder: true,
    });

  const tablesForSelect = useMemo((): TableModel[] => {
    if (!order || !needsTable || order.tableNumber == null) {
      return availableTables;
    }
    if (availableTables.some((t) => t.tableNumber === order.tableNumber)) {
      return availableTables;
    }
    return [
      ...availableTables,
      {
        id: -order.tableNumber,
        tableNumber: order.tableNumber,
        capacity: 0,
        tableStatus: "AVAILABLE",
      },
    ];
  }, [availableTables, order, needsTable]);

  const loadItems = useCallback(async () => {
    if (!order) return;
    setLoadingItems(true);
    setError(null);
    try {
      const qs = buildPageParams(0, 100, { orderNumber: order.orderNumber });
      const res = await apiFetch<PaginatedResponse<OrderItemModel>>(`/order-items/filters?${qs}`);
      const list = res.data.content ?? [];
      setItems(list);
      const drafts: Record<number, ItemDraft> = {};
      for (const line of list) {
        drafts[line.id] = {
          quantity: line.quantity,
          specialInstructions: line.specialInstructions ?? "",
        };
      }
      setItemDrafts(drafts);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được món trong đơn");
      setItems([]);
      setItemDrafts({});
    } finally {
      setLoadingItems(false);
    }
  }, [order]);

  const loadMenu = useCallback(async () => {
    setLoadingMenu(true);
    try {
      const qs = buildPageParams(0, 100, { menuItemStatus: "AVAILABLE" });
      const res = await apiFetch<PaginatedResponse<MenuItemModel>>(`/menu-items/filters?${qs}`);
      setMenuItems(res.data.content ?? []);
    } catch {
      setMenuItems([]);
    } finally {
      setLoadingMenu(false);
    }
  }, []);

  useEffect(() => {
    if (!open || !order) return;
    setTableNumber(order.tableNumber ?? "");
    setOrderType(order.orderType);
    setNotes(order.notes ?? "");
    setAddMenuItemName("");
    setAddQty(1);
    setError(null);
    setSuccessMsg(null);
    void loadItems();
    void loadMenu();
  }, [open, order, loadItems, loadMenu]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  async function saveMetadata() {
    if (!order || (needsTable && tableNumber === "")) return;
    setSavingMeta(true);
    setError(null);
    setSuccessMsg(null);
    try {
      await apiFetch<OrderModel>(`/orders/${order.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          ...(needsTable ? { tableNumber } : {}),
          orderType,
          notes: notes.trim() || null,
        }),
      });
      setSuccessMsg("Đã lưu thông tin đơn.");
      onSaved();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Lưu thông tin đơn thất bại");
    } finally {
      setSavingMeta(false);
    }
  }

  async function saveItem(itemId: number) {
    const draft = itemDrafts[itemId];
    if (!draft) return;
    setSavingItemId(itemId);
    setError(null);
    setSuccessMsg(null);
    try {
      await apiFetch<OrderItemModel>(`/order-items/${itemId}`, {
        method: "PATCH",
        body: JSON.stringify({
          quantity: draft.quantity,
          specialInstructions: draft.specialInstructions.trim() || null,
        }),
      });
      setSuccessMsg("Đã lưu món.");
      await loadItems();
      onSaved();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Lưu món thất bại");
    } finally {
      setSavingItemId(null);
    }
  }

  async function addItem() {
    if (!order || !addMenuItemName) return;
    setAddingItem(true);
    setError(null);
    setSuccessMsg(null);
    try {
      await apiFetch<OrderItemModel[]>("/order-items", {
        method: "POST",
        body: JSON.stringify([
          {
            orderNumber: order.orderNumber,
            menuItemName: addMenuItemName,
            quantity: addQty,
          },
        ]),
      });
      setAddMenuItemName("");
      setAddQty(1);
      setSuccessMsg("Đã thêm món vào đơn.");
      await loadItems();
      onSaved();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Thêm món thất bại");
    } finally {
      setAddingItem(false);
    }
  }

  function patchItemDraft(itemId: number, patch: Partial<ItemDraft>) {
    setItemDrafts((prev) => ({
      ...prev,
      [itemId]: { ...prev[itemId], ...patch },
    }));
  }

  if (!open || !order) return null;

  return (
    <EditOrderDialogShell onClose={onClose}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2
            id="customer-order-edit-title"
            className="font-serif text-xl font-semibold text-brand-900"
            style={{ fontFamily: "var(--font-cormorant), serif" }}
          >
            Chỉnh sửa đơn
          </h2>
          <p className="mt-1 font-mono text-sm font-semibold text-brand-800">{order.orderNumber}</p>
          <p className="mt-2 text-xs text-muted">
            Chỉ sửa được khi đơn đang chờ xác nhận. Lưu từng phần trước khi bấm &quot;Xác nhận đơn&quot;.
          </p>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="shrink-0 rounded-lg border border-stone-200 px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
        >
          Đóng
        </button>
      </div>

      {error ? (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>
      ) : null}
      {successMsg ? (
        <p className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-900">{successMsg}</p>
      ) : null}

      <section className="mt-6 rounded-xl border border-stone-200 bg-stone-50/80 p-4">
        <h3 className="text-sm font-semibold text-stone-800">Thông tin đơn</h3>
        <div className="mt-3 space-y-3">
          <div>
            <label className="block text-xs font-medium text-stone-600">Loại đơn</label>
            <select
              value={orderType}
              onChange={(e) => setOrderType(e.target.value as OrderType)}
              className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
            >
              {ORDER_TYPES.map((t) => (
                <option key={t} value={t}>
                  {ORDER_TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </div>
          {needsTable ? (
            <TableNumberSelect
              id="edit-order-table"
              value={tableNumber}
              onChange={setTableNumber}
              tables={tablesForSelect}
              loading={tablesLoading}
              error={tablesError}
              showStatus={false}
              emptyHint="Không còn bàn trống. Giữ bàn hiện tại hoặc thử lại sau."
            />
          ) : (
            <p className="text-xs text-stone-500">Đơn giao hàng không cần chọn bàn.</p>
          )}
          <div>
            <label className="block text-xs font-medium text-stone-600">Ghi chú đơn</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              maxLength={300}
              className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
              placeholder="Giao nhanh, ít cay…"
            />
          </div>
        </div>
        <button
          type="button"
          disabled={savingMeta || (needsTable && tableNumber === "")}
          onClick={() => void saveMetadata()}
          className="mt-4 rounded-lg bg-brand-800 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
        >
          {savingMeta ? "Đang lưu…" : "Lưu thông tin đơn"}
        </button>
      </section>

      <section className="mt-6">
        <h3 className="text-sm font-semibold text-stone-800">Món trong đơn</h3>
        {loadingItems ? (
          <p className="mt-2 text-sm text-muted">Đang tải món…</p>
        ) : items.length === 0 ? (
          <p className="mt-2 text-sm text-muted">Chưa có món. Thêm món bên dưới trước khi xác nhận đơn.</p>
        ) : (
          <ul className="mt-3 space-y-3">
            {items.map((line) => {
              const draft = itemDrafts[line.id];
              if (!draft) return null;
              const dirty =
                draft.quantity !== line.quantity ||
                (draft.specialInstructions || "") !== (line.specialInstructions || "");
              return (
                <li
                  key={line.id}
                  className="rounded-xl border border-stone-200 bg-surface p-3 shadow-sm"
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-medium text-stone-900">{line.menuItemName}</p>
                      <p className="text-xs text-muted tabular-nums">
                        {formatVnd(line.unitPrice)} × {draft.quantity} ={" "}
                        <span className="font-medium text-stone-800">
                          {formatVnd(String(Number(line.unitPrice) * draft.quantity))}
                        </span>
                      </p>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        className="h-8 w-8 rounded border border-stone-200 text-stone-600 hover:bg-stone-50 disabled:opacity-40"
                        disabled={draft.quantity <= 1 || savingItemId === line.id}
                        onClick={() =>
                          patchItemDraft(line.id, { quantity: Math.max(1, draft.quantity - 1) })
                        }
                      >
                        −
                      </button>
                      <span className="w-8 text-center text-sm font-medium tabular-nums">
                        {draft.quantity}
                      </span>
                      <button
                        type="button"
                        className="h-8 w-8 rounded border border-stone-200 text-stone-600 hover:bg-stone-50 disabled:opacity-40"
                        disabled={savingItemId === line.id}
                        onClick={() => patchItemDraft(line.id, { quantity: draft.quantity + 1 })}
                      >
                        +
                      </button>
                    </div>
                  </div>
                  <label className="mt-2 block text-xs font-medium text-stone-600">Ghi chú món</label>
                  <input
                    type="text"
                    maxLength={300}
                    value={draft.specialInstructions}
                    onChange={(e) =>
                      patchItemDraft(line.id, { specialInstructions: e.target.value })
                    }
                    className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
                    placeholder="Ít đá, không hành…"
                  />
                  <button
                    type="button"
                    disabled={!dirty || savingItemId === line.id}
                    onClick={() => void saveItem(line.id)}
                    className="mt-2 rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-900 hover:bg-brand-100 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {savingItemId === line.id ? "Đang lưu…" : dirty ? "Lưu món này" : "Đã lưu"}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section className="mt-6 rounded-xl border border-dashed border-brand-200 bg-brand-50/40 p-4">
        <h3 className="text-sm font-semibold text-brand-900">Thêm món</h3>
        {loadingMenu ? (
          <p className="mt-2 text-sm text-muted">Đang tải thực đơn…</p>
        ) : (
          <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="min-w-0 flex-1">
              <label className="block text-xs font-medium text-stone-600">Món</label>
              <select
                value={addMenuItemName}
                onChange={(e) => setAddMenuItemName(e.target.value)}
                className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
              >
                <option value="">Chọn món</option>
                {menuItems.map((m) => (
                  <option key={m.id} value={m.name}>
                    {m.name} — {formatVnd(m.price)}
                  </option>
                ))}
              </select>
            </div>
            <div className="shrink-0">
              <label className="block text-xs font-medium text-stone-600">SL</label>
              <input
                type="number"
                min={1}
                max={99}
                value={addQty}
                onChange={(e) => setAddQty(Math.max(1, Number(e.target.value) || 1))}
                className="mt-1 w-20 rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
              />
            </div>
            <button
              type="button"
              disabled={!addMenuItemName || addingItem}
              onClick={() => void addItem()}
              className="rounded-lg bg-brand-800 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50 sm:shrink-0"
            >
              {addingItem ? "Đang thêm…" : "Thêm"}
            </button>
          </div>
        )}
      </section>
    </EditOrderDialogShell>
  );
}

function EditOrderDialogShell({
  onClose,
  children,
}: {
  onClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="customer-order-edit-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
}
