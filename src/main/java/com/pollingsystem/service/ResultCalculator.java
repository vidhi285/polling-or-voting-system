package com.pollingsystem.service;

import com.pollingsystem.model.OptionResult;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollOption;
import com.pollingsystem.model.PollResults;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure calculation: vote counts in, results out. Percentages use one decimal place, HALF_UP. */
public final class ResultCalculator {
    private ResultCalculator() { }

    public static PollResults calculate(Poll poll, Map<String, Long> countsByOption) {
        long total = 0;
        for (PollOption o : poll.options()) {
            total += countsByOption.getOrDefault(o.id(), 0L);
        }
        List<OptionResult> results = new ArrayList<>();
        for (PollOption o : poll.options()) {
            long votes = countsByOption.getOrDefault(o.id(), 0L);
            results.add(new OptionResult(o.id(), o.text(), votes, percentage(votes, total)));
        }
        return new PollResults(poll.id(), poll.question(), poll.status().name(), total, results);
    }

    static double percentage(long votes, long total) {
        if (total == 0) return 0.0; // no division by zero
        return BigDecimal.valueOf(votes * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
