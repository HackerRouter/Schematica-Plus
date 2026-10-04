package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

/** The materials of several placements counted one after another and added up (LitematList's merged bill of materials). */
final class CombinedMaterialScan implements MaterialScanner {
    private final List<MaterialScanner> scans;
    private final EntityPlayer player;
    private final Map<MaterialItemKey, long[]> sums = new LinkedHashMap<>();
    private final Map<MaterialItemKey, String[]> names = new LinkedHashMap<>();
    private int index, skipped, unverified;

    CombinedMaterialScan(List<MaterialScanner> scans, EntityPlayer player) {
        this.scans = scans;
        this.player = player;
    }

    @Override public void step() {
        if (done()) return;
        MaterialScanner scan = scans.get(index);
        scan.step();
        if (!scan.done()) return;
        for (MaterialListModel.Entry<MaterialItemKey> entry : scan.result()) {
            long[] sum = sums.computeIfAbsent(entry.key, k -> new long[4]);
            sum[0] += entry.total; sum[1] += entry.missing; sum[2] += entry.mismatched; sum[3] += entry.unknown;
            names.putIfAbsent(entry.key, new String[] {entry.name, entry.registryName});
        }
        skipped += scan.skipped();
        unverified += scan.unverified();
        index++;
    }

    @Override public boolean done() { return index >= scans.size(); }

    @Override public int percent() {
        if (scans.isEmpty() || done()) return 100;
        return (index * 100 + scans.get(index).percent()) / scans.size();
    }

    @Override public int skipped() { return skipped; }

    @Override public int unverified() { return unverified; }

    @Override public List<MaterialListModel.Entry<MaterialItemKey>> result() {
        List<MaterialListModel.Entry<MaterialItemKey>> result = new ArrayList<>();
        for (Map.Entry<MaterialItemKey, long[]> sum : sums.entrySet()) {
            long[] s = sum.getValue();
            int total = (int) Math.min(Integer.MAX_VALUE, s[0]);
            int missing = (int) Math.min(total, s[1]);
            int mismatched = (int) Math.min(missing, s[2]);
            int unknown = (int) Math.min(missing - mismatched, s[3]);
            String[] name = names.get(sum.getKey());
            result.add(new MaterialListModel.Entry<>(sum.getKey(), name[0], name[1], total, missing, mismatched, unknown));
        }
        MaterialScan.updateAvailable(result, player);
        return result;
    }
}
