import Modal from "../../../util/Modal";
import { useSkullKingSessionContext } from "../../../../context/SkullKingSessionContext";

export default function ScoreboardModal({ id }: { id: string }) {
  const { state } = useSkullKingSessionContext();

  const ranked = [...(state?.teams ?? [])].sort((a, b) => b.finalScore - a.finalScore);

  return (
    <Modal id={id} title="Scoreboard">
      {ranked.length === 0 ? (
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
      )}
    </Modal>
  );
}
