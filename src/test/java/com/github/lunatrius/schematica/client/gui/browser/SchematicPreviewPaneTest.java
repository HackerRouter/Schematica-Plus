package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.config.Configuration;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.util.UiState;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.relauncher.FMLInjectionData;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lwjgl.input.Keyboard;

import static org.junit.Assert.*;

public class SchematicPreviewPaneTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Object previousHome;
    private CommonProxy previousProxy;
    private Configuration previousConfiguration;
    private Object previousState;
    private Object previousLocale;
    private Object previousTranslations;
    private boolean previousPreview3D;
    private boolean previousReplacesImage;

    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Before public void setUp() throws Exception {
        previousHome = field(FMLInjectionData.class, "minecraftHome").get(null);
        field(FMLInjectionData.class, "minecraftHome").set(null, temporary.getRoot());
        previousProxy = SchematicaPlus.proxy;
        SchematicaPlus.proxy = new CommonProxy() {
            @Override public File getDataDirectory() { return temporary.getRoot(); }
            @Override public boolean isPlayerQuotaExceeded(EntityPlayer player) { return false; }
            @Override public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) { return temporary.getRoot(); }
        };
        previousConfiguration = ConfigurationHandler.configuration;
        previousState = field(UiState.class, "root").get(null);
        previousLocale = field(I18n.class, "i18nLocale").get(null);
        previousTranslations = field(StatCollector.class, "localizedName").get(null);
        previousPreview3D = ConfigurationHandler.schematicPreview3D;
        previousReplacesImage = ConfigurationHandler.schematicPreview3DReplacesImage;

        ConfigurationHandler.configuration = new Configuration(new File(temporary.getRoot(), "schematica_plus.cfg"));
        field(UiState.class, "root").set(null, null);

        ConfigurationHandler.schematicPreview3D = true;
        ConfigurationHandler.schematicPreview3DReplacesImage = true;

        Map<String, String> translations = new HashMap<>();
        for (String domain : new String[] {"schematica", "schematica_plus_litematica"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/" + domain + "/lang/en_US.lang")) {
                if (stream != null) translations.putAll(StringTranslate.parseLangFile(stream));
            }
        }
        Locale locale = new Locale();
        field(Locale.class, "field_135032_a").set(locale, translations);
        field(I18n.class, "i18nLocale").set(null, locale);
        StringTranslate translated = new StringTranslate();
        field(StringTranslate.class, "languageList").set(translated, translations);
        field(StatCollector.class, "localizedName").set(null, translated);
    }

    @After public void tearDown() throws Exception {
        ConfigurationHandler.configuration = previousConfiguration;
        field(UiState.class, "root").set(null, previousState);
        ConfigurationHandler.schematicPreview3D = previousPreview3D;
        ConfigurationHandler.schematicPreview3DReplacesImage = previousReplacesImage;
        field(I18n.class, "i18nLocale").set(null, previousLocale);
        field(StatCollector.class, "localizedName").set(null, previousTranslations);
        SchematicaPlus.proxy = previousProxy;
        field(FMLInjectionData.class, "minecraftHome").set(null, previousHome);
    }

    private SchematicPreviewPane createPane(AtomicLong clock, File file, boolean hasImage) throws Exception {
        UiPanel root = new UiPanel();
        root.setBounds(0, 0, 400, 300);
        UiInput input = new UiInput(root);
        SchematicPreviewPane pane = root.add(new SchematicPreviewPane(input, false, () -> {}, clock::get));
        pane.setBounds(10, 10, 120, 120);
        pane.layout(root.bounds());

        SchematicInfoCache cache = new SchematicInfoCache();
        field(SchematicInfoCache.class, "modelFile").set(cache, file);
        field(SchematicInfoCache.class, "model").set(cache, CompletableFuture.completedFuture(null));
        field(SchematicInfoCache.class, "preview3D").set(cache, new SchematicPreview3D(new Schematic(null, 2, 2, 2)));
        if (hasImage) {
            Constructor<SchematicFiles.Info> ctor = SchematicFiles.Info.class.getDeclaredConstructor(boolean.class);
            ctor.setAccessible(true);
            SchematicFiles.Info info = ctor.newInstance(false);
            info.volume = 8;
            @SuppressWarnings("unchecked")
            Map<File, Object> infos = (Map<File, Object>) field(SchematicInfoCache.class, "infos").get(cache);
            infos.put(file, CompletableFuture.completedFuture(info));
            @SuppressWarnings("unchecked")
            Map<File, ResourceLocation> previews = (Map<File, ResourceLocation>) field(SchematicInfoCache.class, "previews").get(cache);
            previews.put(file, new ResourceLocation("schematica:test_preview"));
        }

        pane.setContent(new SchematicPreviewPane.Content(file, 128, cache));
        return pane;
    }

    @Test public void tooltipWaitsTwoSecondsAfterMouseMoveScrollDragAndKeyboard() throws Exception {
        File file = temporary.newFile("sample.schematic");
        AtomicLong clock = new AtomicLong(-5_000_000_000L);
        SchematicPreviewPane pane = createPane(clock, file, false);
        SchematicPreviewPane.Viewport viewport = pane.viewport;

        assertTrue(viewport.tooltip(50, 50).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS - 1L);
        assertTrue(viewport.tooltip(50, 50).isEmpty());
        clock.addAndGet(1L);
        assertFalse(viewport.tooltip(50, 50).isEmpty());

        assertTrue(viewport.tooltip(51, 50).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS);
        assertFalse(viewport.tooltip(51, 50).isEmpty());

        assertTrue(viewport.keyTyped('w', Keyboard.KEY_W));
        assertTrue(viewport.tooltip(51, 50).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS - 1L);
        assertTrue(viewport.tooltip(51, 50).isEmpty());
        clock.addAndGet(1L);
        assertFalse(viewport.tooltip(51, 50).isEmpty());

        assertTrue(viewport.scroll(51, 50, 120));
        assertTrue(viewport.tooltip(51, 50).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS);
        assertFalse(viewport.tooltip(51, 50).isEmpty());

        assertTrue(viewport.mouseDown(51, 50, 0));
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS * 3L);
        assertTrue(viewport.tooltip(51, 50).isEmpty());
        viewport.mouseDrag(60, 55, 0);
        assertTrue(viewport.tooltip(60, 55).isEmpty());
        viewport.mouseUp(60, 55, 0);
        assertTrue(viewport.tooltip(60, 55).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS - 1L);
        assertTrue(viewport.tooltip(60, 55).isEmpty());
        clock.addAndGet(1L);
        assertFalse(viewport.tooltip(60, 55).isEmpty());
    }

    @Test public void tooltipStaysHiddenInImageModeAndWhenModelFailed() throws Exception {
        File file = temporary.newFile("image_mode.schematic");
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        SchematicPreviewPane pane = createPane(clock, file, true);
        SchematicPreviewPane.Viewport viewport = pane.viewport;

        pane.content().modelSelected = false;
        assertFalse(pane.content().showModel());
        assertTrue(viewport.tooltip(50, 50).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS * 2L);
        assertTrue(viewport.tooltip(50, 50).isEmpty());

        pane.content().modelSelected = true;
        assertTrue(viewport.tooltip(50, 50).isEmpty());
        clock.addAndGet(SchematicPreviewPane.TOOLTIP_BUFFER_NANOS);
        assertFalse(viewport.tooltip(50, 50).isEmpty());

        SchematicPreview3D model = pane.content().cache.preview3D(file);
        field(SchematicPreview3D.class, "failed").setBoolean(model, true);
        assertTrue(viewport.tooltip(50, 50).isEmpty());
    }

    @Test public void remembersPerSchematic2d3dPreferenceAcrossReloadRenameAndDelete() throws Exception {
        ConfigurationHandler.schematicPreview3DReplacesImage = false;
        File first = temporary.newFile("first.schematic");
        File second = temporary.newFile("second.schematic");
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        SchematicPreviewPane pane = createPane(clock, first, true);
        SchematicInfoCache cache = pane.content().cache;

        assertFalse(pane.content().showModel());
        UiButton mode = (UiButton) field(SchematicPreviewPane.class, "mode").get(pane);
        UiBounds bounds = mode.bounds();
        mode.mouseDown(bounds.x + 2, bounds.y + 2, 0);
        mode.mouseUp(bounds.x + 2, bounds.y + 2, 0);
        assertTrue(pane.content().showModel());
        assertEquals(Boolean.TRUE, UiState.previewModel(first));
        assertNull(UiState.previewModel(second));

        field(UiState.class, "root").set(null, null);
        assertEquals(Boolean.TRUE, UiState.previewModel(first));
        SchematicPreviewPane.Content reopenedFirst = new SchematicPreviewPane.Content(first, 128, cache);
        assertTrue(reopenedFirst.showModel());

        SchematicPreviewPane.Content freshSecond = new SchematicPreviewPane.Content(second, 128, cache);
        assertFalse(freshSecond.modelSelected);

        reopenedFirst.selectModel(false);
        assertFalse(reopenedFirst.showModel());
        SchematicPreviewPane expanded = new SchematicPreviewPane(new UiInput(new UiPanel()), true, () -> {}, clock::get);
        expanded.setContent(reopenedFirst);
        assertTrue(expanded.viewport.isFocusable());
        assertEquals(Boolean.FALSE, UiState.previewModel(first));
        assertFalse(reopenedFirst.showModel());

        File renamed = new File(temporary.getRoot(), "first_renamed.schematic");
        assertTrue(first.renameTo(renamed));
        UiState.renamed(first, renamed);
        assertNull(UiState.previewModel(first));
        assertEquals(Boolean.FALSE, UiState.previewModel(renamed));

        UiState.deleted(renamed);
        assertNull(UiState.previewModel(renamed));
    }
}
