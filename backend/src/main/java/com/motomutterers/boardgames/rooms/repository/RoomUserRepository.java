package com.motomutterers.boardgames.rooms.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.rooms.model.RoomUser;

public interface RoomUserRepository extends JpaRepository<RoomUser, UUID> {}
