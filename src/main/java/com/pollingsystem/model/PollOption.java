package com.pollingsystem.model;

/** One answer choice. The id is stable, so results never depend on array positions. */
public record PollOption(String id, String text) { }
