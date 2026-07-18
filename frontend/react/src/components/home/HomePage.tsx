import { useEffect, useState } from "react"
import type { SimpleGameResponse } from "../../types/games"
import { getGames } from "../../api/games"
import { useAlertsContext } from "../../context/AlertsContext"
import { Link } from "react-router-dom"
import RoomsSection from "./RoomsSection"

import skullKingImage from '../../assets/skullking/skull-king-1-jeux-Toulon-L-Ataniere.webp'

export default function HomePage() {
  const { setErrorMessage } = useAlertsContext()
  const [loading, setLoading] = useState(true)
  const [games, setGames] = useState<SimpleGameResponse[]>([])

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      const response = await getGames(setErrorMessage)
      setGames(response)
      setLoading(false)
    }
    fetchData()
  }, [])

  if (loading) {
    return (
      <div>
        Loading...
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <RoomsSection />

      <h1 className="text-2xl font-medium text-gray-900 mb-1">Games</h1>
      <p className="text-sm text-gray-500 mb-8">Pick a game to start tracking scores</p>

      <div className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-3">
        {games.map((game) => (
          <Link
            key={game.name}
            to={`/games/${game.name}`}
            className="relative rounded-2xl shadow-lg overflow-hidden aspect-[3/4] flex flex-col justify-end hover:shadow-xl transition-shadow"
          >
            <img
              src={skullKingImage}
              alt={game.name}
              className="absolute inset-0 w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-black/20 backdrop-blur-[2px]" />
            <div className="relative z-10 p-5">
              <p className="text-xl font-semibold text-white">{game.name}</p>
            </div>
          </Link>
        ))}
      </div>

    </div>
  )
}