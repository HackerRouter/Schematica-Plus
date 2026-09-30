// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib HSV/RGBA color editor behavior, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.config;

import java.awt.Color;

import com.github.lunatrius.schematica.util.ColorValue;

public final class ColorPickerModel {
    public enum Channel {
        H(360), S(100), V(100), R(255), G(255), B(255), A(255);
        public final int maximum;
        Channel(int maximum) { this.maximum = maximum; }
    }

    private int color;
    private float hue, saturation, value;

    public ColorPickerModel(int color) { setColor(color); }
    public int color() { return color; }
    public String hex() { return ColorValue.format(color); }

    public void setColor(int color) {
        this.color = color;
        float[] hsv = Color.RGBtoHSB((color >>> 16) & 255, (color >>> 8) & 255, color & 255, null);
        hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
    }

    public boolean setHex(String hex) {
        try { setColor(ColorValue.parse(hex)); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    public float fraction(Channel channel) {
        switch (channel) {
            case H: return hue;
            case S: return saturation;
            case V: return value;
            case R: return ColorValue.red(color);
            case G: return ColorValue.green(color);
            case B: return ColorValue.blue(color);
            default: return ColorValue.alpha(color);
        }
    }

    public int component(Channel channel) { return Math.round(fraction(channel) * channel.maximum); }

    public void setComponent(Channel channel, int number) { setFraction(channel, number / (float) channel.maximum); }

    public void setFraction(Channel channel, float number) {
        if (!Float.isFinite(number)) return;
        number = clamp(number);
        switch (channel) {
            case H: hue = number; updateRGB(); break;
            case S: saturation = number; updateRGB(); break;
            case V: value = number; updateRGB(); break;
            case A: color = (color & 0xFFFFFF) | (Math.round(number * 255) << 24); break;
            default:
                int shift = channel == Channel.R ? 16 : channel == Channel.G ? 8 : 0;
                setColor((color & ~(255 << shift)) | (Math.round(number * 255) << shift));
        }
    }

    public void setSquare(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) return;
        value = clamp(x);
        saturation = 1 - clamp(y);
        updateRGB();
    }

    public int squareColor(float x, float y) { return Color.HSBtoRGB(hue, 1 - clamp(y), clamp(x)); }
    public int hueColor(float y) { return Color.HSBtoRGB(1 - clamp(y), 1, 1); }

    public int barColor(Channel channel, float position) {
        float t = clamp(position);
        switch (channel) {
            case H: return Color.HSBtoRGB(t, saturation, value);
            case S: return Color.HSBtoRGB(hue, t, value);
            case V: return Color.HSBtoRGB(hue, saturation, t);
            case A: return (color & 0xFFFFFF) | (Math.round(t * 255) << 24);
            default:
                int shift = channel == Channel.R ? 16 : channel == Channel.G ? 8 : 0;
                return 0xFF000000 | (color & ~(255 << shift)) | (Math.round(t * 255) << shift);
        }
    }

    private void updateRGB() { color = (color & 0xFF000000) | (Color.HSBtoRGB(hue, saturation, value) & 0xFFFFFF); }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
}
