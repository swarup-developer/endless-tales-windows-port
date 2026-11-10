package com.cnx.desktop.game;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

/**
 * Speech output for blind players, the way audio games do it:
 *  1. the running screen reader, through NVDA's official controller client (also sends braille);
 *  2. otherwise the Windows voice (SAPI, through a background PowerShell using System.Speech);
 *  3. or nothing ("Off").
 * Text is spoken directly, so menus never announce role words like "button".
 */
public final class Speech {
    public enum Mode { AUTO, SCREEN_READER, WINDOWS_VOICE, OFF }

    public static volatile Mode mode = Mode.AUTO;
    /** Windows voice speed, -10..10. */
    public static volatile int voiceRate = 2;

    private static Nvda nvda;
    private static Sapi sapi;
    private static Path logFile;
    private static long nvdaCheckedAt;
    private static boolean nvdaRunning;

    private Speech() {}

    /** Loads the NVDA client from {@code dllDir}; optional speech log for testing. */
    static void init(Path dllDir, Path speechLog) {
        logFile = speechLog;
        if (logFile != null) { try { Files.writeString(logFile, ""); } catch (Exception ignored) {} }
        Path dll = dllDir == null ? null : dllDir.resolve("nvdaControllerClient.dll");
        if (dll != null && Files.isRegularFile(dll)) {
            try { nvda = new Nvda(dll); Log.info("NVDA controller client loaded"); }
            catch (Throwable t) { Log.warn("NVDA controller client not usable: " + t); }
        } else {
            Log.warn("nvdaControllerClient.dll not found in " + dllDir);
        }
    }

    /** True when speech goes anywhere (screen reader or Windows voice). */
    public static boolean enabled() { return mode != Mode.OFF; }

    /** Speaks text. {@code interrupt} stops what is being said first (use for navigation). */
    /** Counts everything said (lets a closing pop-up know whether something new was announced). */
    static volatile long spoken;
    private static volatile boolean hold;
    /** While true nothing is spoken (a screen is being rebuilt behind a pop-up). */
    public static void hold(boolean h) { hold = h; }
    private static final boolean SILENT = Boolean.getBoolean("et2.silent");

    public static void say(String text, boolean interrupt) {
        if (text == null) return;
        spoken++;
        String t = clean(text);
        if (t.isEmpty()) return;
        if (hold) { log("(held) " + t); return; }
        log((interrupt ? "! " : "+ ") + t);
        if (SILENT) return; // self-tests: speech log only, whatever the setting
        if (mode == Mode.OFF) return;
        try {
            if (useNvda()) {
                if (interrupt) nvda.cancel();
                nvda.speak(t);
                nvda.braille(t);
                return;
            }
            if (mode == Mode.SCREEN_READER) return; // asked for screen reader only, none running
            sapi().speak(t, interrupt);
        } catch (Throwable e) {
            Log.warn("Speech failed: " + e);
        }
    }

    public static void say(String text) { say(text, true); }
    public static void queue(String text) { say(text, false); }

    public static void stop() {
        log("! <stop>");
        try {
            if (useNvda()) nvda.cancel();
            else if (sapi != null) sapi.cancel();
        } catch (Throwable ignored) {}
    }

    /** Name of the output in use, for the options screen. */
    public static String outputName() {
        if (mode == Mode.OFF) return "off";
        if (useNvda()) return "NVDA";
        return mode == Mode.SCREEN_READER ? Tr.t("no screen reader running") : Tr.t("Windows voice");
    }

    static void shutdown() { if (sapi != null) sapi.close(); }

    private static boolean useNvda() {
        if (nvda == null || mode == Mode.WINDOWS_VOICE || mode == Mode.OFF) return false;
        long now = System.currentTimeMillis();
        if (now - nvdaCheckedAt > 3000) {
            nvdaCheckedAt = now;
            nvdaRunning = nvda.isRunning();
        }
        return nvdaRunning;
    }

    private static synchronized Sapi sapi() {
        if (sapi == null || !sapi.alive()) sapi = new Sapi();
        sapi.rate(voiceRate);
        return sapi;
    }

