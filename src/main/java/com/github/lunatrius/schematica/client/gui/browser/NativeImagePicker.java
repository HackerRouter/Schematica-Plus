package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.minecraft.util.Util;

import com.github.lunatrius.schematica.reference.Reference;

final class NativeImagePicker {
    private static final String[] EXTENSIONS = {"png", "jpg", "jpeg", "bmp", "gif"};
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus preview image picker");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile File lastDirectory;

    private NativeImagePicker() {}

    static final class Result {
        final File file;
        final boolean unavailable;

        Result(File file, boolean unavailable) {
            this.file = file;
            this.unavailable = unavailable;
        }
    }

    static boolean isSupportedImageName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : EXTENSIONS) {
            if (lower.endsWith("." + ext)) return true;
        }
        return false;
    }

    static Future<Result> pickAsync(String title, File fallbackDirectory) {
        File startDir = lastDirectory != null && lastDirectory.isDirectory() ? lastDirectory : fallbackDirectory;
        return EXECUTOR.submit(() -> {
            Result result = pick(title, startDir);
            if (result.file != null && result.file.getParentFile() != null && result.file.getParentFile().isDirectory()) {
                lastDirectory = result.file.getParentFile();
            }
            return result;
        });
    }

    private static Result pick(String title, File initialDirectory) {
        String safeTitle = title == null || title.trim().isEmpty() ? "Choose Image" : title.trim();
        Util.EnumOS os = Util.getOSType();
        if (os == Util.EnumOS.OSX) {
            Result mac = pickMac(safeTitle, initialDirectory);
            if (mac != null) return mac;
            return new Result(null, true);
        }
        if (os == Util.EnumOS.WINDOWS) {
            Result win = pickAwt(safeTitle, initialDirectory);
            if (win != null) return win;
            Result swing = pickSwing(safeTitle, initialDirectory);
            return swing != null ? swing : new Result(null, true);
        }
        Result zenity = pickZenity(safeTitle, initialDirectory);
        if (zenity != null) return zenity;
        Result kdialog = pickKdialog(safeTitle, initialDirectory);
        if (kdialog != null) return kdialog;
        Result swing = pickSwing(safeTitle, initialDirectory);
        if (swing != null) return swing;
        Result awt = pickAwt(safeTitle, initialDirectory);
        return awt != null ? awt : new Result(null, true);
    }

    private static Result pickMac(String title, File initialDirectory) {
        String escapedTitle = title.replace("\\", "\\\\").replace("\"", "\\\"");
        StringBuilder script = new StringBuilder("POSIX path of (choose file of type {\"public.png\", \"public.jpeg\", \"com.microsoft.bmp\", \"com.compuserve.gif\"} with prompt \"")
            .append(escapedTitle).append("\"");
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            String escapedDir = initialDirectory.getAbsolutePath().replace("\\", "\\\\").replace("\"", "\\\"");
            script.append(" default location POSIX file \"").append(escapedDir).append("\"");
        }
        script.append(")");
        return runCommand(new String[] {"osascript", "-e", script.toString()}, true);
    }

    private static Result pickZenity(String title, File initialDirectory) {
        List<String> command = new ArrayList<>();
        command.add("zenity");
        command.add("--file-selection");
        command.add("--title=" + title);
        command.add("--file-filter=Images | *.png *.jpg *.jpeg *.bmp *.gif *.PNG *.JPG *.JPEG *.BMP *.GIF");
        command.add("--file-filter=All files | *");
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            String dir = initialDirectory.getAbsolutePath();
            if (!dir.endsWith(File.separator)) dir += File.separator;
            command.add("--filename=" + dir);
        }
        return runCommand(command.toArray(new String[0]), false);
    }

    private static Result pickKdialog(String title, File initialDirectory) {
        List<String> command = new ArrayList<>();
        command.add("kdialog");
        command.add("--getopenfilename");
        command.add(initialDirectory != null && initialDirectory.isDirectory() ? initialDirectory.getAbsolutePath() : ".");
        command.add("*.png *.jpg *.jpeg *.bmp *.gif | Images");
        command.add("--title");
        command.add(title);
        return runCommand(command.toArray(new String[0]), false);
    }

    private static Result runCommand(String[] command, boolean treatNonZeroAsCancel) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(false).start();
            String output = readFirstLine(process);
            int code = process.waitFor();
            if (code == 0 && output != null && !output.trim().isEmpty()) {
                return new Result(new File(output.trim()), false);
            }
            if (code == 1 || code == 5 || treatNonZeroAsCancel) {
                return new Result(null, false);
            }
            return null;
        } catch (IOException e) {
            Reference.logger.debug("Native file picker '{}' is unavailable", command[0], e);
            return null;
        } catch (InterruptedException e) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            return new Result(null, false);
        }
    }

    private static String readFirstLine(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    private static Result pickAwt(String title, File initialDirectory) {
        if (GraphicsEnvironment.isHeadless()) return null;
        Frame owner = null;
        try {
            owner = new Frame();
            FileDialog dialog = new FileDialog(owner, title, FileDialog.LOAD);
            if (initialDirectory != null && initialDirectory.isDirectory()) {
                dialog.setDirectory(initialDirectory.getAbsolutePath());
            }
            dialog.setFile("*.png;*.jpg;*.jpeg;*.bmp;*.gif");
            dialog.setFilenameFilter((dir, name) -> isSupportedImageName(name));
            dialog.setVisible(true);
            String file = dialog.getFile();
            String directory = dialog.getDirectory();
            if (file == null || directory == null) return new Result(null, false);
            return new Result(new File(directory, file), false);
        } catch (RuntimeException | LinkageError e) {
            Reference.logger.debug("AWT file dialog failed", e);
            return null;
        } finally {
            if (owner != null) owner.dispose();
        }
    }

    private static Result pickSwing(String title, File initialDirectory) {
        if (GraphicsEnvironment.isHeadless()) return null;
        try {
            JFileChooser chooser = initialDirectory != null && initialDirectory.isDirectory()
                ? new JFileChooser(initialDirectory) : new JFileChooser();
            chooser.setDialogTitle(title);
            chooser.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG, BMP, GIF)", EXTENSIONS));
            int choice = chooser.showOpenDialog(null);
            if (choice == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                return new Result(chooser.getSelectedFile(), false);
            }
            return new Result(null, false);
        } catch (RuntimeException | LinkageError e) {
            Reference.logger.debug("Swing file chooser failed", e);
            return null;
        }
    }
}
