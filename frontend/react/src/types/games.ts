export interface SimpleGameResponse {
    name: string,
    type: string,
    minPlayers: number,
    maxPlayers: number,
    description: string
}

export interface GameResponse{
    name: string,
    type: string,
    gameConfig: string,
    scoringConfig: string,
    minPlayers: number,
    maxPlayers: number,
    description: string
}