package com.github.lunatrius.schematica.client.gui.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.lunatrius.schematica.cloud.CloudCatalog;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CloudNodeTest {
    @Test public void foldersDifferingOnlyInCaseAreBothListed() {
        CloudPath root = CloudPath.root(CloudPath.PRIVATE);
        List<CloudPath> directories = Arrays.asList(root.child("Tower"), root.child("tower"), root.child("alpha"));
        CloudCatalog catalog = new CloudCatalog(directories, new ArrayList<CloudFile>(), 0, 0, true, true, false);
        List<CloudNode> nodes = CloudNode.list(catalog, root, "");
        assertEquals(3, nodes.size());
        assertEquals("alpha", nodes.get(0).name());
        assertEquals("Tower", nodes.get(1).name());
        assertEquals("tower", nodes.get(2).name());
    }
}
