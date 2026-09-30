package com.github.lunatrius.schematica.client.printer;

import java.lang.reflect.Field;
import net.minecraft.item.ItemBucket;
import com.github.lunatrius.schematica.reference.Reference;

final class FluidContainerSupport {
    enum Use {
        BUCKET(false, false), FORESTRY_BUCKET(false, true), BLOCK(true, true);

        final boolean onBlock;
        final boolean stopOnLiquids;

        Use(boolean onBlock, boolean stopOnLiquids) {
            this.onBlock = onBlock;
            this.stopOnLiquids = stopOnLiquids;
        }
    }

    private static final ClassValue<FluidContainerSupport> SUPPORT = new ClassValue<FluidContainerSupport>() {
        @Override protected FluidContainerSupport computeValue(Class<?> type) {
            for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
                String name = parent.getName();
                if (name.equals("mods.railcraft.common.fluids.ItemBucketRailcraft")) {
                    return new FluidContainerSupport(Use.BUCKET, null, null);
                }
                if (name.equals("gregtech.common.items.ItemVolumetricFlask")
                    || name.equals("ic2.core.item.ItemFluidCell")) {
                    return new FluidContainerSupport(Use.BLOCK, null, null);
                }
                if (name.equals("forestry.core.items.ItemLiquidContainer")) {
                    try {
                        Field containerType = parent.getDeclaredField("type");
                        Field drink = parent.getDeclaredField("isDrink");
                        containerType.setAccessible(true);
                        drink.setAccessible(true);
                        return new FluidContainerSupport(Use.FORESTRY_BUCKET, containerType, drink);
                    } catch (ReflectiveOperationException | SecurityException e) {
                        Reference.logger.debug("Forestry container placement is unavailable", e);
                    }
                }
            }
            return new FluidContainerSupport(null, null, null);
        }
    };

    private final Use use;
    private final Field containerType;
    private final Field drink;

    private FluidContainerSupport(Use use, Field containerType, Field drink) {
        this.use = use;
        this.containerType = containerType;
        this.drink = drink;
    }

    static Use use(Object item) {
        if (item instanceof ItemBucket) return Use.BUCKET;
        if (item == null) return null;
        FluidContainerSupport support = SUPPORT.get(item.getClass());
        if (support.containerType != null) {
            try {
                Object type = support.containerType.get(item);
                if (!(type instanceof Enum<?>) || !((Enum<?>) type).name().equals("BUCKET")
                    || support.drink.getBoolean(item)) return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return support.use;
    }
}
