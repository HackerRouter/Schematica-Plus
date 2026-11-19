package com.github.lunatrius.schematica.client.gui.browser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;

import com.github.lunatrius.schematica.client.gui.config.ColorPickerPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiWidget;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;
import com.github.lunatrius.schematica.world.schematic.SchematicMetadata;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class GuiSchematicPreviewEditorTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Object previousLocale;
    private Object previousTranslations;
    private Object previousMinecraft;
    private Minecraft minecraft;
    private FontRenderer font;

    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void invoke(UiScreen screen, String name) throws Exception {
        Method method = UiScreen.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(screen);
    }

    @Before public void setUp() throws Exception {
        previousLocale = field(I18n.class, "i18nLocale").get(null);
        previousTranslations = field(StatCollector.class, "localizedName").get(null);
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

        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Object unsafe = field(unsafeClass, "theUnsafe").get(null);
        Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
        font = (FontRenderer) allocate.invoke(unsafe, FontRenderer.class);
        minecraft = (Minecraft) allocate.invoke(unsafe, Minecraft.class);
        previousMinecraft = field(Minecraft.class, "theMinecraft").get(null);
        field(Minecraft.class, "theMinecraft").set(null, minecraft);
        int[] widths = new int[256];
        Arrays.fill(widths, 6);
        field(FontRenderer.class, "charWidth").set(font, widths);
        field(FontRenderer.class, "glyphWidth").set(font, new byte[65536]);
        minecraft.fontRenderer = font;
    }

    @After public void tearDown() throws Exception {
        field(I18n.class, "i18nLocale").set(null, previousLocale);
        field(StatCollector.class, "localizedName").set(null, previousTranslations);
        field(Minecraft.class, "theMinecraft").set(null, previousMinecraft);
    }

    private File writeMinimalSchematic(String name) throws Exception {
        File file = temporary.newFile(name);
        NBTTagCompound root = new NBTTagCompound();
        root.setShort("Width", (short) 1);
        root.setShort("Height", (short) 1);
        root.setShort("Length", (short) 1);
        root.setString("Materials", "Alpha");
        root.setByteArray("Blocks", new byte[] {0});
        root.setByteArray("Data", new byte[] {0});
        CompressedStreamTools.writeCompressed(root, java.nio.file.Files.newOutputStream(file.toPath()));
        return file;
    }

    private UiInput prepare(UiScreen screen, int width, int height) throws Exception {
        field(GuiScreen.class, "fontRendererObj").set(screen, font);
        field(GuiScreen.class, "mc").set(screen, minecraft);
        screen.width = width;
        screen.height = height;
        UiPanel root = (UiPanel) field(UiScreen.class, "root").get(screen);
        root.setBounds(0, 0, width, height);
        invoke(screen, "createWidgets");
        invoke(screen, "tickScreen");
        invoke(screen, "layoutWidgets");
        UiInput input = (UiInput) field(UiScreen.class, "input").get(screen);
        field(UiInput.class, "buttonSound").set(input, (Runnable) () -> {});
        return input;
    }

    private static void click(UiInput input, UiWidget widget, int button) {
        UiBounds bounds = widget.bounds();
        int x = bounds.x + bounds.width / 2;
        int y = bounds.y + bounds.height / 2;
        input.mouseDown(x, y, button);
        input.mouseUp(x, y, button);
    }

    @Test public void layoutsStayWithinBoundsWithoutOverlappingAcrossSizes() throws Exception {
        File schematic = writeMinimalSchematic("layout.schematic");
        GuiSchematicPreviewEditor screen = new GuiSchematicPreviewEditor(null, schematic, null);
        prepare(screen, 854, 480);
        UiPanel root = (UiPanel) field(UiScreen.class, "root").get(screen);

        for (int[] size : new int[][] {{427, 240}, {854, 480}, {1280, 720}}) {
            screen.width = size[0];
            screen.height = size[1];
            root.setBounds(0, 0, size[0], size[1]);
            invoke(screen, "layoutWidgets");
            List<UiWidget> widgets = root.children();
            for (int i = 0; i < widgets.size(); i++) {
                UiBounds a = widgets.get(i).bounds();
                if (a.isEmpty()) continue;
                assertTrue("widget out of screen bounds", a.x >= 0 && a.y >= 0 && a.right() <= size[0] && a.bottom() <= size[1]);
                for (int j = i + 1; j < widgets.size(); j++) {
                    UiBounds b = widgets.get(j).bounds();
                    if (b.isEmpty()) continue;
                    assertTrue("overlapping widgets at " + size[0] + "x" + size[1], a.intersect(b).isEmpty());
                }
            }
        }
    }

    @Test public void loadsEditsAndPersistsCustomPreviewIntoSchematicFile() throws Exception {
        File schematic = writeMinimalSchematic("edited.schematic");
        File imageFile = temporary.newFile("custom.png");
        BufferedImage source = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                source.setRGB(x, y, x < 32 ? 0xFFFF0000 : 0xFF0000FF);
            }
        }
        assertTrue(ImageIO.write(source, "png", imageFile));

        GuiSchematicPreviewEditor screen = new GuiSchematicPreviewEditor(null, schematic, null);
        UiInput input = prepare(screen, 854, 480);

        UiButton saveButton = (UiButton) field(GuiSchematicPreviewEditor.class, "saveButton").get(screen);
        assertFalse(saveButton.isEnabled());

        UiTextField pathField = (UiTextField) field(GuiSchematicPreviewEditor.class, "pathField").get(screen);
        pathField.setText("\"" + imageFile.getAbsolutePath() + "\"");
        click(input, (UiWidget) field(GuiSchematicPreviewEditor.class, "loadButton").get(screen), 0);
        invoke(screen, "tickScreen");

        assertTrue(screen.editor.hasImage());
        assertTrue(saveButton.isEnabled());

        click(input, (UiWidget) field(GuiSchematicPreviewEditor.class, "flipHButton").get(screen), 0);
        click(input, (UiWidget) field(GuiSchematicPreviewEditor.class, "rotateRightButton").get(screen), 0);
        assertEquals(90.0, screen.editor.rotationDegrees(), 0.001);

        UiIntegerField sizeField = (UiIntegerField) field(GuiSchematicPreviewEditor.class, "sizeField").get(screen);
        sizeField.setValue(64);
        UiIntegerField borderField = (UiIntegerField) field(GuiSchematicPreviewEditor.class, "borderField").get(screen);
        borderField.setValue(4);

        UiButton borderColorButton = (UiButton) field(GuiSchematicPreviewEditor.class, "borderColorButton").get(screen);
        click(input, borderColorButton, 0);
        assertEquals(1, input.modalPanels().size());
        assertTrue(input.modalPanels().get(0) instanceof ColorPickerPanel);
        assertTrue(screen.interceptMouse(0, 0, 0));
        assertTrue(input.modalPanels().isEmpty());

        screen.editor.setBorderColor(0xFF00FF00);
        assertTrue(screen.saveToFile());

        SchematicFiles.Info info = SchematicFiles.info(schematic);
        assertNotNull(info);
        assertNotNull(info.preview);
        assertTrue(SchematicMetadata.validPreview(info.preview));
        assertEquals(64 * 64, info.preview.length);
        assertEquals(0xFF00FF00, info.preview[0]);
        assertEquals(0xFF00FF00, info.preview[64 * 64 - 1]);
    }

    @Test public void managerExposesEditPreviewOptionInEditSelector() throws Exception {
        String[][] options = (String[][]) field(GuiSchematicManager.class, "OPTIONS").get(null);
        assertTrue(Arrays.asList(options[0]).contains("edit_type.edit_preview"));
        assertTrue(Arrays.asList(options[0]).contains("edit_type.set_preview"));
    }
}
