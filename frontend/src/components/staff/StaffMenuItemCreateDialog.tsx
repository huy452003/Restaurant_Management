"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import {
  digitsOnlyFromPriceField,
  formatVndThousands,
  parseMenuPriceFromDigits,
} from "@/lib/staffMenuItemPrice";
import type { CategoryModel, MenuItemModel, MenuItemStatus, PaginatedResponse } from "@/lib/api/types";

const MENU_STATUSES: MenuItemStatus[] = ["AVAILABLE", "OUT_OF_STOCK", "DISCONTINUED"];

const STATUS_LABEL: Record<MenuItemStatus, string> = {
  AVAILABLE: "Có sẵn",
  OUT_OF_STOCK: "Tạm hết",
  DISCONTINUED: "Ngừng bán",
};

type Props = {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

const PRICE_FIELD_INVALID =
  "border-red-500 focus:border-red-600 focus:ring-red-500/25";

export function StaffMenuItemCreateDialog({ open, onClose, onSaved }: Props) {
  const [categories, setCategories] = useState<CategoryModel[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [priceDigits, setPriceDigits] = useState("");
  const [image, setImage] = useState("");
  const [categoryName, setCategoryName] = useState("");
  const [menuItemStatus, setMenuItemStatus] = useState<MenuItemStatus>("AVAILABLE");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open) return;
    void (async () => {
      setError(null);
      setName("");
      setDescription("");
      setPriceDigits("");
      setImage("");
      setMenuItemStatus("AVAILABLE");
      try {
        const res = await apiFetch<PaginatedResponse<CategoryModel>>(
          `/categories/filters?${buildPageParams(0, 200)}`,
        );
        const list = res.data.content ?? [];
        setCategories(list);
        setCategoryName(list[0]?.name ?? "");
      } catch {
        setCategories([]);
        setCategoryName("");
      }
    })();
  }, [open]);

  if (!open) return null;

  const priceDisplay = formatVndThousands(priceDigits);
  const priceParsed = parseMenuPriceFromDigits(priceDigits);
  const priceFieldInvalid = priceDigits.length > 0 && !priceParsed.ok;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const trimmed = name.trim();
    if (trimmed.length < 3 || trimmed.length > 100) {
      setError("Tên món cần 3–100 ký tự");
      return;
    }
    const priceResult = parseMenuPriceFromDigits(priceDigits);
    if (!priceResult.ok) {
      setError(priceResult.message);
      return;
    }
    const img = image.trim();
    if (!img) {
      setError("Ảnh (URL) không được để trống");
      return;
    }
    if (!categoryName.trim()) {
      setError("Chọn danh mục");
      return;
    }
    const desc = description.trim().slice(0, 255);
    setPending(true);
    try {
      const payload = [
        {
          name: trimmed,
          ...(desc ? { description: desc } : {}),
          price: priceResult.value,
          image: img,
          categoryName: categoryName.trim(),
          menuItemStatus,
        },
      ];
      await apiFetch<MenuItemModel[]>("/menu-items", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Tạo món thất bại");
    } finally {
      setPending(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="create-menu-item-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="create-menu-item-title" className="font-serif text-xl font-semibold text-brand-900">
          Thêm món
        </h2>

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Tên món (3–100 ký tự)
            <input className={fieldClass} value={name} onChange={(e) => setName(e.target.value)} required minLength={3} maxLength={100} />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Mô tả (tối đa 255 ký tự)
            <textarea
              className={`${fieldClass} min-h-[72px] resize-y`}
              value={description}
              onChange={(e) => setDescription(e.target.value.slice(0, 255))}
              maxLength={255}
              rows={2}
            />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Giá (VND)
            <input
              className={`${fieldClass} ${priceFieldInvalid ? PRICE_FIELD_INVALID : ""}`}
              type="text"
              inputMode="numeric"
              autoComplete="off"
              placeholder=""
              value={priceDisplay}
              onChange={(e) => setPriceDigits(digitsOnlyFromPriceField(e.target.value))}
              required
            />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Ảnh (URL)
            <input className={fieldClass} value={image} onChange={(e) => setImage(e.target.value)} required />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Danh mục
            <select
              className={fieldClass}
              value={categoryName}
              onChange={(e) => setCategoryName(e.target.value)}
              required
              disabled={categories.length === 0}
            >
              {categories.map((c) => (
                <option key={c.id} value={c.name}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Trạng thái món
            <select
              className={fieldClass}
              value={menuItemStatus}
              onChange={(e) => setMenuItemStatus(e.target.value as MenuItemStatus)}
              required
            >
              {MENU_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </select>
          </label>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={pending || categories.length === 0}
              className="rounded-lg border border-brand-200 bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {pending ? "Đang tạo…" : "Tạo"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
