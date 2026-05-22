"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import { CustomerOrderEditDialog } from "@/components/customer/CustomerOrderEditDialog";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { OrderDetailDialog } from "@/components/OrderDetailDialog";
import { StaffPaymentCreateDialog } from "@/components/staff/StaffPaymentCreateDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { OrderModel } from "@/lib/api/types";
import { LIST_EXTRA_SORT_NEWEST, ORDER_ADMIN_LIST_FILTERS, ORDER_LIST_FILTERS } from "@/lib/list/presets";
import {
  canCustomerCancelOrder,
  canCustomerEditOrder,
  canCustomerSubmitOrder,
} from "@/lib/orders/customer-order-actions";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { ORDER_TYPE_LABEL } from "@/lib/orders/order-labels";
import { canCustomerPayDeliveryOrder } from "@/lib/orders/customer-delivery-payment";
import { canCreatePaymentForOrder } from "@/lib/orders/order-payment";
import { PageHeading } from "@/components/ui/PageHeading";
import { btnSecondaryClass, cardClass } from "@/lib/ui/bakery";
import { formatVnd } from "@/lib/money";

function OrdersPageContent() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const paymentNotice = searchParams.get("payment");
  const paymentOrderNumber = searchParams.get("orderNumber")?.trim() ?? "";
  const [busyId, setBusyId] = useState<number | null>(null);
  const [detailOrder, setDetailOrder] = useState<OrderModel | null>(null);
  const [editingOrder, setEditingOrder] = useState<OrderModel | null>(null);
  const [payingOrder, setPayingOrder] = useState<OrderModel | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const canManagePayment = hasRole("CASHIER", "MANAGER", "ADMIN");
  const isCustomer = hasRole("CUSTOMER");

  const orderListConfig = useMemo(
    () => ({
      basePath: isCustomer ? "/orders/filters" : "/orders/filters/admin",
      filterFields: isCustomer ? ORDER_LIST_FILTERS : ORDER_ADMIN_LIST_FILTERS,
    }),
    [isCustomer],
  );

  const {
    rows: orders,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    draftFilters,
    setFilter,
    applyFilters,
    resetFilters,
    reload,
    goToPage,
    loadInitial,
  } = usePaginatedList<OrderModel>({
    basePath: orderListConfig.basePath,
    pageSize: 10,
    extraParams: LIST_EXTRA_SORT_NEWEST,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/orders");
      return;
    }
    loadInitial();
  }, [user, authLoading, router, loadInitial, orderListConfig.basePath]);

  async function submitOrder(id: number) {
    setBusyId(id);
    setActionError(null);
    try {
      await apiFetch<OrderModel>(`/orders/submit/${id}`, { method: "PATCH" });
      reload();
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "Gửi đơn thất bại");
    } finally {
      setBusyId(null);
    }
  }

  async function cancelOrder(id: number) {
    setBusyId(id);
    setActionError(null);
    try {
      await apiFetch<OrderModel>(`/orders/cancel/${id}`, { method: "PATCH" });
      reload();
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "Hủy thất bại");
    } finally {
      setBusyId(null);
    }
  }

  if (authLoading || !user) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <PageHeading
        title="Đơn hàng"
        subtitle={
          hasRole("CUSTOMER")
            ? "Xác nhận đơn của bạn khi sẵn sàng."
            : "Theo dõi toàn bộ đơn trong hệ thống."
        }
        action={
          <Link href="/menu" className={btnSecondaryClass}>
            Thêm món
          </Link>
        }
      />

      {paymentNotice === "cancelled" ? (
        <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900 ring-1 ring-amber-100">
          Bạn đã hủy thanh toán
          {paymentOrderNumber ? ` cho đơn ${paymentOrderNumber}` : ""}. Bạn có thể chọn{" "}
          <span className="font-semibold">Thanh toán lại</span> bên dưới.
        </p>
      ) : null}
      {paymentNotice === "success" ? (
        <p className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-900 ring-1 ring-emerald-100">
          Thanh toán VNPAY thành công
          {paymentOrderNumber ? ` cho đơn ${paymentOrderNumber}` : ""}.
        </p>
      ) : null}

      {error || actionError ? (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{actionError ?? error}</p>
      ) : null}

      <FilterBar
        fields={orderListConfig.filterFields}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {loading && orders.length === 0 ? (
        <p className="mt-8 text-muted">Đang tải đơn…</p>
      ) : orders.length === 0 ? (
        <p className="mt-8 rounded-xl border border-dashed border-stone-200 bg-stone-50 p-8 text-center text-muted">
          Chưa có đơn.{" "}
          <Link href="/menu" className="font-medium text-brand-800 underline">
            Đặt món
          </Link>
        </p>
      ) : (
        <ul className="mt-8 space-y-4">
          {orders.map((o) => {
            const showSubmit = canCustomerSubmitOrder(o.orderStatus);
            const showCancel = canCustomerCancelOrder(o);
            const showEdit = hasRole("CUSTOMER") && canCustomerEditOrder(o.orderStatus);
            const showCustomerPay =
              hasRole("CUSTOMER") && canCustomerPayDeliveryOrder(o);
            const showStaffPay = canManagePayment && canCreatePaymentForOrder(o);
            return (
            <li
              key={o.id}
              className={`${cardClass} p-5`}
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-mono text-sm font-semibold text-brand-900">{o.orderNumber}</p>
                  <p className="mt-1 text-sm text-muted">
                    {o.tableNumber != null ? `Bàn ${o.tableNumber} · ` : ""}
                    {ORDER_TYPE_LABEL[o.orderType]} ·{" "}
                    <StatusBadge domain="order" status={o.orderStatus} className="align-middle" />
                  </p>
                  {o.totalAmount != null ? (
                    <p className="mt-2 text-lg font-semibold text-brand-800">{formatVnd(o.totalAmount)}</p>
                  ) : null}
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => setDetailOrder(o)}
                    className="rounded-lg border border-stone-200 px-3 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50"
                  >
                    Chi tiết
                  </button>
                  {showEdit ? (
                    <button
                      type="button"
                      onClick={() => setEditingOrder(o)}
                      className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-2 text-sm font-semibold text-brand-900 hover:bg-brand-100"
                    >
                      Chỉnh sửa
                    </button>
                  ) : null}
                  {hasRole("CUSTOMER") && showSubmit ? (
                    <button
                      type="button"
                      disabled={busyId === o.id}
                      onClick={() => void submitOrder(o.id)}
                      className="rounded-lg bg-brand-800 px-3 py-2 text-sm font-medium text-white hover:bg-brand-900 disabled:opacity-50"
                    >
                      Xác nhận đơn
                    </button>
                  ) : null}
                  {hasRole("CUSTOMER") && showCancel ? (
                    <button
                      type="button"
                      disabled={busyId === o.id}
                      onClick={() => void cancelOrder(o.id)}
                      className="rounded-lg border border-red-200 px-3 py-2 text-sm font-medium text-red-800 hover:bg-red-50 disabled:opacity-50"
                    >
                      Hủy đơn
                    </button>
                  ) : null}
                  {showCustomerPay ? (
                    <button
                      type="button"
                      onClick={() =>
                        router.push(
                          `/checkout/payment?orderNumber=${encodeURIComponent(o.orderNumber)}`,
                        )
                      }
                      className="rounded-lg border border-violet-200 bg-violet-50 px-3 py-2 text-sm font-semibold text-violet-900 hover:bg-violet-100"
                    >
                      Thanh toán lại
                    </button>
                  ) : null}
                  {showStaffPay ? (
                    <button
                      type="button"
                      onClick={() => setPayingOrder(o)}
                      className="rounded-lg border border-violet-200 bg-violet-50 px-3 py-2 text-sm font-semibold text-violet-900 hover:bg-violet-100"
                    >
                      Thanh toán
                    </button>
                  ) : null}
                </div>
              </div>
            </li>
            );
          })}
        </ul>
      )}

      {!loading && orders.length > 0 ? (
        <PaginationBar
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          loading={loading}
          onPageChange={goToPage}
          unitLabel="đơn"
        />
      ) : null}

      <OrderDetailDialog
        open={detailOrder != null}
        order={detailOrder}
        onClose={() => setDetailOrder(null)}
        onEdit={
          detailOrder && hasRole("CUSTOMER") && canCustomerEditOrder(detailOrder.orderStatus)
            ? () => {
                setDetailOrder(null);
                setEditingOrder(detailOrder);
              }
            : undefined
        }
      />

      <CustomerOrderEditDialog
        open={editingOrder != null}
        order={editingOrder}
        onClose={() => setEditingOrder(null)}
        onSaved={reload}
      />

      {payingOrder != null ? (
        <StaffPaymentCreateDialog
          open
          initialOrderNumber={payingOrder.orderNumber}
          onClose={() => setPayingOrder(null)}
          onSaved={reload}
        />
      ) : null}
    </div>
  );
}

export default function OrdersPage() {
  return (
    <Suspense fallback={<div className="py-20 text-center text-muted">Đang tải…</div>}>
      <OrdersPageContent />
    </Suspense>
  );
}
