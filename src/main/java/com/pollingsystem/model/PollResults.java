package com.pollingsystem.model;

import java.util.List;

public record PollResults(String pollId, String question, String status, long totalVotes,
                          List<OptionResult> options) { }
