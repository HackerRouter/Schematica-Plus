package gregtech.api.metatileentity;

public class BaseMetaPipeEntity extends CoverableTileEntity {
    public byte mConnections;
    public final PipeMeta meta = new PipeMeta();

    public PipeMeta getMetaTileEntity() { return meta; }

    public static class PipeMeta {
        public byte mConnections;
    }
}
