package com.github.lunatrius.schematica.client.verifier;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.github.lunatrius.schematica.client.verifier.VerificationScan.Group;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Pair;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Type;

public final class VerificationSelection {
    private final Set<Type> categories = EnumSet.noneOf(Type.class);
    private final Set<Pair> entries = new HashSet<>();
    private long revision;

    public void toggle(Type type) {
        if (type == Type.ALL || type == Type.CORRECT) return;
        if (!categories.remove(type)) {
            categories.add(type);
            entries.removeIf(pair -> VerificationScan.classify(pair.expected, pair.found) == type);
        }
        revision++;
    }

    public void toggle(Group group) {
        if (group.type == Type.CORRECT) return;
        if (!entries.remove(group.pair)) {
            categories.remove(group.type);
            entries.add(group.pair);
        }
        revision++;
    }

    public boolean category(Type type) { return categories.contains(type); }
    public boolean entry(Group group) { return entries.contains(group.pair); }
    public boolean includes(Group group) { return category(group.type) || entry(group); }
    public boolean empty() { return categories.isEmpty() && entries.isEmpty(); }
    public long revision() { return revision; }
    public void clear() { categories.clear(); entries.clear(); revision++; }
}
