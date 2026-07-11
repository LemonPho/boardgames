interface PlayerCounterCardProps {
  title: string;
  value: number;
  round: number;
  cardCount: number;
  submitted: boolean;
  submittedLabel: string;
  submitLabel: string;
  onIncrement?: () => void;
  onDecrement?: () => void;
  onSubmit?: () => void;
}

export function PlayerCounterCard({
  title,
  value,
  round,
  cardCount,
  submitted,
  submittedLabel,
  submitLabel,
  onIncrement,
  onDecrement,
  onSubmit,
}: PlayerCounterCardProps) {
  return (
    <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-xl p-6">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {round} · {cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">{title}</h2>

      <div className="bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-xl py-8 px-4 flex items-center justify-center gap-6 mb-6">
        <button
          type="button"
          aria-label="Decrease"
          onClick={onDecrement}
          disabled={submitted || !onDecrement}
          className="w-14 h-14 rounded-full flex items-center justify-center text-2xl border border-neutral-300 dark:border-neutral-700 disabled:opacity-40 active:scale-95 transition"
        >
          −
        </button>
        <div className="text-6xl font-medium min-w-[80px] text-center tabular-nums">
          {value}
        </div>
        <button
          type="button"
          aria-label="Increase"
          onClick={onIncrement}
          disabled={submitted || !onIncrement}
          className="w-14 h-14 rounded-full flex items-center justify-center text-2xl border border-neutral-300 dark:border-neutral-700 disabled:opacity-40 active:scale-95 transition"
        >
          +
        </button>
      </div>

      <button
        type="button"
        onClick={onSubmit}
        disabled={submitted || !onSubmit}
        className="w-full h-12 rounded-lg text-base font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-50 active:scale-[0.98] transition"
      >
        {submitted ? submittedLabel : submitLabel}
      </button>
    </div>
  );
}
