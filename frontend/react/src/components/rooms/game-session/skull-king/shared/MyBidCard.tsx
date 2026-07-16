interface MyBidCardProps {
  round: number;
  cardCount: number;
  bid: number | null;
  leads?: boolean;
}

export function MyBidCard({ round, cardCount, bid, leads }: MyBidCardProps) {
  return (
    <div
      className={`bg-white dark:bg-neutral-900 rounded-xl p-6 border ${
        leads ? "border-amber-400 ring-1 ring-amber-300" : "border-neutral-200 dark:border-neutral-800"
      }`}
    >
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {round} · {cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Round in progress</h2>
      {leads && (
        <div className="text-center -mt-4 mb-6 text-sm font-medium text-amber-600 dark:text-amber-500">
          ▶ You go first this round
        </div>
      )}

      <div className="bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-xl py-8 px-4 text-center">
        <div className="text-sm text-neutral-500 dark:text-neutral-400 mb-2">
          Your bid
        </div>
        <div className="text-6xl font-medium tabular-nums">
          {bid ?? "–"}
        </div>
      </div>
    </div>
  );
}
