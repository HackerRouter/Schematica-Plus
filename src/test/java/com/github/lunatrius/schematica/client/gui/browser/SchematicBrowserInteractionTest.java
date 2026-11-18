package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.config.Configuration;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.save.GuiAreaSelectionManager;
import com.github.lunatrius.schematica.client.util.UiState;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.CommonProxy;

import cpw.mods.fml.relauncher.FMLInjectionData;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class SchematicBrowserInteractionTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Configuration previousConfiguration;
    private Object previousLocale, previousState, previousHome, previousMinecraft, previousTranslations;
    private CommonProxy previousProxy;
    private Minecraft minecraft;
    private FontRenderer font;
    private File directory, schematic;

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
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
        previousLocale = field(I18n.class, "i18nLocale").get(null);
        previousState = field(UiState.class, "root").get(null);
        ConfigurationHandler.configuration = new Configuration(new File(temporary.getRoot(), "schematica_plus.cfg"));
        Map<String, String> translations = new HashMap<>();
        for (String domain : new String[] {"minecraft", "schematica", "schematica_plus_litematica"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/" + domain + "/lang/en_US.lang")) {
                if (stream != null) translations.putAll(StringTranslate.parseLangFile(stream));
            }
        }
        Locale locale = new Locale();
        field(Locale.class, "field_135032_a").set(locale, translations);
        field(I18n.class, "i18nLocale").set(null, locale);
        previousTranslations = field(StatCollector.class, "localizedName").get(null);
        StringTranslate translated = new StringTranslate();
        field(StringTranslate.class, "languageList").set(translated, translations);
        field(StatCollector.class, "localizedName").set(null, translated);
        field(UiState.class, "root").set(null, null);
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        font = (FontRenderer) unsafe.getMethod("allocateInstance", Class.class)
            .invoke(field(unsafe, "theUnsafe").get(null), FontRenderer.class);
        minecraft = (Minecraft) unsafe.getMethod("allocateInstance", Class.class)
            .invoke(field(unsafe, "theUnsafe").get(null), Minecraft.class);
        previousMinecraft = field(Minecraft.class, "theMinecraft").get(null);
        field(Minecraft.class, "theMinecraft").set(null, minecraft);
        int[] widths = new int[256];
        Arrays.fill(widths, 6);
        field(FontRenderer.class, "charWidth").set(font, widths);
        field(FontRenderer.class, "glyphWidth").set(font, new byte[65536]);
        directory = temporary.newFolder("schematics");
        schematic = new File(directory, "example.schematic");
        assertTrue(schematic.createNewFile());
    }

    @After public void tearDown() throws Exception {
        ConfigurationHandler.configuration = previousConfiguration;
        field(I18n.class, "i18nLocale").set(null, previousLocale);
        field(StatCollector.class, "localizedName").set(null, previousTranslations);
        field(UiState.class, "root").set(null, previousState);
        field(Minecraft.class, "theMinecraft").set(null, previousMinecraft);
        SchematicaPlus.proxy = previousProxy;
        field(FMLInjectionData.class, "minecraftHome").set(null, previousHome);
    }

    @Test public void infoStarReceivesClicksAndPersistsFavorites() throws Exception {
        Browser screen = new Browser();
        screen.open(854, 480);
        UiButton star = screen.button("schematica.ui.browser.star.hover");
        assertFalse(star.isVisible());
        screen.select(schematic);
        for (int[] size : new int[][] {{427, 240}, {854, 480}, {1280, 720}}) {
            screen.resize(size[0], size[1]);
            UiBounds bounds = star.bounds();
            for (int x : new int[] {bounds.x, bounds.right() - 1}) {
                for (int y : new int[] {bounds.y, bounds.bottom() - 1}) {
                    assertSame(star, screen.mouse.hit(x, y));
                }
            }
        }
        screen.click(star, 1);
        assertFalse(UiState.favorite(schematic));
        screen.click(star, 0);
        assertTrue(UiState.favorite(schematic));
        field(UiState.class, "root").set(null, null);
        assertTrue(UiState.favorite(schematic));
        screen.click(star, 0);
        assertFalse(UiState.favorite(schematic));
    }

    @Test public void renamedFilesKeepTheirFavoriteAndRecentEntries() throws Exception {
        UiState.toggleFavorite(schematic);
        UiState.addRecent(schematic);
        File renamed = new File(schematic.getParentFile(), "renamed-" + schematic.getName());
        assertTrue(schematic.renameTo(renamed));
        UiState.renamed(schematic, renamed);
        assertTrue(UiState.favorite(renamed));
        assertFalse(UiState.favorite(schematic));
        assertTrue(renamed.renameTo(schematic));
        UiState.renamed(renamed, schematic);
        assertTrue(UiState.favorite(schematic));
        UiState.deleted(schematic);
        assertFalse(UiState.favorite(schematic));
    }

    @Test public void removingFavoriteUpdatesTheCollectionImmediately() throws Exception {
        UiState.toggleFavorite(schematic);
        Browser screen = new Browser();
        screen.open(854, 480);
        screen.click(screen.button("schematica.ui.browser.favorites.hover"), 0);
        screen.select(schematic);
        UiButton star = screen.button("schematica.ui.browser.star.hover");
        screen.click(star, 0);
        assertFalse(UiState.favorite(schematic));
        assertTrue(screen.files.entries().isEmpty());
        assertFalse(star.isVisible());
        assertTrue(screen.browser.listing());
        screen.click(screen.button("schematica.ui.browser.favorites.hover"), 0);
        assertFalse(screen.browser.listing());
        assertEquals(schematic, screen.files.entries().get(0).file);
    }

    @Test public void togglingFavoriteInRecentKeepsTheRecentEntry() throws Exception {
        UiState.addRecent(schematic);
        Browser screen = new Browser();
        screen.open(854, 480);
        screen.click(screen.button("schematica.ui.browser.recent.hover"), 0);
        screen.select(schematic);
        screen.click(screen.button("schematica.ui.browser.star.hover"), 0);
        assertTrue(UiState.favorite(schematic));
        assertEquals(schematic, screen.files.entries().get(0).file);
        screen.click(screen.button("schematica.ui.browser.star.hover"), 0);
        assertFalse(UiState.favorite(schematic));
        assertEquals(schematic, screen.files.entries().get(0).file);
    }

    @Test public void emptyStatusDoesNotCoverTheInfoPanel() throws Exception {
        Browser screen = new Browser();
        screen.open(854, 480);
        SchematicInfoPanel info = null;
        for (UiWidget widget : screen.widgets().children()) if (widget instanceof SchematicInfoPanel) info = (SchematicInfoPanel) widget;
        assertNotNull(info);
        UiBounds bounds = info.bounds();
        int x = bounds.x + 20, y = bounds.bottom() - 15;
        assertSame(info, screen.mouse.hit(x, y));
        screen.setStatus("Loading schematic");
        assertNotSame(info, screen.mouse.hit(x, y));
        screen.setStatus("");
        assertSame(info, screen.mouse.hit(x, y));
    }

    @Test public void areaSearchOwnsTheWholeTextField() throws Exception {
        GuiAreaSelectionManager screen = new GuiAreaSelectionManager(null);
        UiInput mouse = prepare(screen, 854, 480);
        UiButton searchButton = (UiButton) field(GuiAreaSelectionManager.class, "searchButton").get(screen);
        UiTextField search = (UiTextField) field(GuiAreaSelectionManager.class, "search").get(screen);
        for (int i = 0; i < 2; i++) {
            click(mouse, searchButton, 0);
            UiBounds bounds = search.bounds();
            for (int x = bounds.x; x < bounds.right(); x++) assertSame(search, mouse.hit(x, bounds.y + 5));
            invoke(screen, "layoutWidgets");
            assertSame(search, mouse.hit(bounds.x + 5, bounds.y + 5));
            click(mouse, searchButton, 0);
            assertFalse(search.isVisible());
            assertTrue(((UiWidget) field(GuiAreaSelectionManager.class, "home").get(screen)).isVisible());
        }
    }

    @Test public void loadCheckboxDoesNotCoverTheInfoPanelAtSmallGuiSizes() throws Exception {
        GuiSchematicLoad screen = new GuiSchematicLoad(null);
        UiInput mouse = prepare(screen, 427, 240);
        UiWidget checkbox = (UiWidget) field(GuiSchematicLoad.class, "createPlacement").get(screen);
        SchematicInfoPanel info = (SchematicInfoPanel) field(GuiSchematicBrowser.class, "info").get(screen);
        int x = info.bounds().x + 30, y = info.bounds().bottom() - 1;
        assertFalse(checkbox.bounds().contains(x, y));
        assertSame(info, mouse.hit(x, y));
    }

    private UiInput prepare(UiScreen screen, int width, int height) throws Exception {
        field(GuiScreen.class, "fontRendererObj").set(screen, font);
        field(GuiScreen.class, "mc").set(screen, minecraft);
        screen.width = width;
        screen.height = height;
        UiPanel root = (UiPanel) field(UiScreen.class, "root").get(screen);
        root.setBounds(0, 0, width, height);
        invoke(screen, "createWidgets");
        invoke(screen, "layoutWidgets");
        return new UiInput(root);
    }

    private static void invoke(UiScreen screen, String name) throws Exception {
        java.lang.reflect.Method method = UiScreen.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(screen);
    }

    private static void click(UiInput mouse, UiWidget widget, int button) {
        UiBounds bounds = widget.bounds();
        int x = bounds.x + bounds.width / 2, y = bounds.y + bounds.height / 2;
        mouse.mouseDown(x, y, button);
        mouse.mouseUp(x, y, button);
    }

    private final class Browser extends GuiSchematicBrowser {
        private final UiInput mouse = new UiInput(root);

        Browser() { super(null, "Browser", false); }
        @Override protected boolean collections() { return true; }
        @Override protected void createActions() {}
        @Override protected SchematicBrowserModel createModel() throws java.io.IOException {
            return new SchematicBrowserModel(directory);
        }

        void open(int width, int height) {
            fontRendererObj = font;
            createWidgets();
            resize(width, height);
            opened();
        }

        void resize(int width, int height) {
            this.width = width;
            this.height = height;
            root.setBounds(0, 0, width, height);
            layoutWidgets();
        }

        void select(File file) {
            for (int i = 0; i < files.entries().size(); i++) {
                if (files.entries().get(i).file.equals(file)) {
                    files.select(i);
                    tickScreen();
                    return;
                }
            }
            throw new AssertionError("File not listed: " + file);
        }

        UiPanel widgets() { return root; }

        UiButton button(String tooltip) {
            for (UiWidget widget : root.children()) {
                if (widget instanceof UiButton && widget.tooltip(0, 0).contains(UiTranslations.format(tooltip).split("\n")[0])) return (UiButton) widget;
            }
            throw new AssertionError("Button not found: " + tooltip);
        }

        void click(UiButton button, int mouseButton) {
            UiBounds bounds = button.bounds();
            int x = bounds.x + bounds.width / 2, y = bounds.y + bounds.height / 2;
            mouse.mouseDown(x, y, mouseButton);
            mouse.mouseUp(x, y, mouseButton);
            tickScreen();
        }
    }
}
