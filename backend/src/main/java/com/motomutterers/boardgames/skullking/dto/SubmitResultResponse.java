package com.motomutterers.boardgames.skullking.dto;

// Reply to a live submit: the row's version after the write. The client sends it back as
// expectedVersion on its next save for that team, so its own fast taps never conflict.
public record SubmitResultResponse(int version) {}
