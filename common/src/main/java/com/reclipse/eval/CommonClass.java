package com.reclipse.eval;

import com.reclipse.eval.platform.Services;

public class CommonClass {
    public static void init() {
        Constants.LOG.info("Eval mod initialized on {}! Environment: {}", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
    }
}
