import type { GameResponse, SimpleGameResponse } from "../types/games";
import { setAxiosError } from "../util/api";
import { api } from "./axiosSetup";

export const getGames = async (setErrorMessage: (message: string) => void): Promise<Array<SimpleGameResponse> | []> => {
    try{
        const response = await api.get("/games/all");
        const games = new Array<SimpleGameResponse>;
        if(response.data){
            for(let i=0; i < response.data.length; i++){
                let game: SimpleGameResponse = {
                    description: response.data[i].description,
                    maxPlayers: response.data[i].maxPlayers,
                    minPlayers: response.data[i].minPlayers,
                    name: response.data[i].name,
                    type: response.data[i].type
                };
                games.push(game);
            }
        }

        return games ;
    } catch(error){
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const getGame = async (name: string | undefined, setErrorMessage: (message: string) => void): Promise<GameResponse | null> => {
    if(name == undefined) return null;
    
    try{
        const response = await api.get(`/games/${name}`);
        if(response.data) {
            const game: GameResponse = {
                name: response.data.name,
                type: response.data.type,
                gameConfig: response.data.gameConfig,
                scoringConfig: response.data.scoringConfig,
                minPlayers: response.data.minPlayers,
                maxPlayers: response.data.maxPlayers,
                description: response.data.description
            }

            return game;
        }

        return null;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}