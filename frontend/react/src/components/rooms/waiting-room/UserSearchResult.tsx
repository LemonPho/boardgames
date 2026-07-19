import { useState } from "react";
import type { UserAvailabilityResponse } from "../../../types/user";
import SubmitButton from "../../util/SubmitButton";

export default function UserSearchResult({ user, handleInviteUserToRoom }: { user: UserAvailabilityResponse, handleInviteUserToRoom: (username: string) => Promise<void> }) {
  const [loading, setLoading] = useState(false);

  if (user.inGame) {
    return (
      <div
        key={user.username}
        className="flex items-center justify-between px-3 py-2 rounded-xl border border-gray-100 opacity-60"
      >
        <div className="flex items-center gap-3">
          <div className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-600">
            {user.username[0].toUpperCase()}
          </div>
          <span className="text-sm text-gray-800">{user.username}</span>
        </div>
        <span className="text-xs text-gray-400 bg-gray-100 px-2 py-1 rounded-full">
          In game
        </span>
      </div>
    )
  }
  
  if (user.invited) {
    return (
      <div
        key={user.username}
        className="flex items-center justify-between px-3 py-2 rounded-xl border border-green-200 opacity-100"
      >
        <div className="flex items-center gap-3">
          <div className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-600">
            {user.username[0].toUpperCase()}
          </div>
          <span className="text-sm text-gray-800">{user.username}</span>
        </div>
        <span className="text-xs text-gray-400 bg-green-100 px-2 py-1 rounded-full">
          Invited
        </span>
      </div>
    )
  } 

  return(
    <div
      key={user.username}
      className="flex items-center justify-between px-3 py-2 rounded-xl border border-gray-100 hover:border-gray-300 transition-colors"
    >
      <div className="flex items-center gap-3">
        <div className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-600">
          {user.username[0].toUpperCase()}
        </div>
        <span className="text-sm text-gray-800">{user.username}</span>
      </div>
      <SubmitButton
        text="Invite"
        loading={loading}
        setLoading={setLoading}
        onSubmit={() => handleInviteUserToRoom(user.username)}
        className="text-xs text-gray-500 border border-gray-200 rounded-lg px-2 py-1 hover:border-gray-400 transition-colors disabled:opacity-40"
      />
    </div>
  )
}