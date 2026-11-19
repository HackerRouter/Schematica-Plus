package com.github.lunatrius.schematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModVersion implements Comparable<ModVersion> {

    private static final Pattern CORE = Pattern.compile("^[vV]?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(.*)$");
    private static final Pattern KNOWN_PRERELEASE = Pattern.compile("^[.-](alpha|beta|pre|rc)(?:[.-]?(\\d+))?(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_DESCRIBE = Pattern.compile("^-(\\d+)-g[0-9a-fA-F]{4,}(-dirty)?$");
    private static final Pattern BRANCH_OFFSET = Pattern.compile("^-([A-Za-z][A-Za-z0-9_]*)[.](\\d+)(-dirty)?$");
    private static final Pattern GENERIC_PRERELEASE = Pattern.compile("^-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)$");

    public final int major;
    public final int minor;
    public final int patch;
    private final List<String> prerelease;
    public final int devCommits;
    public final boolean dirty;
    public final String raw;

    private ModVersion(int major, int minor, int patch, List<String> prerelease, int devCommits, boolean dirty, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = Collections.unmodifiableList(prerelease);
        this.devCommits = devCommits;
        this.dirty = dirty;
        this.raw = raw;
    }

    public static ModVersion parse(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        String text = trimmed;
        int plus = text.indexOf('+');
        if (plus >= 0) text = text.substring(0, plus);
        Matcher core = CORE.matcher(text);
        if (!core.matches()) return null;
        int major;
        int minor;
        int patch;
        try {
            major = Integer.parseInt(core.group(1));
            minor = Integer.parseInt(core.group(2));
            patch = core.group(3) == null ? 0 : Integer.parseInt(core.group(3));
        } catch (NumberFormatException error) {
            return null;
        }
        String tail = core.group(4);
        if (tail.isEmpty()) {
            return new ModVersion(major, minor, patch, Collections.emptyList(), 0, false, trimmed);
        }
        List<String> pre = new ArrayList<>();
        String rest = tail;
        Matcher known = KNOWN_PRERELEASE.matcher(tail);
        if (known.matches()) {
            pre.add(known.group(1).toLowerCase(Locale.ROOT));
            if (known.group(2) != null) pre.add(known.group(2));
            rest = known.group(3);
        }
        if (rest.isEmpty()) {
            return new ModVersion(major, minor, patch, pre, 0, false, trimmed);
        }
        if ("-dirty".equalsIgnoreCase(rest) || ".dirty".equalsIgnoreCase(rest)) {
            return new ModVersion(major, minor, patch, pre, 0, true, trimmed);
        }
        Matcher describe = GIT_DESCRIBE.matcher(rest);
        if (describe.matches()) {
            try {
                int commits = Integer.parseInt(describe.group(1));
                return new ModVersion(major, minor, patch, pre, commits, describe.group(2) != null, trimmed);
            } catch (NumberFormatException error) {
                return null;
            }
        }
        Matcher branch = BRANCH_OFFSET.matcher(rest);
        if (branch.matches() && !isKnownPrereleaseLabel(branch.group(1))) {
            try {
                int commits = Integer.parseInt(branch.group(2));
                return new ModVersion(major, minor, patch, pre, commits, branch.group(3) != null, trimmed);
            } catch (NumberFormatException error) {
                return null;
            }
        }
        if (pre.isEmpty()) {
            Matcher generic = GENERIC_PRERELEASE.matcher(rest);
            if (generic.matches()) {
                String[] parts = generic.group(1).split("\\.");
                for (String part : parts) pre.add(part.toLowerCase(Locale.ROOT));
                return new ModVersion(major, minor, patch, pre, 0, false, trimmed);
            }
        }
        return null;
    }

    private static boolean isKnownPrereleaseLabel(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return "alpha".equals(lower) || "beta".equals(lower) || "pre".equals(lower) || "rc".equals(lower);
    }

    public boolean isPrerelease() {
        return !prerelease.isEmpty();
    }

    public boolean isDevBuild() {
        return devCommits > 0 || dirty;
    }

    @Override
    public int compareTo(ModVersion other) {
        int cmp = Integer.compare(major, other.major);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(minor, other.minor);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(patch, other.patch);
        if (cmp != 0) return cmp;
        cmp = comparePrerelease(prerelease, other.prerelease);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(devCommits, other.devCommits);
        if (cmp != 0) return cmp;
        return Boolean.compare(dirty, other.dirty);
    }

    private static int comparePrerelease(List<String> left, List<String> right) {
        if (left.isEmpty() && right.isEmpty()) return 0;
        if (left.isEmpty()) return 1;
        if (right.isEmpty()) return -1;
        int limit = Math.min(left.size(), right.size());
        for (int i = 0; i < limit; i++) {
            int cmp = compareIdentifier(left.get(i), right.get(i));
            if (cmp != 0) return cmp;
        }
        return Integer.compare(left.size(), right.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNum = isDigits(left);
        boolean rightNum = isDigits(right);
        if (leftNum && rightNum) {
            return compareNumericStrings(left, right);
        }
        if (leftNum) return -1;
        if (rightNum) return 1;
        return left.compareTo(right);
    }

    private static boolean isDigits(String text) {
        if (text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return true;
    }

    private static int compareNumericStrings(String left, String right) {
        String a = stripLeadingZeros(left);
        String b = stripLeadingZeros(right);
        int cmp = Integer.compare(a.length(), b.length());
        return cmp != 0 ? cmp : a.compareTo(b);
    }

    private static String stripLeadingZeros(String value) {
        int i = 0;
        while (i < value.length() - 1 && value.charAt(i) == '0') i++;
        return value.substring(i);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ModVersion)) return false;
        return compareTo((ModVersion) obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, prerelease, devCommits, dirty);
    }

    @Override
    public String toString() {
        return raw;
    }
}
