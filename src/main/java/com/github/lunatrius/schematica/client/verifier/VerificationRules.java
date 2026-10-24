package com.github.lunatrius.schematica.client.verifier;

import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.util.BlockGroups;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.State;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Type;

final class VerificationRules {
    private final boolean different = BlockGroups.enabled;
    private final boolean age = VisualSettings.ignoreCropAge;
    private final boolean survival = VisualSettings.ignoreSurvivalStates;
    private final boolean fluids = VisualSettings.ignoreExistingFluids;

    boolean current() {
        return different == BlockGroups.enabled && age == VisualSettings.ignoreCropAge
            && survival == VisualSettings.ignoreSurvivalStates && fluids == VisualSettings.ignoreExistingFluids;
    }

    Type classify(State a, State b, BlockGroups.Snapshot da, BlockGroups.Snapshot db) {
        if (a.equals(b)) return Type.CORRECT;
        if (a.air()) return Type.EXTRA;
        if (b.air()) return Type.MISSING;
        boolean same = a.block.equals(b.block);
        if (!different || !da.known || !db.known) {
            if (!same) return Type.WRONG_BLOCK;
            return tolerated(a, b, da, db, true) ? Type.CORRECT : Type.WRONG_STATE;
        }
        if (tolerated(a, b, da, db, same)) return Type.CORRECT;
        boolean group = da.group != null && da.group.equals(db.group);
        if (!same && !group) return Type.WRONG_BLOCK;
        if (!group) return Type.WRONG_STATE;
        int va = da.variants | (survival ? da.survival : 0), vb = db.variants | (survival ? db.survival : 0);
        return (a.metadata & ~va) == (b.metadata & ~vb) ? Type.DIFF_BLOCK : Type.WRONG_STATE;
    }

    private boolean tolerated(State a, State b, BlockGroups.Snapshot da, BlockGroups.Snapshot db, boolean same) {
        return survival && (same || da.switched != null && da.switched.equals(db.switched))
            && (a.metadata & ~da.survival) == (b.metadata & ~da.survival)
            || age && same && (a.metadata & ~da.age) == (b.metadata & ~da.age);
    }
}
