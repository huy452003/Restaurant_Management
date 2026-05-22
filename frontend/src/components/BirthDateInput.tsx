"use client";

import {
  useEffect,
  useId,
  useRef,
  useState,
  type ClipboardEvent,
  type KeyboardEvent,
  type RefObject,
} from "react";
import {
  constrainBirthParts,
  deserializeBirthField,
  getBirthFieldError,
  parsePastedBirth,
  serializeBirthField,
  shouldAutoAdvanceBirthPart,
  type BirthParts,
} from "@/lib/birth";

type Props = {
  value: string;
  onChange: (value: string) => void;
  /** Báo lỗi ra form cha (vô hiệu / làm mờ nút Lưu khi có message). */
  onValidationChange?: (error: string | null) => void;
  className?: string;
  /** compact: cột cố định (account); fill: 3 cột full width (auth register) */
  layout?: "compact" | "fill";
  required?: boolean;
  id?: string;
};

type PartKey = keyof BirthParts;

const PART_LIMIT: Record<PartKey, number> = { day: 2, month: 2, year: 4 };
const PART_ORDER: PartKey[] = ["day", "month", "year"];

export function BirthDateInput({
  value,
  onChange,
  onValidationChange,
  className,
  layout = "compact",
  required,
  id,
}: Props) {
  const autoId = useId();
  const groupId = id ?? autoId;

  const [parts, setParts] = useState<BirthParts>(() => deserializeBirthField(value));
  const [touched, setTouched] = useState(false);
  const lastEmitted = useRef(value);

  const dayRef = useRef<HTMLInputElement>(null);
  const monthRef = useRef<HTMLInputElement>(null);
  const yearRef = useRef<HTMLInputElement>(null);
  const refs: Record<PartKey, RefObject<HTMLInputElement | null>> = {
    day: dayRef,
    month: monthRef,
    year: yearRef,
  };

  useEffect(() => {
    if (value !== lastEmitted.current) {
      setParts(deserializeBirthField(value));
      lastEmitted.current = value;
    }
  }, [value]);

  const serialized = serializeBirthField(parts);
  const error = getBirthFieldError(serialized, touched);

  useEffect(() => {
    onValidationChange?.(error);
  }, [error, onValidationChange]);

  const segmentClass = [
    className,
    "w-full tabular-nums text-center",
    error ? "border-red-500 focus:border-red-600 focus:ring-red-600/25" : "",
  ]
    .filter(Boolean)
    .join(" ");

  function emit(next: BirthParts) {
    setParts(next);
    const nextSerialized = serializeBirthField(next);
    lastEmitted.current = nextSerialized;
    onChange(nextSerialized);
  }

  function focusPart(key: PartKey) {
    refs[key].current?.focus();
    refs[key].current?.select();
  }

  function updatePart(key: PartKey, raw: string) {
    const draft: BirthParts = { ...parts, [key]: raw };
    const next = constrainBirthParts(draft);
    emit(next);

    if (shouldAutoAdvanceBirthPart(key, next[key])) {
      const idx = PART_ORDER.indexOf(key);
      const nextKey = PART_ORDER[idx + 1];
      if (nextKey) focusPart(nextKey);
    }
  }

  function onPartKeyDown(key: PartKey, e: KeyboardEvent<HTMLInputElement>) {
    if (e.key !== "Backspace" || parts[key].length > 0) return;
    const idx = PART_ORDER.indexOf(key);
    const prevKey = PART_ORDER[idx - 1];
    if (prevKey) {
      e.preventDefault();
      focusPart(prevKey);
    }
  }

  function onPaste(e: ClipboardEvent<HTMLDivElement>) {
    const parsed = parsePastedBirth(e.clipboardData.getData("text"));
    if (!parsed) return;
    e.preventDefault();
    const constrained = constrainBirthParts(parsed);
    emit(constrained);
    if (shouldAutoAdvanceBirthPart("year", constrained.year)) yearRef.current?.focus();
    else if (shouldAutoAdvanceBirthPart("month", constrained.month)) yearRef.current?.focus();
    else if (shouldAutoAdvanceBirthPart("day", constrained.day)) monthRef.current?.focus();
  }

  const segmentProps = {
    required,
    className: segmentClass,
    onBlur: () => setTouched(true),
  };

  const gridClass =
    layout === "fill"
      ? "grid h-full w-full grid-cols-3 items-center gap-2"
      : "grid grid-cols-[3.25rem_3.25rem_4.75rem] items-end gap-2 sm:grid-cols-[3.5rem_3.5rem_5.25rem] sm:gap-3";

  return (
    <div className="space-y-1.5">
      <div
        className={gridClass}
        role="group"
        aria-labelledby={`${groupId}-legend`}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${groupId}-error` : undefined}
        onPaste={onPaste}
      >

        <label className="block">
          <input
            ref={dayRef}
            id={`${groupId}-day`}
            type="text"
            inputMode="numeric"
            autoComplete="bday-day"
            placeholder="DD"
            maxLength={2}
            value={parts.day}
            onChange={(e) => updatePart("day", e.target.value)}
            onKeyDown={(e) => onPartKeyDown("day", e)}
            {...segmentProps}
          />
        </label>

        <label className="block">
          <input
            ref={monthRef}
            id={`${groupId}-month`}
            type="text"
            inputMode="numeric"
            autoComplete="bday-month"
            placeholder="MM"
            maxLength={2}
            value={parts.month}
            onChange={(e) => updatePart("month", e.target.value)}
            onKeyDown={(e) => onPartKeyDown("month", e)}
            {...segmentProps}
          />
        </label>

        <label className="block">
          <input
            ref={yearRef}
            id={`${groupId}-year`}
            type="text"
            inputMode="numeric"
            autoComplete="bday-year"
            placeholder="YYYY"
            maxLength={4}
            value={parts.year}
            onChange={(e) => updatePart("year", e.target.value)}
            onKeyDown={(e) => onPartKeyDown("year", e)}
            {...segmentProps}
          />
        </label>
      </div>

      {error ? (
        <p id={`${groupId}-error`} className="text-sm text-red-700" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
