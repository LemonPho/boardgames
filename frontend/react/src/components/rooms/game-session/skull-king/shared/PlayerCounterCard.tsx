interface PlayerCounterCardProps {
  title: string;
  value: number;
  round: number;
  cardCount: number;
  // Status shown under the counter: null hides it, "saving" / "saved" reflect
  // the debounced auto-save.
  status?: "saving" | "saved" | null;
  onIncrement?: () => void;
  onDecrement?: () => void;
}

export function PlayerCounterCard({
  title,
  value,
  round,
  cardCount,
  status,
  onIncrement,
  onDecrement,
}: PlayerCounterCardProps) {
  return (
    <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-xl p-6">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {round} · {cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">{title}</h2>

      <div className="bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-xl py-8 px-4 flex items-center justify-center gap-6 mb-4">
        {onDecrement && (
          <button
            type="button"
            aria-label="Decrease"
            onClick={onDecrement}
            className="w-14 h-14 rounded-full flex items-center justify-center text-2xl border border-neutral-300 dark:border-neutral-700 active:scale-95 transition"
          >
            −
          </button>
        )}
        <div className="text-6xl font-medium min-w-[80px] text-center tabular-nums">
          {value}
        </div>
        {onIncrement && (
          <button
            type="button"
            aria-label="Increase"
            onClick={onIncrement}
            className="w-14 h-14 rounded-full flex items-center justify-center text-2xl border border-neutral-300 dark:border-neutral-700 active:scale-95 transition"
          >
            +
          </button>
        )}
      </div>

      <div className="h-5 text-center text-sm text-neutral-400">
        {status === "saving" && "Saving…"}
        {status === "saved" && "✓ Saved"}
      </div>
    </div>
  );
}
