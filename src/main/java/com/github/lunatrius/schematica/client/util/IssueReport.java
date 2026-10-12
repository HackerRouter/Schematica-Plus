// Issue tracker link and introduction preference, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.util;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import org.lwjgl.Sys;

import com.github.lunatrius.schematica.reference.Reference;

public final class IssueReport {
    public static final String URL = "https://github.com/HackerRouter/Schematica-Plus/issues";
    private static final String SKIP_INTRODUCTION = "skip_issue_report_introduction";

    private IssueReport() {}

    public static boolean skipIntroduction() {
        return Boolean.parseBoolean(UiState.get(SKIP_INTRODUCTION, "false"));
    }

    public static void rememberSkipIntroduction() {
        UiState.set(SKIP_INTRODUCTION, "true");
    }

    public static void open(Runnable failed) {
        Thread thread = new Thread(() -> {
            if (!openBrowser()) Minecraft.getMinecraft().func_152344_a(failed);
        }, "Schematica Plus issue tracker");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean openBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(URL));
                return true;
            }
        } catch (Exception error) {
            Reference.logger.debug("Could not open the issue tracker with Desktop", error);
        }
        try {
            String[] command;
            switch (Util.getOSType()) {
                case OSX: command = new String[] {"/usr/bin/open", URL}; break;
                case WINDOWS: command = new String[] {"rundll32", "url.dll,FileProtocolHandler", URL}; break;
                default: command = new String[] {"xdg-open", URL}; break;
            }
            Process process = new ProcessBuilder(command).inheritIO().start();
            if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() == 0) return true;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception error) {
            Reference.logger.debug("Could not open the issue tracker with the system browser command", error);
        }
        try {
            if (Sys.openURL(URL)) return true;
        } catch (RuntimeException error) {
            Reference.logger.warn("Could not open the issue tracker", error);
        }
        return false;
    }
}
