// Minimal AE2 part host fixture, by HackerRouter, 2026.
package appeng.api.parts;

import net.minecraftforge.common.util.ForgeDirection;

public interface IPartHost {
    Object getPart(ForgeDirection side);
    void removePart(ForgeDirection side, boolean suppressUpdate);
}
