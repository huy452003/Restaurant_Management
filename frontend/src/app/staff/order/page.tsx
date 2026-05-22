"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffServeOrderPanel } from "@/components/staff/StaffServeOrderPanel";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { useServeCart } from "@/hooks/use-serve-cart";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { CategoryModel, MenuItemModel, OrderModel, PaginatedResponse, UserRole } from "@/lib/api/types";
import type { FilterValues } from "@/lib/list/utils";

const AVAILABLE_FILTER = { menuItemStatus: "AVAILABLE" };

function buildMenuFilters(category: string | null, name: string): FilterValues {
  const filters: FilterValues = { ...AVAILABLE_FILTER };
  if (category) filters.categoryName = category;
  const trimmed = name.trim();
  if (trimmed) filters.name = trimmed;
  return filters;
}
const SERVE_ROLES: UserRole[] = ["ADMIN", "MANAGER", "CASHIER"];

function canServeOrder(role: UserRole): boolean {
  return SERVE_ROLES.includes(role);
}

export default function StaffOrderPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  const [categories, setCategories] = useState<CategoryModel[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [categoriesError, setCategoriesError] = useState<string | null>(null);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const [notes, setNotes] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [lastOrderNumber, setLastOrderNumber] = useState<string | null>(null);
  const [nameQuery, setNameQuery] = useState("");

  const {
    lines,
    addItem,
    incrementItem,
    decrementItem,
    clearCart,
  } = useServeCart();

  const { tables, loading: tablesLoading, error: tablesError, reload: reloadTables } = useRestaurantTables({
    tableStatus: "AVAILABLE",
    freshSnapshot: true,
  });

  const {
    rows: items,
    page,
    totalPages,
    totalElements,
    loading: loadingItems,
    error: itemsError,
    goToPage,
    loadInitial,
    search,
  } = usePaginatedList<MenuItemModel>({
    basePath: "/menu-items/filters",
    pageSize: 9,
    initialFilters: AVAILABLE_FILTER,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/order");
      return;
    }
    if (!canServeOrder(user.role)) {
      router.replace("/staff");
      return;
    }
    loadInitial();
    let cancelled = false;
    (async () => {
      setLoadingCategories(true);
      setCategoriesError(null);
      try {
        const catQs = buildPageParams(0, 100, { categoryStatus: "AVAILABLE" });
        const catRes = await apiFetch<PaginatedResponse<CategoryModel>>(`/categories/filters?${catQs}`);
        if (!cancelled) setCategories(catRes.data.content ?? []);
      } catch (e) {
        if (!cancelled) {
          setCategoriesError(e instanceof ApiError ? e.message : "Không tải được danh mục");
        }
      } finally {
        if (!cancelled) setLoadingCategories(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user, authLoading, router, loadInitial]);

  useEffect(() => {
    if (tables.length === 0) return;
    if (tableNumber === "" || !tables.some((t) => t.tableNumber === tableNumber)) {
      setTableNumber(tables[0].tableNumber);
    }
  }, [tables, tableNumber]);

  const selectCategory = useCallback(
    (category: string | null) => {
      setSelectedCategory(category);
      search(buildMenuFilters(category, nameQuery));
    },
    [search, nameQuery],
  );

  function applyNameFilter() {
    search(buildMenuFilters(selectedCategory, nameQuery));
  }

  function clearNameFilter() {
    setNameQuery("");
    search(buildMenuFilters(selectedCategory, ""));
  }

  async function submitOrder() {
    if (lines.length === 0 || tableNumber === "") return;
    setMessage(null);
    setLastOrderNumber(null);
    setSubmitting(true);
    try {
      const orderRes = await apiFetch<OrderModel>("/orders", {
        method: "POST",
        body: JSON.stringify({
          tableNumber,
          orderType: "DINE_IN",
          notes: notes.trim() || undefined,
        }),
      });
      const order = orderRes.data;
      const orderNumber = order.orderNumber;
      await apiFetch<unknown[]>("/order-items", {
        method: "POST",
        body: JSON.stringify(
          lines.map((line) => ({
            orderNumber,
            menuItemName: line.item.name,
            quantity: line.quantity,
          })),
        ),
      });
      await apiFetch<OrderModel>(`/orders/submit/${order.id}`, { method: "PATCH" });
      clearCart();
      setNotes("");
      setLastOrderNumber(orderNumber);
      setMessage(`Đã gửi đơn ${orderNumber} tới bếp.`);
      void reloadTables();
    } catch (e) {
      setMessage(e instanceof ApiError ? e.message : "Không gửi được đơn");
      void reloadTables();
    } finally {
      setSubmitting(false);
    }
  }

  const fetchError = itemsError ?? categoriesError;
  const loadingMenu = loadingCategories || (loadingItems && items.length === 0);

  if (authLoading || !user || !canServeOrder(user.role)) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted">
        Đang kiểm tra quyền…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <StaffBackLink />
      <header className="mt-2 rounded-2xl border border-stone-200/80 bg-gradient-to-r from-brand-50/80 via-surface to-surface px-5 py-5 shadow-sm sm:px-6">
        <h1
          className="font-serif text-3xl font-semibold tracking-tight text-brand-900 sm:text-4xl"
          style={{ fontFamily: "var(--font-cormorant), serif" }}
        >
          Đặt món
        </h1>
        <p className="mt-1.5 max-w-xl text-sm leading-relaxed text-muted">
          Ghi đơn tại quầy — chọn bàn, thêm món và xác nhận gửi bếp.
        </p>
      </header>

      {fetchError ? (
        <p className="mt-4 rounded-xl bg-red-50 p-4 text-sm text-red-800 ring-1 ring-red-100">{fetchError}</p>
      ) : null}

      {loadingMenu ? (
        <div className="mt-8 grid gap-3 sm:grid-cols-2 2xl:grid-cols-3" aria-busy="true">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-14 animate-pulse rounded-xl bg-stone-200/60" />
          ))}
        </div>
      ) : (
        <div className="mt-6 flex flex-col gap-6 xl:flex-row xl:items-start">
          <div className="order-2 min-w-0 flex-1 xl:order-1">
            <div className="rounded-2xl border border-stone-200/90 bg-surface p-4 shadow-sm sm:p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-stone-500">Tìm kiếm</p>
              <div className="mt-2 flex flex-col gap-2 sm:flex-row sm:items-center">
              <label htmlFor="staff-order-name-filter" className="sr-only">
                Tìm theo tên món
              </label>
              <input
                id="staff-order-name-filter"
                type="search"
                value={nameQuery}
                onChange={(e) => setNameQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") applyNameFilter();
                }}
                placeholder="Tìm theo tên món…"
                className="min-w-0 flex-1 rounded-xl border border-stone-200 bg-stone-50/50 px-3.5 py-2.5 text-sm text-stone-900 outline-none transition focus:border-brand-300 focus:bg-white focus:ring-2 focus:ring-brand-600/20"
              />
              <div className="flex shrink-0 gap-2">
                <button
                  type="button"
                  onClick={applyNameFilter}
                  className="rounded-xl bg-brand-800 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-900"
                >
                  Tìm
                </button>
                {nameQuery.trim() ? (
                  <button
                    type="button"
                    onClick={clearNameFilter}
                    className="rounded-xl border border-stone-200 bg-white px-4 py-2.5 text-sm font-medium text-stone-600 transition hover:bg-stone-50"
                  >
                    Xóa
                  </button>
                ) : null}
              </div>
            </div>

              <p className="mt-4 text-xs font-semibold uppercase tracking-wide text-stone-500">Danh mục</p>
              <nav className="mt-2 flex flex-wrap gap-2" aria-label="Danh mục món">
              <CategoryChip
                label="Tất cả"
                active={selectedCategory === null}
                onClick={() => selectCategory(null)}
              />
              {categories.map((cat) => (
                <CategoryChip
                  key={cat.id}
                  label={cat.name}
                  active={selectedCategory === cat.name}
                  onClick={() => selectCategory(cat.name)}
                />
              ))}
              </nav>
            </div>

            <div className="mt-5 flex items-baseline justify-between gap-3">
              <h2 className="text-sm font-semibold text-stone-800">Thực đơn</h2>
              {!loadingItems && totalElements > 0 ? (
                <span className="text-xs tabular-nums text-muted">{totalElements} món</span>
              ) : null}
            </div>

            {items.length === 0 ? (
              <p className="mt-3 rounded-2xl border border-dashed border-stone-200 bg-stone-50/50 px-4 py-12 text-center text-sm text-muted">
                {nameQuery.trim()
                  ? "Không tìm thấy món phù hợp."
                  : selectedCategory
                    ? `Chưa có món trong danh mục «${selectedCategory}».`
                    : "Hiện chưa có món nào."}
              </p>
            ) : (
              <ul className="mt-3 grid gap-2.5 sm:grid-cols-2 2xl:grid-cols-3">
                {items.map((item) => (
                  <li key={item.id}>
                    <MenuItemRow name={item.name} onAdd={() => addItem(item)} />
                  </li>
                ))}
              </ul>
            )}

            {!loadingItems && totalElements > 0 ? (
              <div className="mt-4">
                <PaginationBar
                  page={page}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  loading={loadingItems}
                  onPageChange={goToPage}
                  unitLabel="món"
                />
              </div>
            ) : null}
          </div>

          <aside className="order-1 w-full shrink-0 xl:order-2 xl:w-[22rem]">
            <StaffServeOrderPanel
              lines={lines}
              tableNumber={tableNumber}
              onTableNumberChange={setTableNumber}
              tables={tables}
              tablesLoading={tablesLoading}
              tablesError={tablesError}
              notes={notes}
              onNotesChange={setNotes}
              onIncrement={incrementItem}
              onDecrement={decrementItem}
              onClearCart={clearCart}
              onSubmit={() => void submitOrder()}
              submitting={submitting}
              message={message}
              lastOrderNumber={lastOrderNumber}
            />
          </aside>
        </div>
      )}
    </div>
  );
}

function MenuItemRow({ name, onAdd }: { name: string; onAdd: () => void }) {
  const initial = name.trim().charAt(0).toUpperCase() || "?";

  return (
    <article className="group flex items-center gap-3 rounded-xl border border-stone-200/90 bg-surface px-3 py-3 shadow-sm transition hover:border-brand-200 hover:shadow-md">
      <span
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-sm font-semibold text-brand-800 ring-1 ring-brand-100 transition group-hover:bg-brand-100"
        aria-hidden
      >
        {initial}
      </span>
      <h3 className="min-w-0 flex-1 truncate text-sm font-medium leading-snug text-stone-900">{name}</h3>
      <button
        type="button"
        onClick={onAdd}
        className="shrink-0 rounded-lg bg-brand-800 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-brand-900 active:scale-95"
      >
        + Thêm
      </button>
    </article>
  );
}

function CategoryChip({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`shrink-0 rounded-full border px-3.5 py-1.5 text-sm font-medium transition active:scale-95 ${
        active
          ? "border-brand-800 bg-brand-800 text-white shadow-sm"
          : "border-stone-200 bg-white text-stone-600 hover:border-brand-200 hover:text-brand-900"
      }`}
    >
      {label}
    </button>
  );
}
