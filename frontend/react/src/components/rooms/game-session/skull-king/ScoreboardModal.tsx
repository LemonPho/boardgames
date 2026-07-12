import { useState } from "react";
import Modal from "../../../util/Modal";
import { useSkullKingSessionContext } from "../../../../context/SkullKingSessionContext";
import { useRoomContext } from "../../../../context/RoomContext";
import RoundHistoryView from "./RoundHistoryView";

const TOTAL_ROUNDS = 10;

type Tab = "scores" | "rounds";

export default function ScoreboardModal({ id }: { id: string }) {
  const { state } = useSkullKingSessionContext();
  const { room } = useRoomContext();
  const [tab, setTab] = useState<Tab>("scores");
  const [selectedRound, setSelectedRound] = useState<number | null>(null);

  const ranked = [...(state?.teams ?? [])].sort((a, b) => b.finalScore - a.finalScore);
  const currentRound = state?.round ?? 1;

  const switchTab = (next: Tab): void => {
    setTab(next);
    setSelectedRound(null);
  };

  return (
    <Modal id={id} title="Scoreboard">
      {/* Tabs */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => switchTab("scores")}
          className={`text-sm px-3 py-1.5 rounded-lg transition-colors ${
            tab === "scores"
              ? "bg-gray-800 text-white"
              : "text-gray-500 border border-gray-200 hover:border-gray-400"
          }`}
        >
          Scores
        </button>
        <button
          onClick={() => switchTab("rounds")}
          className={`text-sm px-3 py-1.5 rounded-lg transition-colors ${
            tab === "rounds"
              ? "bg-gray-800 text-white"
              : "text-gray-500 border border-gray-200 hover:border-gray-400"
          }`}
        >
          Rounds
        </button>
      </div>

      {tab === "scores" && (
        ranked.length === 0 ? (
          <p className="text-sm text-gray-500">No teams yet.</p>
        ) : (
          <div className="flex flex-col gap-1">
            {ranked.map((team, index) => (
              <div
                key={team.id}
                className="flex items-center justify-between px-3 py-2 rounded-xl border border-gray-100"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className="w-6 text-sm font-semibold text-gray-400 tabular-nums text-center">
                    {index + 1}
                  </span>
                  <span className="text-sm text-gray-800 truncate">
                    {team.player.displayName}
                  </span>
                </div>
                <span className="text-sm font-semibold tabular-nums">
                  {team.finalScore}
                </span>
              </div>
            ))}
          </div>
        )
      )}

      {tab === "rounds" && (
        selectedRound !== null && room ? (
          <RoundHistoryView
            roomName={room.name}
            round={selectedRound}
            onBack={() => setSelectedRound(null)}
          />
        ) : (
          <div className="grid grid-cols-5 gap-2">
            {Array.from({ length: TOTAL_ROUNDS }, (_, i) => i + 1).map((round) => {
              const isCurrent = round === currentRound;
              const isCompleted = round < currentRound;

              return (
                <button
                  key={round}
                  type="button"
                  disabled={!isCompleted}
                  onClick={() => setSelectedRound(round)}
                  className={`aspect-square rounded-xl text-sm font-semibold tabular-nums flex items-center justify-center border transition ${
                    isCurrent
                      ? "bg-gray-800 text-white border-gray-800"
                      : isCompleted
                        ? "border-gray-200 text-gray-700 hover:border-gray-400"
                        : "border-gray-100 text-gray-300"
                  }`}
                >
                  {round}
                </button>
              );
            })}
          </div>
        )
      )}
    </Modal>
  );
}
