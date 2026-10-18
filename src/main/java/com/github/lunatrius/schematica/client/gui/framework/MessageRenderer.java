// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib MessageRenderer, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.lunatrius.schematica.util.MessageType;

public final class MessageRenderer {
    private static final int BOX_WIDTH = 400;

    private final List<Message> messages = new ArrayList<>();
    private final int backgroundColor;
    private final int borderColor;
    private final boolean useBorder;
    private final boolean centeredVertically;

    public MessageRenderer(int backgroundColor, int borderColor, boolean useBorder, boolean centeredVertically) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.useBorder = useBorder;
        this.centeredVertically = centeredVertically;
    }

    public void add(MessageType type, long lifeTime, String text) {
        messages.add(new Message(type.formatting(), System.currentTimeMillis() + lifeTime, text.replace("\\n", "\n")));
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public void draw(UiDraw draw, int x, int y, int lineHeight) {
        if (messages.isEmpty()) return;
        int height = 20;
        for (Message message : messages) height += message.lines(draw).size() * lineHeight - 1 + 5;
        x -= BOX_WIDTH / 2;
        y -= centeredVertically ? height / 2 : height;
        UiBounds box = new UiBounds(x, y, BOX_WIDTH, height);
        int inset = useBorder ? 1 : 0;
        draw.fill(new UiBounds(x + inset, y + inset, BOX_WIDTH - 2 * inset, height - 2 * inset), backgroundColor);
        if (useBorder) draw.border(box, borderColor);
        x += 10;
        y += 10;
        long now = System.currentTimeMillis();
        for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext();) {
            Message message = iterator.next();
            for (String line : message.lines(draw)) {
                draw.text(message.format + line + "§r", x, y, 0xFFFFFFFF);
                y += lineHeight;
            }
            y += 3;
            if (now > message.expires) iterator.remove();
        }
    }

    private static final class Message {
        final String format;
        final long expires;
        final String text;
        List<String> lines;

        Message(String format, long expires, String text) {
            this.format = format;
            this.expires = expires;
            this.text = text;
        }

        List<String> lines(UiDraw draw) {
            if (lines == null) lines = new ArrayList<>(UiTextWrap.wrap(text, BOX_WIDTH - 20, draw::textWidth));
            return lines;
        }
    }
}
