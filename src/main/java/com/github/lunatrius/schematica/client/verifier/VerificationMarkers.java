package com.github.lunatrius.schematica.client.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.lunatrius.schematica.client.verifier.VerificationScan.Marker;

public final class VerificationMarkers {
    public final VerificationSelection selection = new VerificationSelection();
    private final List<Marker> markers = new ArrayList<>();
    private final List<Marker> view = Collections.unmodifiableList(markers);
    private final Map<Long, Marker> positions = new HashMap<>();
    private VerificationScan scan;
    private VerificationScan.MarkerSearch search;
    private long selectedRevision = -1, scanRevision = -1, filteredRevision = -1;
    private int x, y, z, limit;

    public void clear() {
        selection.clear(); markers.clear(); positions.clear(); scan = null; search = null;
        selectedRevision = scanRevision = filteredRevision = -1;
    }

    public List<Marker> markers() { return view; }
    public Marker at(int x, int y, int z) { return y < 0 || y > 255 ? null : positions.get(key(x, y, z)); }

    public void update(VerificationScan scan, int x, int y, int z, int limit, int budget, long deadline) {
        if (this.scan != scan || selectedRevision != selection.revision() || this.limit != limit) {
            this.scan = scan; this.limit = limit; selectedRevision = selection.revision();
            search = null; scanRevision = filteredRevision = -1; markers.clear(); positions.clear();
        }
        if (selection.empty()) return;
        if (filteredRevision != scan.revision()) {
            markers.removeIf(marker -> scan.at(marker.x, marker.y, marker.z) != marker.group || scan.ignored(marker.group));
            index(); filteredRevision = scan.revision();
        }
        if (search == null && (scanRevision != scan.revision() || this.x != x || this.y != y || this.z != z)) {
            this.x = x; this.y = y; this.z = z; scanRevision = scan.revision();
            search = scan.closest(selection, x + 0.5, y + 0.5, z + 0.5, limit);
        }
        if (search != null) {
            search.step(budget, deadline);
            if (search.done()) {
                markers.clear();
                for (Marker marker : search.result()) if (scan.at(marker.x, marker.y, marker.z) == marker.group && !scan.ignored(marker.group)) markers.add(marker);
                index(); search = null;
            }
        }
    }

    private void index() {
        positions.clear();
        for (Marker marker : markers) positions.put(key(marker.x, marker.y, marker.z), marker);
    }

    private static long key(int x, int y, int z) { return ((long) x & 0x3ffffffL) << 34 | ((long) z & 0x3ffffffL) << 8 | y & 255L; }
}
