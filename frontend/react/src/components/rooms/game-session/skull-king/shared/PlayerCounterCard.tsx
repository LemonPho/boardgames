interface PlayerCounterCardProps {
  title: string;
  value: number;
  round: number;
  cardCount: number;
  // Status shown under the counter: null hides it, "saving" / "saved" reflect
  // the debounced auto-save.
  status?: "saving" | "saved" | null;
  // This player's team leads (goes first) this round.
  leads?: boolean;
  onIncrement?: () => void;
  onDecrement?: () => void;
}

export function PlayerCounterCard({
  title,
  value,
  status,
  leads,
  onIncrement,
  onDecrement,
}: PlayerCounterCardProps) {
  return (
    <div
      className={`bg-white dark:bg-neutral-900 rounded-xl p-6 border ${
        leads ? "border-amber-400 ring-1 ring-amber-300" : "border-neutral-200 dark:border-neutral-800"
      }`}
    >
      {leads && (
        <div className="text-center mb-2 text-sm font-medium text-amber-600 dark:text-amber-500">
          ▶ You go first this round
        </div>
      )}
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
