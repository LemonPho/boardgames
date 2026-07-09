package com.motomutterers.boardgames.rooms.model.Room;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/*
  RoomUser {
    uuid id PK
    uuid room_id FK
    uuid user_id FK nullable
    string display_name
    boolean is_anonymous
    string role
    timestamp joined_at
  }
*/

@Entity
@Table(name = "rooms_users")
public class RoomUser {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", columnDefinition = "room_user_roles")
    private RoomUserRoles role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public RoomUser(){}

    public RoomUser(User user, Room room, RoomUserRoles role){
      this.user = user;
      this.room = room;
      this.role = role;
      this.displayName = user.getUsername();
    }

    public RoomUser(String displayName, Room room){
      this.displayName = displayName;
      this.room = room;
      this.role = RoomUserRoles.ANONYMOUS;
    }

    public UUID getId(){
      return id;
    }

    public Room getRoom(){
      return room;
    }

    public User getUser(){
      return user;
    }

    public String getDisplayName(){
      return displayName;
    }

    public RoomUserRoles getRole(){
      return role;
    }

    public LocalDateTime getJoinedAt(){
      return joinedAt;
    }

    public void setRoom(Room room){
        this.room = room;
    }

    public void setUser(User user){
        this.user = user;
    }

    public void setDisplayName(String displayName){
        this.displayName = displayName;
    }

    public void setRole(RoomUserRoles role) {
        this.role = role;
    }

    public Team getTeam(){
        return team;
    }

    public void setTeam(Team team){
        this.team = team;
    }

    public void setJoinedAt(LocalDateTime joinedAt){
        this.joinedAt = joinedAt;
    }
}
