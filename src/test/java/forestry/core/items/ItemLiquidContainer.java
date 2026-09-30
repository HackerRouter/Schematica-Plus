package forestry.core.items;

public class ItemLiquidContainer {
    public enum Type { BUCKET, CAN, CAPSULE }

    private final Type type;
    private final boolean isDrink;

    public ItemLiquidContainer(Type type, boolean drink) {
        this.type = type;
        this.isDrink = drink;
    }
}
