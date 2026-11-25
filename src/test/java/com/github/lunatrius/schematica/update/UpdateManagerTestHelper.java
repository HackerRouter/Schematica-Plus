package com.github.lunatrius.schematica.update;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdateManagerTestHelper {
    private UpdateManagerTestHelper() {}

    public static UpdateManager create(UpdateManager.Transport transport, File currentJar, File updatesDir,
        AtomicReference<String> ignored, AtomicBoolean shutdownCalled) {
        return new UpdateManager(
            transport,
            () -> currentJar,
            (dir, oldJar, stagedJar, targetJar) -> {},
            () -> "1.0.0-beta.5",
            () -> "auto",
            ignored::get,
            ignored::set,
            () -> updatesDir,
            () -> shutdownCalled.set(true),
            false);
    }
}
