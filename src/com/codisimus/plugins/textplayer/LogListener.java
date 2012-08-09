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
    @Override
    public void publish(LogRecord record) {
        if (record.getLevel() != Level.SEVERE)
            return;

        String msg = record.getMessage();
        if (msg == null)
            return;

        msg = record.getLoggerName()+" generated an error: "+msg;
        for (User user: TextPlayer.getUsers())
            if (user.watchingErrors)
                user.sendText(msg);
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
}