interface KrakenToggleProps {
  krakenPlayed: boolean;
  editable: boolean;
  onToggle: (value: boolean) => void;
}

// A Kraken destroys one trick, so the round's tricks must sum to one fewer than
// the cards dealt. This toggle records whether that happened.
export function KrakenToggle({ krakenPlayed, editable, onToggle }: KrakenToggleProps) {
  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={krakenPlayed}
      onClick={() => editable && onToggle(!krakenPlayed)}
      disabled={!editable}
      className={`w-full flex items-center justify-between gap-3 rounded-xl px-3 py-2 border text-sm transition disabled:opacity-100 ${
        krakenPlayed
          ? "bg-neutral-900 text-white border-neutral-900"
          : "bg-neutral-50 border-neutral-200"
      } ${editable ? "active:scale-[0.99]" : ""}`}
    >
      <span className="flex items-center gap-2.5 min-w-0">
        <span
          className={`w-5 h-5 rounded-md border flex items-center justify-center text-xs shrink-0 ${
            krakenPlayed ? "bg-white/20 border-white/40" : "border-neutral-300"
          }`}
        >
          {krakenPlayed ? "✓" : ""}
        </span>
        <span className="truncate">Kraken destroyed a trick</span>
      </span>
      <span className={`text-xs shrink-0 ${krakenPlayed ? "opacity-80" : "text-neutral-400"}`}>
        −1 trick
      </span>
    </button>
  );
}