    /** One line, no markup the game uses for colors / placeholders. */
    static String clean(String s) {
        return s.replace('\n', ' ').replace('\r', ' ').replace("*", "")
                .replaceAll("\\s*[\u00B7\u2014]\\s*", ", ")          // "Lisport · Farm" -> "Lisport, Farm"
                .replace("\u2192", Tr.t(" to ")).replace("\u25C0", "").replace("\u25B6", "")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("\\s+([,.])", "$1")                      // "Tester , Warrior" -> "Tester, Warrior"
                .replaceAll("\\.{2,}", ".")                          // "sir.." -> "sir."
                .replaceAll("([.!?]),", "$1")                        // "No quests yet., 3 of 9" -> "No quests yet. 3 of 9"
                .trim();
    }

    private static synchronized void log(String line) {
        if (logFile == null) return;
        try { Files.writeString(logFile, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND); }
        catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- NVDA controller client (Java FFM)

    private static final class Nvda {
        private final MethodHandle testIfRunning, speakText, cancelSpeech, brailleMessage;

        Nvda(Path dll) {
            SymbolLookup lib = SymbolLookup.libraryLookup(dll, Arena.global());
            Linker linker = Linker.nativeLinker();
            FunctionDescriptor noArgs = FunctionDescriptor.of(ValueLayout.JAVA_INT);
            FunctionDescriptor wstr = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
            testIfRunning = linker.downcallHandle(lib.find("nvdaController_testIfRunning").orElseThrow(), noArgs);
            cancelSpeech = linker.downcallHandle(lib.find("nvdaController_cancelSpeech").orElseThrow(), noArgs);
            speakText = linker.downcallHandle(lib.find("nvdaController_speakText").orElseThrow(), wstr);
            brailleMessage = linker.downcallHandle(lib.find("nvdaController_brailleMessage").orElseThrow(), wstr);
        }

        boolean isRunning() {
            try { return (int) testIfRunning.invokeExact() == 0; } catch (Throwable t) { return false; }
        }

        void cancel() {
            try { int r = (int) cancelSpeech.invokeExact(); } catch (Throwable ignored) {}
        }

        void speak(String text) { call(speakText, text); }
        void braille(String text) { call(brailleMessage, text); }

        private static void call(MethodHandle h, String text) {
            try (Arena a = Arena.ofConfined()) {
                MemorySegment s = a.allocateFrom(text, StandardCharsets.UTF_16LE); // wchar_t*, null-terminated
                int r = (int) h.invokeExact(s);
            } catch (Throwable t) {
                Log.warn("NVDA call failed: " + t);
            }
        }
    }

    // ---------------------------------------------------------------- Windows voice (SAPI via System.Speech)

    private static final class Sapi {
        private final Process process;
        private final BufferedWriter in;

        Sapi() {
            String script = String.join("\n",
                    "[Console]::InputEncoding = [Text.Encoding]::UTF8",
                    "Add-Type -AssemblyName System.Speech",
                    "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer",
                    "$s.SetOutputToDefaultAudioDevice()",
                    "while (($l = [Console]::In.ReadLine()) -ne $null) {",
                    "  if ($l.Length -eq 0) { continue }",
                    "  $c = $l.Substring(0,1); $t = $l.Substring(1)",
                    "  if ($c -eq 'C') { $s.SpeakAsyncCancelAll() }",
                    "  elseif ($c -eq 'S') { $s.SpeakAsyncCancelAll(); [void]$s.SpeakAsync($t) }",
                    "  elseif ($c -eq 'Q') { [void]$s.SpeakAsync($t) }",
                    "  elseif ($c -eq 'R') { $s.Rate = [int]$t }",
                    "}");
            String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            try {
                process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("Windows voice not available", e);
            }
        }

        boolean alive() { return process.isAlive(); }

        private int lastRate = Integer.MIN_VALUE;

        void rate(int r) { if (r != lastRate) { lastRate = r; send("R" + Math.max(-10, Math.min(10, r))); } }
        void speak(String t, boolean interrupt) { send((interrupt ? "S" : "Q") + t); }
        void cancel() { send("C"); }

        private synchronized void send(String line) {
            try { in.write(line); in.newLine(); in.flush(); } catch (Exception e) { Log.warn("Windows voice: " + e); }
        }

        void close() { try { in.close(); } catch (Exception ignored) {} process.destroy(); }
    }
}
