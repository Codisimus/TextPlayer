package com.codisimus.plugins.textplayer;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Listens for error logs
 *
 * @author Codisimus
 */
public class LogListener extends Handler {
    static final int ANTI_SPAM_TIMER = 60 * 1000;
    static long antiSpamClock;

    @Override
    public void publish(LogRecord record) {
        if (record.getLevel() != Level.SEVERE) {
            return;
        }

        long time = System.currentTimeMillis();
        if (time < antiSpamClock) {
            return;
        }

        String msg = record.getMessage();
        if (msg == null) {
            return;
        }

        for (User user: TextPlayer.getUsers()) {
            if (user.watchingErrors) {
                user.sendText(record.getLoggerName() + " generated an error", msg);
            }
        }

        antiSpamClock = time + ANTI_SPAM_TIMER;
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
}
