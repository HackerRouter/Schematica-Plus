package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.reference.Names;

final class ConfigReader {
    static final String LEGACY = Names.Config.LANG_PREFIX + ".";
    static final String GENERIC = "litematica.config.generic.name.";
    static final String INFO_OVERLAYS = "litematica.config.info_overlays.name.";
    static final String VISUALS = "litematica.config.visuals.name.";
    static final String MALILIB = "malilib.config.generic.name.";

    private final Configuration config;
    private final String category, language;

    ConfigReader(Configuration config, String category, String language) {
        this.config = config; this.category = category; this.language = language;
    }

    ConfigReader in(String category, String language) { return new ConfigReader(config, category, language); }

    Property label(String name, Property property) { return property.setLanguageKey(language + name); }

    boolean flag(String name, boolean fallback) {
        return label(name, config.get(category, name, fallback)).getBoolean(fallback);
    }

    boolean flag(String name, boolean fallback, String comment) {
        return label(name, config.get(category, name, fallback, comment)).getBoolean(fallback);
    }

    int integer(String name, int fallback, int min, int max) {
        return integer(name, fallback, min, max, null);
    }

    int integer(String name, int fallback, int min, int max, String comment) {
        Property previous = config.getCategory(category).get(name);
        if (previous != null && previous.getType() == Property.Type.DOUBLE) {
            double value = previous.getDouble(fallback);
            int rounded = Double.isFinite(value) ? (int) Math.round(Math.max(min, Math.min(max, value))) : fallback;
            config.getCategory(category).put(name, new Property(name, Integer.toString(rounded), Property.Type.INTEGER));
        }
        Property property = label(name, comment == null ? config.get(category, name, fallback) : config.get(category, name, fallback, comment));
        property.setMinValue(min).setMaxValue(max);
        int stored = property.getInt(fallback), value = Math.max(min, Math.min(max, stored));
        if (value != stored) property.set(value);
        return value;
    }

    double decimal(String name, double fallback, double min, double max) {
        Property property = label(name, config.get(category, name, fallback));
        property.setMinValue(min).setMaxValue(max);
        double value = property.getDouble(fallback);
        if (Double.isFinite(value) && value >= min && value <= max) return value;
        property.set(fallback);
        return fallback;
    }

    String choice(String name, String fallback, String... values) {
        Property property = label(name, config.get(category, name, fallback));
        property.setValidValues(values);
        String value = property.getString();
        for (String candidate : values) {
            if (!candidate.equalsIgnoreCase(value)) continue;
            if (!candidate.equals(value)) property.set(candidate);
            return candidate;
        }
        property.set(fallback);
        return fallback;
    }

    String text(String name, String fallback) {
        return label(name, config.get(category, name, fallback)).getString();
    }

    String[] list(String name, String[] fallback) {
        return label(name, config.get(category, name, fallback)).getStringList();
    }
}
