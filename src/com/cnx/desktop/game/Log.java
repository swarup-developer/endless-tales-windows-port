package com.cnx.desktop.game;

import com.cnx.desktop.DesktopRuntime;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Writes game and engine messages to game.log in the save folder (and to stderr). */
public final class Log {
    private static Path file;
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Log() {}

    static void init(Path logFile) {
        file = logFile;
        try { Files.writeString(file, "", StandardCharsets.UTF_8); } catch (IOException ignored) {}
        DesktopRuntime.host = new DesktopRuntime.Host() {
            @Override public void log(String level, String tag, String message) {
                if ("V".equals(level) || "D".equals(level)) return;
                // the game's data loader asks for the player before one exists; harmless
                if (message != null && message.contains("No Character instantiated on Shell")) return;
                write(level + "/" + tag + ": " + message);
            }
        };
    }

    public static void info(String msg) { write("I: " + msg); }
    public static void warn(String msg) { write("W: " + msg); }

    public static void error(String msg, Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) t.printStackTrace(new PrintWriter(sw));
        write("E: " + msg + (t != null ? "\n" + sw : ""));
    }

    private static synchronized void write(String line) {
        String l = LocalTime.now().format(T) + " " + line;
        System.err.println(l);
        if (file != null) {
            try { Files.writeString(file, l + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND); }
            catch (IOException ignored) {}
        }
    }
}
