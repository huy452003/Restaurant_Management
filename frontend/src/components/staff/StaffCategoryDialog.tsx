"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { CategoryModel } from "@/lib/api/types";

const CATEGORY_STATUSES = ["AVAILABLE", "OUT_OF_STOCK", "DISCONTINUED"] as const;
type CategoryStatusValue = (typeof CATEGORY_STATUSES)[number];

const STATUS_LABEL: Record<CategoryStatusValue, string> = {
  AVAILABLE: "Có sẵn",
  OUT_OF_STOCK: "Tạm hết hàng",
  DISCONTINUED: "Ngừng kinh doanh",
};

export type StaffCategoryDialogProps = {
  open: boolean;
  mode: "create" | "edit";
  row: CategoryModel | null;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

function isCategoryStatus(s: string): s is CategoryStatusValue {
  return (CATEGORY_STATUSES as readonly string[]).includes(s);
}

export function StaffCategoryDialog({ open, mode, row, onClose, onSaved }: StaffCategoryDialogProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [image, setImage] = useState("");
  const [categoryStatus, setCategoryStatus] = useState<CategoryStatusValue>("AVAILABLE");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (mode === "create") {
      void Promise.resolve().then(() => {
        setName("");
        setDescription("");
        setImage("");
        setCategoryStatus("AVAILABLE");
        setError(null);
      });
      return;
    }
    if (!row) return;
    void Promise.resolve().then(() => {
      setName(row.name ?? "");
      setDescription(row.description ?? "");
      setImage(row.image ?? "");
      setCategoryStatus(isCategoryStatus(row.categoryStatus) ? row.categoryStatus : "AVAILABLE");
      setError(null);
    });
  }, [open, mode, row]);

  if (!open || (mode === "edit" && !row)) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (mode === "edit" && !row) return;
    setError(null);
    const trimmed = name.trim();
    if (trimmed.length < 3 || trimmed.length > 50) {
      setError("Tên danh mục cần 3–50 ký tự");
      return;
    }
    const img = image.trim();
    if (!img) {
      setError("Đường dẫn ảnh không được để trống");
      return;
    }
    const desc = description.trim().slice(0, 255);
    setPending(true);
    try {
      if (mode === "create") {
        await apiFetch<CategoryModel[]>("/categories", {
          method: "POST",
          body: JSON.stringify([
            {
              name: trimmed,
              description: desc,
              image: img,
              categoryStatus,
            },
          ]),
        });
      } else if (row) {
        const update = {
          name: trimmed,
          description: desc,
          image: img,
          categoryStatus,
        };
        await apiFetch<CategoryModel[]>("/categories", {
          method: "PUT",
          body: JSON.stringify({ ids: [row.id], updates: [update] }),
        });
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : mode === "create" ? "Tạo thất bại" : "Cập nhật thất bại");
    } finally {
      setPending(false);
    }
  }

  const titleId = mode === "create" ? "create-category-title" : "edit-category-title";

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
        aria-labelledby={titleId}
        onMouseDown={(e) => e.stopPropagation()}
      >
        {mode === "create" ? (
          <h2 id={titleId} className="font-serif text-xl font-semibold text-brand-900">
            Thêm danh mục
          </h2>
        ) : (
          <h2 id={titleId} className="font-serif text-xl font-semibold text-brand-900">
            Sửa danh mục{" "}
            <span className="font-mono text-[1.0625rem] font-semibold tabular-nums tracking-normal text-brand-900">
              #{row!.id}
            </span>
          </h2>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Tên danh mục (3–50 ký tự)
            <input className={fieldClass} value={name} onChange={(e) => setName(e.target.value)} required minLength={3} maxLength={50} />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Mô tả (tối đa 255 ký tự)
            <textarea
              className={`${fieldClass} min-h-[88px] resize-y`}
              value={description}
              onChange={(e) => setDescription(e.target.value.slice(0, 255))}
              maxLength={255}
              rows={3}
            />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Ảnh (URL)
            <input className={fieldClass} value={image} onChange={(e) => setImage(e.target.value)} required />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Trạng thái
            <select
              className={fieldClass}
              value={categoryStatus}
              onChange={(e) => setCategoryStatus(e.target.value as CategoryStatusValue)}
              required
            >
              {CATEGORY_STATUSES.map((s) => (
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
              disabled={pending}
              className="rounded-lg border border-brand-200 bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {pending ? "Đang lưu…" : mode === "create" ? "Tạo" : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
