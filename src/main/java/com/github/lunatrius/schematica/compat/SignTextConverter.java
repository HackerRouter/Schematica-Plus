package com.github.lunatrius.schematica.compat;

import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

/**
 * Converts modern sign text NBT to 1.7.10 format.
 * Extracted from TileEntityTranslator for maintainability.
 *
 * Handles:
 * - Modern 1.20+ front_text/back_text with messages list
 * - NBT text components (TAG_COMPOUND) to JSON string conversion
 * - JSON text component string normalization for 1.7.10
 * - snake_case to camelCase key conversion (click_event -> clickEvent)
 *
 * @author HackerRouter
 */
public final class SignTextConverter {

    private SignTextConverter() {}

    /**
     * Converts modern sign NBT to 1.7.10 format in-place.
     * Modern (1.20+): front_text/back_text with messages list containing JSON text components
     * 1.7.10: Text1-Text4 as JSON text component strings
     */
    public static void convertSign(NBTTagCompound teTag) {
        // Modern 1.20+ format: front_text.messages[]
        if (teTag.hasKey("front_text", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound frontText = teTag.getCompoundTag("front_text");
            if (frontText.hasKey("messages", Constants.NBT.TAG_LIST)) {
                // Try reading as TAG_STRING first
                NBTTagList messagesStr = frontText.getTagList("messages", Constants.NBT.TAG_STRING);
                if (messagesStr.tagCount() > 0) {
                    for (int i = 0; i < Math.min(messagesStr.tagCount(), 4); i++) {
                        String jsonText = messagesStr.getStringTagAt(i);
                        teTag.setString("Text" + (i + 1), convertJsonTextTo1710(jsonText));
                    }
                } else {
                    // Try reading as TAG_COMPOUND (NBT text components from litematic)
                    NBTTagList messagesCompound = frontText.getTagList("messages", Constants.NBT.TAG_COMPOUND);
                    if (messagesCompound.tagCount() > 0) {
                        for (int i = 0; i < Math.min(messagesCompound.tagCount(), 4); i++) {
                            NBTTagCompound textComponent = messagesCompound.getCompoundTagAt(i);
                            String jsonText = nbtTextComponentToJson(textComponent);
                            teTag.setString("Text" + (i + 1), jsonText);
                        }
                    }
                }
            }
            teTag.removeTag("front_text");
            teTag.removeTag("back_text");
            teTag.removeTag("is_waxed");
        }

        // Ensure all 4 text lines exist; convert any remaining JSON format
        for (int i = 1; i <= 4; i++) {
            String key = "Text" + i;
            if (teTag.hasKey(key, Constants.NBT.TAG_STRING)) {
                String text = teTag.getString(key);
                if (text.startsWith("{") || text.startsWith("[")) {
                    teTag.setString(key, convertJsonTextTo1710(text));
                }
            } else {
                teTag.setString(key, "{\"text\":\"\"}");
            }
        }
    }

    /**
     * Converts an NBT text component compound to a 1.7.10-compatible JSON string.
     * Handles the modern litematic format where text components are stored as NBT compounds.
     *
     * 1.7.10 IChatComponent.Serializer supports:
     *   text, color, bold, italic, underlined, strikethrough, obfuscated,
     *   clickEvent {action, value}, hoverEvent {action, value}, extra[]
     *
     * Modern format differences:
     *   - snake_case keys: click_event -> clickEvent, hover_event -> hoverEvent
     *   - clickEvent uses "command" key instead of "value"
     *   - hoverEvent uses "contents" instead of "value"
     *   - Boolean formatting stored as NBT byte (1B/0B) instead of JSON boolean
     */
    static String nbtTextComponentToJson(NBTTagCompound nbt) {
        if (nbt == null || nbt.hasNoTags()) return "{\"text\":\"\"}";

        Set<String> keys = nbt.func_150296_c();

        // Check for the empty text marker {"":""}
        if (keys.size() == 1 && keys.contains("")) {
            return "{\"text\":\"\"}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        boolean hasText = false;

        for (String key : keys) {
            String camelKey = convertKeyToCamelCase(key);

            // Handle clickEvent specially — convert to 1.7.10 format
            if ("clickEvent".equals(camelKey)) {
                if (!first) sb.append(',');
                first = false;
                sb.append("\"clickEvent\":");
                appendClickEventAsJson(sb, nbt.getTag(key));
                continue;
            }
            // Handle hoverEvent specially
            if ("hoverEvent".equals(camelKey)) {
                if (!first) sb.append(',');
                first = false;
                sb.append("\"hoverEvent\":");
                appendHoverEventAsJson(sb, nbt.getTag(key));
                continue;
            }

            // Skip keys not supported by 1.7.10 text components
            if (!isSupported1710TextKey(camelKey)) {
                continue;
            }

            if ("text".equals(camelKey)) hasText = true;

            if (!first) sb.append(',');
            first = false;

            sb.append('"').append(escapeJsonString(camelKey)).append("\":");

            NBTBase tag = nbt.getTag(key);
            if ("extra".equals(camelKey)) {
                appendExtraArrayAsJson(sb, tag);
            } else {
                appendSimpleNbtValueAsJson(sb, tag);
            }
        }

        // Ensure "text" key is always present
        if (!hasText) {
            if (!first) sb.append(',');
            sb.append("\"text\":\"\"");
        }

        sb.append('}');
        return sb.toString();
    }

    /**
     * Returns true if the key is a valid 1.7.10 text component field.
     */
    private static boolean isSupported1710TextKey(String key) {
        switch (key) {
            case "text":
            case "color":
            case "bold":
            case "italic":
            case "underlined":
            case "strikethrough":
            case "obfuscated":
            case "extra":
            case "clickEvent":
            case "hoverEvent":
            case "insertion":
                return true;
            default:
                return false;
        }
    }

    /**
     * Converts a modern clickEvent NBT to 1.7.10 JSON format.
     * Modern: {action: "run_command", command: "/say hi"}
     * 1.7.10: {"action":"run_command","value":"/say hi"}
     */
    private static void appendClickEventAsJson(StringBuilder sb, NBTBase tag) {
        if (tag == null || tag.getId() != Constants.NBT.TAG_COMPOUND) {
            sb.append("{\"action\":\"run_command\",\"value\":\"\"}");
            return;
        }
        NBTTagCompound ce = (NBTTagCompound) tag;
        String action = ce.hasKey("action") ? ce.getString("action") : "run_command";
        String value = "";
        if (ce.hasKey("value")) {
            value = ce.getString("value");
        } else if (ce.hasKey("command")) {
            value = ce.getString("command");
        } else if (ce.hasKey("url")) {
            value = ce.getString("url");
        }
        sb.append("{\"action\":\"").append(escapeJsonString(action))
          .append("\",\"value\":\"").append(escapeJsonString(value)).append("\"}");
    }

    /**
     * Converts a modern hoverEvent NBT to 1.7.10 JSON format.
     * Modern: {action: "show_text", contents: {text: "hello"}}
     * 1.7.10: {"action":"show_text","value":{"text":"hello"}}
     */
    private static void appendHoverEventAsJson(StringBuilder sb, NBTBase tag) {
        if (tag == null || tag.getId() != Constants.NBT.TAG_COMPOUND) {
            sb.append("{\"action\":\"show_text\",\"value\":\"\"}");
            return;
        }
        NBTTagCompound he = (NBTTagCompound) tag;
        String action = he.hasKey("action") ? he.getString("action") : "show_text";
        sb.append("{\"action\":\"").append(escapeJsonString(action)).append("\",\"value\":");
        if (he.hasKey("value")) {
            appendSimpleNbtValueAsJson(sb, he.getTag("value"));
        } else if (he.hasKey("contents")) {
            NBTBase contents = he.getTag("contents");
            if (contents.getId() == Constants.NBT.TAG_COMPOUND) {
                sb.append(nbtTextComponentToJson((NBTTagCompound) contents));
            } else {
                appendSimpleNbtValueAsJson(sb, contents);
            }
        } else {
            sb.append("\"\"");
        }
        sb.append('}');
    }

    /**
     * Appends the "extra" array, recursively converting each element as a text component.
     */
    private static void appendExtraArrayAsJson(StringBuilder sb, NBTBase tag) {
        if (tag == null || tag.getId() != Constants.NBT.TAG_LIST) {
            sb.append("[]");
            return;
        }
        NBTTagList list = (NBTTagList) tag;
        sb.append('[');
        for (int i = 0; i < list.tagCount(); i++) {
            if (i > 0) sb.append(',');
            int listType = list.func_150303_d();
            if (listType == Constants.NBT.TAG_COMPOUND) {
                sb.append(nbtTextComponentToJson(list.getCompoundTagAt(i)));
            } else if (listType == Constants.NBT.TAG_STRING) {
                String s = list.getStringTagAt(i);
                sb.append("{\"text\":\"").append(escapeJsonString(s)).append("\"}");
            } else {
                sb.append("{\"text\":\"\"}");
            }
        }
        sb.append(']');
    }

    /**
     * Appends a simple NBT value as a JSON value (string, boolean, number).
     */
    private static void appendSimpleNbtValueAsJson(StringBuilder sb, NBTBase tag) {
        if (tag == null) {
            sb.append("\"\"");
            return;
        }

        switch (tag.getId()) {
            case Constants.NBT.TAG_STRING:
                sb.append('"').append(escapeJsonString(((NBTTagString) tag).func_150285_a_())).append('"');
                break;
            case Constants.NBT.TAG_BYTE:
                byte bVal = ((NBTTagByte) tag).func_150290_f();
                if (bVal == 1) sb.append("true");
                else if (bVal == 0) sb.append("false");
                else sb.append(bVal);
                break;
            case Constants.NBT.TAG_INT:
                sb.append(((NBTTagInt) tag).func_150287_d());
                break;
            case Constants.NBT.TAG_SHORT:
                sb.append(((NBTTagShort) tag).func_150289_e());
                break;
            case Constants.NBT.TAG_LONG:
                sb.append(((NBTTagLong) tag).func_150291_c());
                break;
            case Constants.NBT.TAG_FLOAT:
                sb.append(((NBTTagFloat) tag).func_150288_h());
                break;
            case Constants.NBT.TAG_DOUBLE:
                sb.append(((NBTTagDouble) tag).func_150286_g());
                break;
            default:
                sb.append('"').append(escapeJsonString(tag.toString())).append('"');
                break;
        }
    }

    /**
     * Converts a modern JSON text component string to a 1.7.10-compatible JSON text string.
     */
    static String convertJsonTextTo1710(String jsonText) {
        if (jsonText == null || jsonText.isEmpty()) return "{\"text\":\"\"}";

        if ("\"\"".equals(jsonText) || "{\"\":\"\"}".equals(jsonText)) {
            return "{\"text\":\"\"}";
        }

        if (jsonText.startsWith("\"") && jsonText.endsWith("\"") && !jsonText.contains("{")) {
            String inner = jsonText.substring(1, jsonText.length() - 1);
            if (inner.isEmpty()) return "{\"text\":\"\"}";
            return "{\"text\":\"" + escapeJsonString(inner) + "\"}";
        }

        if (jsonText.startsWith("{")) {
            if (jsonText.contains("\"\":\"\"") && !jsonText.contains("\"text\"")) {
                return "{\"text\":\"\"}";
            }
            return jsonText;
        }

        return "{\"text\":\"" + escapeJsonString(jsonText) + "\"}";
    }

    /**
     * Escapes a string for use inside a JSON string value.
     */
    static String escapeJsonString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Converts modern snake_case NBT text component keys to 1.7.10 camelCase.
     */
    private static String convertKeyToCamelCase(String key) {
        if (key == null || key.isEmpty()) return key;
        switch (key) {
            case "click_event": return "clickEvent";
            case "hover_event": return "hoverEvent";
            case "has_glowing_text": return "hasGlowingText";
            default: return key;
        }
    }
}
