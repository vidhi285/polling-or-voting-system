package com.pollingsystem.util;

import java.util.UUID;

public final class Ids {
    private Ids() { }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String newOptionId() {
        return "opt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
