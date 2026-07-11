interface AdminCounterCardProps {
  playerName: string;
  value: number;
  submitted: boolean;
  editable: boolean;
  onIncrement: () => void;
  onDecrement: () => void;
  onSubmit: () => void;
}

export function AdminCounterCard({
  playerName,
  value,
  submitted,
  editable,
  onIncrement,
  onDecrement,
  onSubmit,
}: AdminCounterCardProps) {
  return (
    <div className="bg-white border border-neutral-200 rounded-xl p-3 flex flex-col gap-3">
      <span className="font-medium text-sm truncate">{playerName}</span>

      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          aria-label={`Decrease ${playerName}'s value`}
          onClick={onDecrement}
          disabled={submitted || !editable}
          className="w-9 h-9 rounded-full flex items-center justify-center text-base border border-neutral-300 disabled:opacity-40 active:scale-95 transition"
        >
          −
        </button>
        <span className="text-xl font-medium min-w-[28px] text-center tabular-nums">
          {value}
        </span>
        <button
          type="button"
          aria-label={`Increase ${playerName}'s value`}
          onClick={onIncrement}
          disabled={submitted || !editable}
          className="w-9 h-9 rounded-full flex items-center justify-center text-base border border-neutral-300 disabled:opacity-40 active:scale-95 transition"
        >
          +
        </button>
      </div>

      <button
        type="button"
        onClick={onSubmit}
        className={`w-full h-9 rounded-lg text-sm font-medium transition active:scale-[0.98] ${
          submitted || !editable
            ? "bg-neutral-100 text-neutral-500"
            : "bg-neutral-900 text-white"
        }`}
      >
        {submitted || !editable ? "Locked" : "Submit"}
      </button>
    </div>
  );
}
