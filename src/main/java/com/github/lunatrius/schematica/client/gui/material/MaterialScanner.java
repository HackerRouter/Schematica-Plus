package com.github.lunatrius.schematica.client.gui.material;

import java.util.List;

public interface MaterialScanner {
    void step();
    boolean done();
    int percent();
    int skipped();
    default int unverified() { return 0; }
    List<MaterialListModel.Entry<MaterialItemKey>> result();
}
