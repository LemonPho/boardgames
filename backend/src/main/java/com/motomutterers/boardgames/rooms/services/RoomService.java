package com.motomutterers.boardgames.rooms.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.model.Room;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final GameService gameService;

    public RoomService(
        RoomRepository roomRepository,
        GameService gameService
    ){
        this.roomRepository = roomRepository;
        this.gameService = gameService;
    }

    public Room getRoomById(UUID id){
        return roomRepository.findById(id)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
    }

    public Room getRoomByName(String name){
        return roomRepository.findByName(name)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
    }

    public void createRoom(String gameName){

    }
}
