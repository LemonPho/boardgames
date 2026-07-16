import { useEffect, useRef, useState } from "react";
import type { TeamResponse } from "../../../../../types/teams";

interface AdminCounterCardProps {
  playerName: string;
  editable: boolean;
  team: TeamResponse;
  // The team's value from the server: seeds an editable card and is shown live
  // on read-only cards (so other teams' updates appear).
  serverValue: number;
  max: number;
  status?: "saving" | "saved" | null;
  // This team leads (goes first) this round.
  leads?: boolean;
  // Reports a new value up to the parent, which stores it and debounces the save.
  change: (teamId: string, value: number) => void;
}

export function AdminCounterCard({
  playerName,
  editable,
  team,
  serverValue,
  max,
  status,
  leads,
  change,
}: AdminCounterCardProps) {
  const [value, setValue] = useState<number>(serverValue);

  // Adopt a genuinely new server value (e.g. a correction from another device),
  // but don't clobber a local edit the admin hasn't reconciled with the server yet.
  const lastServer = useRef<number>(serverValue);
  useEffect(() => {
    if (serverValue !== lastServer.current) {
      lastServer.current = serverValue;
      if (serverValue !== value) setValue(serverValue);
    }
  }, [serverValue, value]);

  const update = (next: number): void => {
    if (next < 0 || next > max) return;
    setValue(next);
    change(team.id, next);
  };

  // Editable cards own their value; read-only cards mirror the live server value.
  const displayValue = editable ? value : serverValue;

  return (
    <div
      className={`bg-white rounded-xl p-3 flex flex-col gap-2 border ${
        leads ? "border-amber-400 ring-1 ring-amber-300" : "border-neutral-200"
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="font-medium text-sm truncate flex items-center gap-1">
          {leads && <span className="text-amber-500" aria-label="Goes first">▶</span>}
          {playerName}
        </span>
        <span className="text-[11px] text-neutral-400 shrink-0 h-4">
          {editable && status === "saving" && "Saving…"}
          {editable && status === "saved" && "✓"}
        </span>
      </div>

      <div className="flex items-center justify-center gap-2">
        {editable && (
          <button
            type="button"
            aria-label={`Decrease ${playerName}'s value`}
            onClick={() => update(value - 1)}
            className="w-9 h-9 rounded-full flex items-center justify-center text-base border border-neutral-300 active:scale-95 transition"
          >
            −
          </button>
        )}
        <span className="text-xl font-medium min-w-[28px] text-center tabular-nums">
          {displayValue}
        </span>
        {editable && (
          <button
            type="button"
            aria-label={`Increase ${playerName}'s value`}
            onClick={() => update(value + 1)}
            className="w-9 h-9 rounded-full flex items-center justify-center text-base border border-neutral-300 active:scale-95 transition"
          >
            +
          </button>
        )}
      </div>
    </div>
  );
}
