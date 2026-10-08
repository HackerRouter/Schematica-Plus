// Minimal multipart test fixture, by HackerRouter, 2026.
package codechicken.multipart;

import java.util.Arrays;
import java.util.List;
import net.minecraft.tileentity.TileEntity;

public class TileMultipart extends TileEntity {
    private final List<Object> parts;
    public TileMultipart(Object... parts) { this.parts = Arrays.asList(parts); }
    public List<Object> jPartList() { return parts; }
}
