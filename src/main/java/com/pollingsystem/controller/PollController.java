package com.pollingsystem.controller;

import com.pollingsystem.dto.CreatePollRequest;
import com.pollingsystem.dto.PollView;
import com.pollingsystem.model.Poll;
import com.pollingsystem.service.PollService;
import com.pollingsystem.service.ResultsService;
import com.pollingsystem.util.Json;
import java.util.List;

/** Translates poll requests into service calls. No HTTP-server types and no business rules here. */
public class PollController {
    private final PollService pollService;
    private final ResultsService resultsService;
    private final Json json;

    public PollController(PollService pollService, ResultsService resultsService, Json json) {
        this.pollService = pollService;
        this.resultsService = resultsService;
        this.json = json;
    }

    public ApiResponse list() {
        List<PollView> views = pollService.listPolls().stream().map(PollView::from).toList();
        return ApiResponse.ok(views);
    }

    public ApiResponse create(byte[] body) {
        CreatePollRequest request = json.read(body, CreatePollRequest.class);
        Poll poll = pollService.createPoll(request.question(), request.options());
        return ApiResponse.created(PollView.from(poll));
    }

    public ApiResponse get(String id) {
        return ApiResponse.ok(PollView.from(pollService.getPoll(id)));
    }

    public ApiResponse close(String id) {
        return ApiResponse.ok(PollView.from(pollService.closePoll(id)));
    }

    public ApiResponse stats() {
        return ApiResponse.ok(resultsService.getStats());
    }
}
