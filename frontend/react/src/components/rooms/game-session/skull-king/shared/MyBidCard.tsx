interface MyBidCardProps {
  round: number;
  cardCount: number;
  bid: number | null;
}

export function MyBidCard({ round, cardCount, bid }: MyBidCardProps) {
  return (
    <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-xl p-6">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {round} · {cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Round in progress</h2>

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
