package com.pollingsystem.model;

import java.time.Instant;

public record Vote(String id, String pollId, String voterId, String optionId, Instant castAt) { }
