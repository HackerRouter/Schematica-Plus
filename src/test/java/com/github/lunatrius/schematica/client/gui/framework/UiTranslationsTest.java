package com.github.lunatrius.schematica.client.gui.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringTranslate;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiTranslationsTest {

    private static final String DOMAIN = "schematica_plus_litematica";
    private static final String[] LANGUAGES = {"en_US", "es_ES", "fr_FR", "it_IT", "ja_JP", "ko_KR", "lzh", "nl_NL",
        "ru_RU", "sv_SE", "tr_TR", "uk_UA", "zh_CN", "zh_TW"};

    private InputStream language(String language) throws IOException {
        InputStream stream = getClass().getResourceAsStream("/assets/" + DOMAIN + "/lang/" + language + ".lang");
        if (stream == null) throw new IOException("Missing locale: " + language);
        return stream;
    }

    private Map<String, String> translations(String language) throws IOException {
        try (InputStream stream = language(language)) { return StringTranslate.parseLangFile(stream); }
    }

    private Map<String, String> catalog(String language) throws IOException {
        Map<String, String> result = translations(language);
        try (InputStream stream = getClass().getResourceAsStream("/assets/schematica/lang/" + language + ".lang")) {
            if (stream != null) result.putAll(StringTranslate.parseLangFile(stream));
        }
        return result;
    }

    private Set<String> referencedKeys() throws IOException {
        Set<String> keys = new java.util.TreeSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"((?:schematica|Schematica|litematica|malilib)\\.[^\"\\s]+)\"");
        try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(java.nio.file.Paths.get("src/main/java"))) {
            for (java.nio.file.Path path : paths.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String source = new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
                java.util.regex.Matcher matcher = pattern.matcher(source);
                while (matcher.find()) keys.add(matcher.group(1));
            }
        }
        keys.removeIf(key -> key.endsWith(".") || key.endsWith(".cfg"));
        keys.removeAll(Arrays.asList("schematica.config", "schematica.gui.material", "litematica.gui.label.area_editor.corner_"));
        return keys;
    }

    @Test public void activeTextKeysExistInEnglishAndChinese() throws IOException {
        Set<String> keys = referencedKeys();
        assertTrue(keys.size() > 250);
        for (String language : Arrays.asList("en_US", "zh_CN")) {
            Map<String, String> values = catalog(language);
            for (String key : keys) assertTrue(language + ": " + key, values.containsKey(key));
        }
    }

    @Test public void activeTemplatesFormatAcrossAllBundledLanguages() throws IOException {
        Map<String, String> english = catalog("en_US");
        Set<String> keys = referencedKeys();
        java.util.regex.Pattern argument = java.util.regex.Pattern.compile("%(?:(\\d+)\\$)?s");
        for (String language : LANGUAGES) {
            Map<String, String> values = new java.util.HashMap<>(english);
            values.putAll(catalog(language));
            for (String key : keys) {
                if (!english.containsKey(key)) continue;
                java.util.regex.Matcher matcher = argument.matcher(english.get(key));
                int count = 0, sequential = 0;
                while (matcher.find()) count = Math.max(count, matcher.group(1) == null ? ++sequential : Integer.parseInt(matcher.group(1)));
                Object[] arguments = new Object[count];
                Arrays.fill(arguments, "D:\\new\\test.schemplus");
                String text = UiTranslations.formatTemplate(values.get(key), arguments);
                assertFalse(language + ": " + key + " -> " + text, text.startsWith("Format error:"));
            }
        }
    }

    @Test public void shipsAllUpstreamKeysAndLocalesIncludingNonUiKeys() throws IOException {
        Map<String, String> english = translations("en_US");
        assertEquals(1644, english.size());
        assertTrue(english.containsKey("block.litematica.black_glass_fallback"));
        assertTrue(english.containsKey("modmenu.descriptionTranslation.litematica"));
        assertTrue(english.containsKey("tag.block.malilib.all_signs_fix"));
        for (String language : LANGUAGES) {
            Map<String, String> values = translations(language);
            Set<String> expected = english.keySet();
            if (language.equals("tr_TR")) {
                expected = expected.stream().filter(key -> key.contains("litematica")).collect(Collectors.toSet());
            }
            assertTrue(language, values.keySet().containsAll(expected));
            assertEquals(language, language.equals("tr_TR") ? 1181 : 1644, values.size());
            for (String text : values.values()) assertFalse(language, text.contains("\uFFFD"));
        }
    }

    @Test public void nativeLanguageLoaderUsesSelectedLocaleAndEnglishFallback() {
        net.minecraft.client.resources.Locale locale = new net.minecraft.client.resources.Locale();
        IResourceManager resources = new IResourceManager() {
            @Override public Set<String> getResourceDomains() { return Collections.singleton(DOMAIN); }
            @Override public IResource getResource(ResourceLocation location) throws IOException {
                final InputStream stream = getClass().getResourceAsStream("/assets/" + DOMAIN + "/" + location.getResourcePath());
                if (stream == null) throw new IOException(location.toString());
                return new IResource() {
                    @Override public InputStream getInputStream() { return stream; }
                    @Override public boolean hasMetadata() { return false; }
                    @Override public IMetadataSection getMetadata(String section) { return null; }
                };
            }
            @Override public List<IResource> getAllResources(ResourceLocation location) throws IOException {
                return Collections.singletonList(getResource(location));
            }
        };
        locale.loadLocaleDataFiles(resources, Arrays.asList("en_US", "zh_CN"));
        assertEquals("加载原理图", locale.formatMessage("litematica.gui.title.load_schematic", new Object[0]));
        locale.loadLocaleDataFiles(resources, Arrays.asList("en_US", "tr_TR"));
        assertEquals("Reset", locale.formatMessage("malilib.gui.button.reset", new Object[0]));
        locale.loadLocaleDataFiles(resources, Arrays.asList("en_US", "missing"));
        assertEquals("Load Schematic", locale.formatMessage("litematica.gui.title.load_schematic", new Object[0]));
    }

    @Test public void decodesMultilineTemplateBeforeInsertingUserPaths() throws IOException {
        String path = "D:\\new\\test\\schematic.schemplus";
        String text = translations("en_US").get("malilib.message.error.failed_to_create_file");
        assertEquals("Failed to create file\n  \u00a7e'" + path + "'\u00a7r", UiTranslations.formatTemplate(text, path));
        assertEquals("second\nfirst", UiTranslations.formatTemplate("%2$s\\n%1$s", "first", "second"));
        assertEquals("a\nb\tc\rd\\n", UiTranslations.formatTemplate("a\\nb\\tc\\rd\\\\n"));
        assertEquals("100%", UiTranslations.formatTemplate("100%"));
    }

    @Test public void retainsLiteralEscapesFromUpstreamAndHandlesInvalidArguments() throws IOException {
        String text = UiTranslations.formatTemplate(translations("tr_TR").get("litematica.config.generic.comment.commandUseStrict"));
        assertTrue(text.contains("\\n"));
        assertFalse(text.contains("\n"));
        assertEquals("Format error: %2$s", UiTranslations.formatTemplate("%2$s", "only one"));
        assertEquals("unknown.key", UiTranslations.formatTemplate("unknown.key"));
    }
}
