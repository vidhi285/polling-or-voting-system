package com.pollingsystem.controller;

import com.pollingsystem.dto.CastVoteRequest;
import com.pollingsystem.dto.VoteView;
import com.pollingsystem.service.ResultsService;
import com.pollingsystem.service.VoteService;
import com.pollingsystem.util.Json;

public class VoteController {
    private final VoteService voteService;
    private final ResultsService resultsService;
    private final Json json;

    public VoteController(VoteService voteService, ResultsService resultsService, Json json) {
        this.voteService = voteService;
        this.resultsService = resultsService;
        this.json = json;
    }

    public ApiResponse castVote(String pollId, byte[] body) {
        CastVoteRequest request = json.read(body, CastVoteRequest.class);
        return ApiResponse.ok(VoteView.from(voteService.castVote(pollId, request.voterId(), request.optionId())));
    }

    public ApiResponse results(String pollId) {
        return ApiResponse.ok(resultsService.getResults(pollId));
    }
}
