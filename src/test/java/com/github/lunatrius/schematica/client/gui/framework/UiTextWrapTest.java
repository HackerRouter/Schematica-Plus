// Dialog wrapping regression checks, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiTextWrapTest {
    @Test public void mixedTextUsesTheWholeLine() {
        assertEquals(Arrays.asList("开发和移植。欢迎提交 Pull Request。", "仓库中已包含详尽的项目说明。"),
            UiTextWrap.wrap("开发和移植。欢迎提交 Pull Request。仓库中已包含详尽的项目说明。", 24, String::length));
    }

    @Test public void nonbreakingPhraseMovesTogether() {
        assertEquals(Arrays.asList("欢迎提交", "Pull Request。", "谢谢！"),
            UiTextWrap.wrap("欢迎提交 Pull\u00a0Request。谢谢！", 13, String::length));
    }

    @Test public void paragraphsKeepExactlyOneEmptyLine() {
        assertEquals(Arrays.asList("中文 English", "", "第二段", ""),
            UiTextWrap.wrap("中文 English\n\n第二段\n", 20, String::length));
    }

    @Test public void longWordsAndSurrogatePairsAlwaysMakeProgress() {
        assertEquals(Arrays.asList("abcd", "efgh", "ij"), UiTextWrap.wrap("abcdefghij", 4, String::length));
        assertEquals(Arrays.asList("😀", "中"), UiTextWrap.wrap("😀中", 1, String::length));
    }

    @Test public void formattingSurvivesSoftAndHardBreaks() {
        List<String> lines = UiTextWrap.wrap("§a中文§l粗体中文\n继续§r原色§r", 4,
            value -> value.replaceAll("§.", "").length());
        assertEquals(Arrays.asList("§a中文§l粗体", "§a§l中文", "§a§l继续§r原色§r"), lines);
    }

    @Test public void englishKeepsWordsAndFitsMeasuredWidth() {
        List<String> lines = UiTextWrap.wrap("One wide word and another", 12, String::length);
        assertEquals(Arrays.asList("One wide", "word and", "another"), lines);
        for (String line : lines) assertTrue(line.length() <= 12);
    }
}
