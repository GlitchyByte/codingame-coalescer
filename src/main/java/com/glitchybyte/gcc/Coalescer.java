// Copyright 2020 GlitchyByte
// SPDX-License-Identifier: Apache-2.0

package com.glitchybyte.gcc;

import com.glitchybyte.glib.GStrings;
import com.glitchybyte.glib.console.GConsole;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class Coalescer implements Runnable {

    /**
     * Main Java class file as per CodinGame.
     */
    public static final String PLAYER_JAVA = "Player.java";

    /**
     * Magic comment to be replaced by a timestamp.
     */
    private static final String MAGIC_COMMENT_TIMESTAMP = "[[GCC::TIMESTAMP]]";

    /**
     * Magic comment to be replaced by all other Java classes.
     */
    private static final String MAGIC_COMMENT_CODE = "[[GCC::CODE]]";

    /**
     * Timestamp formatter.
     */
    private static final DateTimeFormatter timestampFormatter =
            DateTimeFormatter.ofPattern("uuuu-MM-dd|HH:mm:ss.SSS", Locale.US)
                    .withZone(ZoneOffset.systemDefault());

    private final Path watchedPath;
    private final Path outputPath;
    private int fileLinesPrinted = 0;

    /**
     * Creates the coalescer.
     *
     * @param watchedPath Watched directory.
     * @param outputPath Output directory.
     */
    public Coalescer(final Path watchedPath, final Path outputPath) {
        this.watchedPath = watchedPath;
        this.outputPath = outputPath.resolve(PLAYER_JAVA);
    }

    /**
     * Main loop of the thread.
     */
    @Override
    public void run() {
        GConsole.println("Watching: %s", GConsole.coloredText(watchedPath.toString(), GConsole.COLOR_BRIGHT_WHITE));
        GConsole.println("Output: %s", GConsole.coloredText(outputPath.toString(), GConsole.COLOR_BRIGHT_WHITE));
        GConsole.flush();
        final WatchService watchService;
        try {
            // Register watcher.
            watchService = FileSystems.getDefault().newWatchService();
            watchedPath.register(watchService,
                    new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE },
                    SensitivityWatchEventModifier.HIGH);
            // Watch and update loop.
            while (true) {
                updateOutput();
                try {
                    final WatchKey key = watchService.take();
                    if (key == null) {
                        continue;
                    }
                    key.pollEvents();
                    if (!key.reset()) {
                        GConsole.println("Can't reset watch key. Exiting!");
                        GConsole.flush();
                        return;
                    }
                } catch (final InterruptedException e) {
                    // We out!
                    return;
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void updateOutput() throws IOException {
        GConsole.print(GConsole.cursorUp(fileLinesPrinted));
        for (int i = 0; i < fileLinesPrinted; ++i) {
            GConsole.println(" ".repeat(80));
        }
        GConsole.print(GConsole.cursorUp(fileLinesPrinted));
        fileLinesPrinted = 0;
        final List<Path> files = Files.list(watchedPath)
                .filter(path -> Files.isRegularFile(path) &&
                        (path.getFileName().toString().length() > 5) &&
                        path.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());
        final List<JavaClass> javaClasses = readJavaClasses(files);
        writeOutput(javaClasses);
    }

    private List<JavaClass> readJavaClasses(final List<Path> files) throws IOException {
        final List<JavaClass> javaClasses = new ArrayList<>();
        for (final Path file: files) {
            final JavaClass javaClass = readJavaClass(file);
            javaClasses.add(javaClass);
            ++fileLinesPrinted;
        }
        if (fileLinesPrinted == 0) {
            GConsole.println("Read: %s", GConsole.coloredText("Nothing", GConsole.COLOR_BRIGHT_YELLOW));
            GConsole.flush();
            ++fileLinesPrinted;
        }
        return javaClasses;
    }

    private JavaClass readJavaClass(final Path file) throws IOException {
        final String filename = file.getFileName().toString();
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        GConsole.println("Read: %s (%d lines)", GConsole.coloredText(filename, GConsole.COLOR_BRIGHT_GREEN), lines.size());
        GConsole.flush();
        return new JavaClass(filename, lines);
    }

    private void writeOutput(final List<JavaClass> javaClasses) throws IOException {
        try (final Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writeImports(writer, javaClasses);
            final JavaClass player = javaClasses.stream().filter(jc -> jc.name.equals(PLAYER_JAVA)).findFirst().orElse(null);
            if (player == null) {
                throw new IllegalStateException("No Player.java found, but this should never happen!");
            }
            javaClasses.remove(player);
            writeMainClass(writer, player, javaClasses);
        }
        GConsole.println("Write: %s", GConsole.coloredText(PLAYER_JAVA, GConsole.COLOR_BRIGHT_CYAN));
        GConsole.flush();
        ++fileLinesPrinted;
    }

    private void writeImports(final Writer writer, final List<JavaClass> javaClasses) throws IOException {
        final Set<String> imports = new HashSet<>();
        javaClasses.forEach(javaClass -> imports.addAll(javaClass.getImports()));
        for (final String line: imports) {
            writer.write(line + GStrings.NEW_LINE);
        }
        writer.write(GStrings.NEW_LINE);
    }

    private void writeMainClass(final Writer writer, final JavaClass player, final List<JavaClass> others) throws IOException {
        for (final String line: player.getContents()) {
            if (line.contains(MAGIC_COMMENT_TIMESTAMP)) {
                writer.write("    // " + getTimestamp() + GStrings.NEW_LINE);
                continue;
            } else if (line.contains(MAGIC_COMMENT_CODE)) {
                for (final JavaClass javaClass: others) {
                    writeOtherClass(writer, javaClass);
                    writer.write(GStrings.NEW_LINE);
                }
                continue;
            }
            writer.write(line + GStrings.NEW_LINE);
        }
    }

    private void writeOtherClass(final Writer writer, final JavaClass javaClass) throws IOException {
        boolean first = true;
        for (final String line: javaClass.getContents()) {
            if (first) {
                writer.write("    static " + line + GStrings.NEW_LINE);
                first = false;
                continue;
            }
            writer.write("    " + line + GStrings.NEW_LINE);
        }
    }

    private String getTimestamp() {
        return timestampFormatter.format(Instant.now());
    }
}
