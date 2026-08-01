package com.motomutterers.boardgames.auth.dto;

// The trusted claims lifted out of a verified Google ID token.
//   sub   : Google's immutable user id — the identity key
//   email : the address Google holds for the account
//   emailVerified : whether Google itself has verified that address. Only a
//                   verified address may be matched against an existing local
//                   account, otherwise linking could be forged.
public record GoogleIdentity(
    String sub,
    String email,
    boolean emailVerified
) {}
