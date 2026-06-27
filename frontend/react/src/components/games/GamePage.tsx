import { useEffect, useState } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { getGame } from "../../api/games"
import { useAlertsContext } from "../../context/AlertsContext"
import type { GameResponse } from "../../types/games"

import skullKingImage from '../../assets/skull-king-1-jeux-Toulon-L-Ataniere.webp'
import { createRoom } from "../../api/rooms"
import type { CreateRoomRequest, TrackingMode } from "../../types/rooms"

const TRACKING_OPTIONS: { value: TrackingMode; label: string; description: string }[] = [
  { value: "SELF", label: "Self", description: "Each player marks their own points during the game." },
  { value: "ADMIN", label: "Admin", description: "Only the room creator can mark points for all players." },
]

export default function GamePage() {
  const { setErrorMessage } = useAlertsContext()
  const { name } = useParams()

  const [game, setGame] = useState<GameResponse | null>(null)
  const [trackingMode, setTrackingMode] = useState<TrackingMode>("SELF")

  const navigate = useNavigate();

  const handleCreateRoom = async (): Promise<void> => {
    if(game == null) return;

    const request: CreateRoomRequest = {
      trackingMode: trackingMode,
      gameName: game?.name,
    }
    const response = await createRoom(request, setErrorMessage);
    navigate(`/rooms/${response?.name}`);
  }

  useEffect(() => {
    const fetchData = async () => {
      const response = await getGame(name, setErrorMessage)
      setGame(response)
    }
    fetchData()
  }, [])

  if (!game) return null

  return (
    <div className="min-h-screen bg-gray-100 flex justify-center p-6">
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-lg overflow-hidden">

        {/* Hero */}
        <div className="relative h-48">
          <img
            src={skullKingImage}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
          <div className="absolute bottom-0 left-0 p-5">
            <h1 className="text-2xl font-semibold text-white">{game.name}</h1>
            <span className="inline-block mt-1.5 text-xs text-white border border-white/40 bg-white/20 rounded-full px-2.5 py-0.5">
              {game.type}
            </span>
          </div>
        </div>

        <div className="p-6 flex flex-col gap-6">

          {/* Description */}
          <p className="text-sm text-gray-500 leading-relaxed">{game.description}</p>

          {/* Player count */}
          <div className="flex gap-3">
            {[
              { label: "Min players", value: game.minPlayers },
              { label: "Max players", value: game.maxPlayers },
            ].map(({ label, value }) => (
              <div key={label} className="flex-1 bg-gray-50 rounded-xl p-3 text-center">
                <p className="text-xs text-gray-400 mb-1">{label}</p>
                <p className="text-base font-semibold text-gray-800">{value}</p>
              </div>
            ))}
          </div>

          <hr className="border-gray-100" />

          {/* Tracking mode */}
          <div>
            <p className="text-sm font-semibold text-gray-700 mb-3">Tracking mode</p>
            <div className="flex flex-col gap-2">
              {TRACKING_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  onClick={() => setTrackingMode(option.value)}
                  className={`flex items-start gap-3 text-left border rounded-xl p-4 transition-colors ${trackingMode === option.value
                      ? "border-gray-800 bg-gray-50"
                      : "border-gray-200 hover:border-gray-300"
                    }`}
                >
                  <div className={`mt-0.5 w-4 h-4 rounded-full border-2 flex items-center justify-center flex-shrink-0 ${trackingMode === option.value ? "border-gray-800" : "border-gray-300"
                    }`}>
                    {trackingMode === option.value && (
                      <div className="w-2 h-2 rounded-full bg-gray-800" />
                    )}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-800">{option.label}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{option.description}</p>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* CTA */}
          <button
            onClick={handleCreateRoom}
            className="w-full bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-3 rounded-xl transition-colors"
          >
            Create room
          </button>
        </div>
      </div>
    </div>
  )
}