"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { PaginationBar } from "@/components/list/PaginationBar";
import { useAuth } from "@/context/auth-context";
import { useCart } from "@/context/cart-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { CategoryModel, MenuItemModel, PaginatedResponse } from "@/lib/api/types";
import { PageHeading } from "@/components/ui/PageHeading";
import { btnPrimaryClass, cardClass, fontSerif } from "@/lib/ui/bakery";
import { formatVnd } from "@/lib/money";

const AVAILABLE_FILTER = { menuItemStatus: "AVAILABLE" };

export default function MenuPage() {
  const { user, loading: authLoading } = useAuth();
  const { addItem } = useCart();
  const router = useRouter();
  const [categories, setCategories] = useState<CategoryModel[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [categoriesError, setCategoriesError] = useState<string | null>(null);
  const [loadingCategories, setLoadingCategories] = useState(true);

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
      router.replace("/login?next=/menu");
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
        if (!cancelled) {
          setCategories(catRes.data.content ?? []);
        }
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

  const addToCart = useCallback((item: MenuItemModel) => addItem(item), [addItem]);

  function selectCategory(name: string | null) {
    setSelectedCategory(name);
    if (name) {
      search({ ...AVAILABLE_FILTER, categoryName: name });
    } else {
      search(AVAILABLE_FILTER);
    }
  }

  const fetchError = itemsError ?? categoriesError;
  const loadingMenu = loadingCategories || (loadingItems && items.length === 0);

  if (authLoading || !user) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted">
        Đang kiểm tra phiên đăng nhập…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <PageHeading
        title="Thực đơn"
        subtitle="Chọn món yêu thích và thêm vào giỏ — giao diện thẻ sản phẩm ấm áp, dễ duyệt."
      />

      {fetchError ? (
        <p className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-800 ring-1 ring-red-100">{fetchError}</p>
      ) : null}

      {loadingMenu ? (
        <p className="mt-10 text-muted">Đang tải món…</p>
      ) : (
        <div className="mt-6 flex flex-col gap-6 lg:flex-row lg:items-start">
          <aside className="w-full shrink-0 lg:w-60 xl:w-64">
            <div className={`${cardClass} p-3 lg:sticky lg:top-24 lg:p-4`}>
              <p className="mb-3 hidden px-1 text-xs font-semibold uppercase tracking-wide text-muted lg:block">
                Danh mục
              </p>
              <nav
                className="flex gap-2 overflow-x-auto pb-1 lg:flex-col lg:gap-1 lg:overflow-visible lg:pb-0"
                aria-label="Danh mục món"
              >
                <CategorySidebarItem
                  label="Tất cả món"
                  active={selectedCategory === null}
                  onClick={() => selectCategory(null)}
                  variant="all"
                />
                {categories.map((cat) => (
                  <CategorySidebarItem
                    key={cat.id}
                    label={cat.name}
                    image={cat.image}
                    active={selectedCategory === cat.name}
                    onClick={() => selectCategory(cat.name)}
                  />
                ))}
              </nav>
            </div>
          </aside>

          <main className="min-w-0 flex-1">

            {items.length === 0 ? (
              <p className="rounded-xl bg-stone-50 px-4 py-8 text-center text-sm text-muted ring-1 ring-stone-200">
                {selectedCategory
                  ? `Chưa có món trong danh mục «${selectedCategory}».`
                  : "Hiện chưa có món nào."}
              </p>
            ) : (
              <ul className="grid gap-8 sm:grid-cols-2 xl:grid-cols-3">
                {items.map((item) => (
                  <li
                    key={item.id}
                    className={`${cardClass} group flex flex-col overflow-hidden transition hover:-translate-y-0.5`}
                  >
                    <div className="relative aspect-[4/3] w-full overflow-hidden bg-brand-50">
                      {item.image?.startsWith("http") ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={item.image}
                          alt={item.name}
                          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                        />
                      ) : (
                        <div className="flex h-full items-center justify-center text-sm text-muted">Ảnh món</div>
                      )}
                    </div>
                    <div className="flex flex-1 flex-col p-5">
                      <p className="text-xs font-semibold uppercase tracking-wider text-accent">
                        {item.categoryName}
                      </p>
                      <h2 className="mt-1 font-serif text-lg font-semibold text-brand-900" style={fontSerif}>
                        {item.name}
                      </h2>
                      {item.description ? (
                        <p className="mt-2 line-clamp-2 flex-1 text-sm leading-relaxed text-muted">
                          {item.description}
                        </p>
                      ) : (
                        <div className="flex-1" />
                      )}
                      <div className="mt-4 flex items-center justify-between gap-3 border-t border-brand-50 pt-4">
                        <p className="text-lg font-semibold text-brand-800">{formatVnd(item.price)}</p>
                        <button type="button" onClick={() => addToCart(item)} className={btnPrimaryClass}>
                          Thêm
                        </button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {!loadingItems && totalElements > 0 ? (
              <PaginationBar
                page={page}
                totalPages={totalPages}
                totalElements={totalElements}
                loading={loadingItems}
                onPageChange={goToPage}
                unitLabel="món"
              />
            ) : null}
          </main>
        </div>
      )}
    </div>
  );
}

function CategorySidebarItem({
  label,
  image,
  active,
  onClick,
  variant = "category",
}: {
  label: string;
  image?: string;
  active: boolean;
  onClick: () => void;
  variant?: "all" | "category";
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      className={`group flex min-w-[11rem] shrink-0 items-center gap-3 rounded-2xl px-2.5 py-2 text-left transition lg:min-w-0 lg:w-full lg:px-3 lg:py-2.5 ${
        active
          ? "bg-brand-800 text-white shadow-sm lg:border-l-0"
          : "hover:bg-brand-50 lg:hover:ring-1 lg:hover:ring-brand-100"
      }`}
    >
      <span className="flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-stone-100 ring-1 ring-stone-200/80">
        {variant === "all" ? (
          <AllCategoriesIcon active={active} />
        ) : image?.startsWith("http") ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={image} alt={label} className="h-full w-full object-cover" />
        ) : (
          <span
            className={`flex h-full w-full items-center justify-center text-sm font-semibold ${
              active ? "bg-brand-100 text-brand-800" : "bg-stone-200/80 text-stone-500"
            }`}
          >
            {label.charAt(0).toUpperCase()}
          </span>
        )}
      </span>
      <span
        className={`line-clamp-2 text-sm font-medium leading-snug ${
          active ? "text-white" : "text-brand-800 group-hover:text-brand-900"
        }`}
      >
        {label}
      </span>
    </button>
  );
}

function AllCategoriesIcon({ active }: { active: boolean }) {
  return (
    <svg
      className={`h-6 w-6 ${active ? "text-brand-800" : "text-stone-500"}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      aria-hidden
    >
      <path strokeLinecap="round" d="M4 6h7M4 12h16M4 18h11" />
      <circle cx="17" cy="6" r="2" fill="currentColor" stroke="none" opacity={active ? 1 : 0.45} />
      <circle cx="20" cy="18" r="2" fill="currentColor" stroke="none" opacity={active ? 1 : 0.45} />
    </svg>
  );
}
