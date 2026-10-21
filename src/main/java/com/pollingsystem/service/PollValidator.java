package com.pollingsystem.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure validation rules for new polls. Returns every problem found, not just the first. */
public final class PollValidator {
    public static final int MAX_QUESTION_LENGTH = 200;
    public static final int MAX_OPTION_LENGTH = 100;
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 10;

    private PollValidator() { }

    public static List<String> validate(String question, List<String> options) {
        List<String> errors = new ArrayList<>();

        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            errors.add("Question is required.");
        } else if (q.length() > MAX_QUESTION_LENGTH) {
            errors.add("Question must be at most " + MAX_QUESTION_LENGTH + " characters.");
        }

        if (options == null || options.size() < MIN_OPTIONS) {
            errors.add("At least two options are required.");
            return errors;
        }
        if (options.size() > MAX_OPTIONS) {
            errors.add("A poll can have at most " + MAX_OPTIONS + " options.");
        }

        boolean anyEmpty = false;
        boolean anyTooLong = false;
        Set<String> seen = new HashSet<>();
        boolean duplicate = false;
        for (String option : options) {
            String o = option == null ? "" : option.trim();
            if (o.isEmpty()) {
                anyEmpty = true;
                continue;
            }
            if (o.length() > MAX_OPTION_LENGTH) anyTooLong = true;
            if (!seen.add(o.toLowerCase(Locale.ROOT))) duplicate = true;
        }
        if (anyEmpty) errors.add("Options must not be empty.");
        if (anyTooLong) errors.add("Each option must be at most " + MAX_OPTION_LENGTH + " characters.");
        if (duplicate) errors.add("Options must be unique.");
        return errors;
    }
}
