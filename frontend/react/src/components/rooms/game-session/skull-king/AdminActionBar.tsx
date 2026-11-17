import { useState } from "react";
import { useRoundData } from "../../../../context/RoundDataContext";
import SubmitButton from "../../../util/SubmitButton";

/**
 * The admin's phase-advance button, pinned to the bottom of the viewport so it's
 * reachable without scrolling past a tall card grid (6–8 players). Renders nothing
 * when there's no action (non-admins, or phases without one).
 *
 * It must sit inside a RoundData provider. The live provider is keyed by round, so
 * the loading state here resets with each round.
 */
export default function AdminActionBar() {
  const { advance, advanceLabel, advanceDisabled } = useRoundData();
  const [loading, setLoading] = useState(false);

  if (advance === null) return null;

  // -mx/px cancel out <main>'s horizontal padding (px-4, sm:px-6) so the bar's
  // background spans the full column; the inner max-w-md lines the button up with
  // the phase content above it. sticky (not fixed) so the bar settles at the end of
  // the session column instead of covering the page footer. The background matches
  // the page (LayoutPage's bg-gray-50) so the bar doesn't read as a separate panel;
  // it's only there to stop cards showing through while they scroll underneath.
  return (
    <div className="sticky bottom-0 z-10 -mx-4 sm:-mx-6 px-4 sm:px-6 pt-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] bg-gray-50">
      <div className="max-w-md mx-auto">
        <SubmitButton
          text={advanceLabel ?? "Continue"}
          loading={loading}
          setLoading={setLoading}
          onSubmit={advance}
          disabled={advanceDisabled}
          className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
        />
      </div>
    </div>
  );
}
