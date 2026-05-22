"use client";

import Link from "next/link";
import { useCart } from "@/context/cart-context";
import { formatVnd } from "@/lib/money";

export function CartItemsPanel() {
  const { lines, incrementItem, decrementItem } = useCart();

  if (lines.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-stone-200 bg-stone-50/80 px-6 py-16 text-center">
        <p className="text-muted">Chưa có món trong giỏ.</p>
        <Link
          href="/menu"
          className="mt-4 inline-block rounded-xl bg-brand-800 px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-900"
        >
          Xem thực đơn
        </Link>
      </div>
    );
  }

  return (
    <ul className="grid gap-6 sm:grid-cols-2">
      {lines.map((line) => (
        <li
          key={line.item.id}
          className="flex gap-4 overflow-hidden rounded-2xl border border-stone-200 bg-surface p-4 shadow-sm"
        >
          <div className="relative h-28 w-28 shrink-0 overflow-hidden rounded-xl bg-stone-100">
            {line.item.image?.startsWith("http") ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={line.item.image} alt="" className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-muted">Ảnh</div>
            )}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-xs font-medium uppercase tracking-wide text-accent">{line.item.categoryName}</p>
            <h2 className="mt-0.5 font-semibold text-stone-900">{line.item.name}</h2>
            {line.item.description ? (
              <p className="mt-1 line-clamp-2 text-sm text-muted">{line.item.description}</p>
            ) : null}
            <p className="mt-2 text-lg font-semibold text-brand-800">
              {formatVnd(Number(line.item.price) * line.quantity)}
              <span className="ml-1 text-sm font-normal text-stone-500">
                ( × {line.quantity} )
              </span>
            </p>
            <div className="mt-3 flex items-center gap-2">
              <button
                type="button"
                className="h-9 w-9 rounded-lg border border-stone-200 text-stone-600 hover:bg-stone-50"
                onClick={() => decrementItem(line.item.id)}
                aria-label="Giảm số lượng"
              >
                −
              </button>
              <span className="min-w-8 text-center text-sm font-semibold">{line.quantity}</span>
              <button
                type="button"
                className="h-9 w-9 rounded-lg border border-stone-200 text-stone-600 hover:bg-stone-50"
                onClick={() => incrementItem(line.item.id)}
                aria-label="Tăng số lượng"
              >
                +
              </button>
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}
