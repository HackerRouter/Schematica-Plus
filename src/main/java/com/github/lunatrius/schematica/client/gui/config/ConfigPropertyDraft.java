package com.github.lunatrius.schematica.client.gui.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.util.ColorValue;

public final class ConfigPropertyDraft {
    public final Property property;
    private final boolean color;
    private String text;
    private String[] values;
    public boolean slider;

    public ConfigPropertyDraft(Property property) { this(property, false); }

    public ConfigPropertyDraft(Property property, boolean color) {
        this.property = property;
        this.color = color;
        text = property.getString();
        values = property.getStringList().clone();
    }

    public String text() { return text; }

    public void setText(String text) { this.text = text; }

    public String[] values() { return values.clone(); }

    public void setValues(String[] values) { this.values = values.clone(); }

    public boolean numeric() {
        return !property.isList() && (property.getType() == Property.Type.INTEGER
            || property.getType() == Property.Type.DOUBLE);
    }

    public boolean valid() {
        if (!property.isList()) return validValue(text);
        if (property.getMaxListLength() >= 0 && values.length > property.getMaxListLength()) return false;
        if (property.isListLengthFixed() && values.length != property.getDefaults().length) return false;
        for (String value : values) if (!validValue(value)) return false;
        return true;
    }

    private boolean validValue(String value) {
        try {
            if (color) ColorValue.parse(value);
            if (property.getType() == Property.Type.INTEGER || property.getType() == Property.Type.DOUBLE) {
                double number = property.getType() == Property.Type.INTEGER ? Integer.parseInt(value)
                    : Double.parseDouble(value);
                if (!Double.isFinite(number) || number < Double.parseDouble(property.getMinValue())
                    || number > Double.parseDouble(property.getMaxValue())) return false;
            } else if (property.getType() == Property.Type.BOOLEAN
                && !"true".equals(value) && !"false".equals(value)) return false;
            String[] allowed = property.getValidValues();
            return (allowed == null || allowed.length == 0 || Arrays.asList(allowed).contains(value))
                && (property.getValidationPattern() == null || property.getValidationPattern().matcher(value).matches());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean modified() {
        if (!valid()) return true;
        return property.isList() ? !Arrays.equals(values, property.getDefaults())
            : !sameValue(text, property.getDefault());
    }

    private boolean sameValue(String a, String b) {
        if (color) {
            try { return ColorValue.parse(a) == ColorValue.parse(b); }
            catch (IllegalArgumentException ignored) { return false; }
        }
        if (numeric()) {
            try {
                return Double.compare(Double.parseDouble(a), Double.parseDouble(b)) == 0;
            } catch (NumberFormatException ignored) {}
        }
        return a.equals(b);
    }

    public void reset() {
        text = property.getDefault();
        values = property.getDefaults().clone();
    }

    public boolean apply() {
        if (!valid()) return false;
        if (property.isList()) {
            if (Arrays.equals(values, property.getStringList())) return false;
            property.set(values.clone());
        } else {
            if (sameValue(text, property.getString())) return false;
            property.set(color ? ColorValue.format(ColorValue.parse(text)) : text);
        }
        return true;
    }

    public boolean supportsSlider() {
        if (!numeric()) return false;
        double min = Double.parseDouble(property.getMinValue());
        double max = Double.parseDouble(property.getMaxValue());
        return min < max && min >= -1_000_000 && max <= 1_000_000;
    }

    public double fraction() {
        if (!valid() || !supportsSlider()) return 0;
        double min = Double.parseDouble(property.getMinValue());
        return (Double.parseDouble(text) - min) / (Double.parseDouble(property.getMaxValue()) - min);
    }

    public void setFraction(double fraction) {
        if (!supportsSlider() || !Double.isFinite(fraction)) return;
        double min = Double.parseDouble(property.getMinValue());
        double value = min + Math.max(0, Math.min(1, fraction)) * (Double.parseDouble(property.getMaxValue()) - min);
        text = property.getType() == Property.Type.INTEGER ? Long.toString(Math.round(value))
            : BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
