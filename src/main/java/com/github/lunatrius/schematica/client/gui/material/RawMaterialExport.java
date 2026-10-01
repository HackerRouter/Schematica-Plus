// SPDX-License-Identifier: LGPL-3.0-only
// Litematica MaterialListJson / MaterialListJsonBase / MaterialListJsonEntry / MaterialListJsonCache, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The Raw Materials export: every material is taken apart through its first crafting recipe, or its first smelting
 * recipe when it has no crafting recipe, down to items without recipes or base materials like ingots. 1.7.10 has no
 * stonecutter, so the crafting-over-stonecutter choice of the original has nothing to choose between.
 */
public final class RawMaterialExport {
    private static final int MAX_DEPTH = 32;
    private static final String GATHER = "GATHER";

    /** One item and how it is made; a leaf (no recipe) is gathered. */
    public static final class Node {
        public final String item;
        public final int count;
        public final RecipeIndex.Recipe recipe;
        public final boolean multiple;
        public final List<Node> requirements = new ArrayList<>();

        Node(String item, int count, RecipeIndex.Recipe recipe, boolean multiple) {
            this.item = item;
            this.count = count;
            this.recipe = recipe;
            this.multiple = multiple;
        }

        public boolean leaf() { return recipe == null; }
    }

    /** A raw material: how many of it, the steps to make the results from it, and the results needing it. */
    public static final class Raw {
        public final String item;
        public final long total;
        public final List<Step> steps;
        public final Map<String, Long> results;

        Raw(String item, long total, List<Step> steps, Map<String, Long> results) {
            this.item = item;
            this.total = total;
            this.steps = steps;
            this.results = results;
        }
    }

    public static final class Step {
        public final String item;
        public final int count;
        public final RecipeIndex.Type type;
        public final String category;
        public final int recipeId;

        Step(Node node) {
            item = node.item;
            count = node.count;
            type = node.leaf() ? RecipeIndex.Type.UNKNOWN : node.recipe.type;
            category = node.leaf() ? GATHER : node.recipe.category();
            recipeId = node.leaf() ? -1 : node.recipe.id;
        }

        @Override public boolean equals(Object object) {
            if (!(object instanceof Step)) return false;
            Step other = (Step) object;
            return item.equals(other.item) && count == other.count && type == other.type && category.equals(other.category) && recipeId == other.recipeId;
        }

        @Override public int hashCode() { return item.hashCode() * 31 + count; }
    }

    /** The three reports of one export. */
    public static final class Result {
        public final List<Node> details = new ArrayList<>();
        public final List<Raw> flat = new ArrayList<>();
        public final List<Raw> combined = new ArrayList<>();
    }

    private RawMaterialExport() {}

    public static Node resolve(RecipeIndex index, String item, int count) {
        return resolve(index, item, count, new HashSet<String>(), 0);
    }

    private static Node resolve(RecipeIndex index, String item, int count, Set<String> ancestors, int depth) {
        List<RecipeIndex.Recipe> recipes = index.recipesFor(item);
        RecipeIndex.Recipe recipe = depth >= MAX_DEPTH || index.isBaseMaterial(item) ? null : choose(recipes, item);
        if (recipe == null) return new Node(item, count, null, false);
        int crafts = (int) Math.min(Integer.MAX_VALUE, ((long) count + recipe.outputCount - 1) / recipe.outputCount);
        Map<String, Integer> needs = new LinkedHashMap<>();
        for (List<String> slot : recipe.ingredients) {
            // Stop a looping state; ie Redstone Dust -> Redstone Block -> Redstone Dust
            for (String alternative : slot) if (alternative.equals(item) || ancestors.contains(alternative)) return new Node(item, count, null, false);
            needs.merge(slot.get(0), crafts, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
        }
        int matching = 0;
        for (RecipeIndex.Recipe other : recipes) if (other.crafting() == recipe.crafting()) matching++;
        Node node = new Node(item, count, recipe, matching > 1);
        ancestors.add(item);
        for (Map.Entry<String, Integer> need : needs.entrySet()) node.requirements.add(resolve(index, need.getKey(), need.getValue(), ancestors, depth + 1));
        ancestors.remove(item);
        return node;
    }

    /** The first crafting recipe, else the first smelting recipe; re-coloring recipes that take the item itself are skipped. */
    private static RecipeIndex.Recipe choose(List<RecipeIndex.Recipe> recipes, String item) {
        RecipeIndex.Recipe furnace = null;
        for (RecipeIndex.Recipe recipe : recipes) {
            boolean self = false;
            for (List<String> slot : recipe.ingredients) self |= slot.get(0).equals(item);
            if (self) continue;
            if (recipe.crafting()) return recipe;
            if (furnace == null && recipe.type == RecipeIndex.Type.FURNACE) furnace = recipe;
        }
        return furnace;
    }

    public static Result export(RecipeIndex index, Map<String, Integer> materials) {
        Result result = new Result();
        List<Step> previous = null;
        Map<String, Raw> combined = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> material : materials.entrySet()) {
            Node root = resolve(index, material.getKey(), material.getValue());
            result.details.add(root);
            List<Step> steps = new ArrayList<>();
            List<Node> leaves = new ArrayList<>();
            collect(root, steps, leaves);
            Map<String, Long> results = new LinkedHashMap<>();
            results.put(root.item, (long) root.count);
            boolean first = true;
            for (Node leaf : leaves) {
                boolean carry = first && (previous == null || !sameSteps(previous, steps));
                result.flat.add(new Raw(leaf.item, leaf.count, carry ? steps : new ArrayList<Step>(), results));
                first = false;
                Raw old = combined.get(leaf.item);
                Map<String, Long> merged = new LinkedHashMap<>(old == null ? new LinkedHashMap<String, Long>() : old.results);
                merged.merge(root.item, (long) root.count, Long::sum);
                combined.put(leaf.item, new Raw(leaf.item, (old == null ? 0 : old.total) + leaf.count, new ArrayList<Step>(), merged));
            }
            previous = steps;
        }
        Map<String, Raw> repacked = new LinkedHashMap<>();
        for (Raw raw : combined.values()) for (Raw part : repack(index, raw)) {
            Raw old = repacked.get(part.item);
            if (old == null) { repacked.put(part.item, part); continue; }
            Map<String, Long> merged = new LinkedHashMap<>(part.results);
            for (Map.Entry<String, Long> entry : old.results.entrySet()) merged.putIfAbsent(entry.getKey(), entry.getValue());
            repacked.put(part.item, new Raw(part.item, old.total + part.total, part.steps, merged));
        }
        result.combined.addAll(repacked.values());
        return result;
    }

    /** Steps are listed last-made first, as the original builds them. */
    private static void collect(Node node, List<Step> steps, List<Node> leaves) {
        steps.add(0, new Step(node));
        if (node.leaf()) leaves.add(node);
        for (Node requirement : node.requirements) collect(requirement, steps, leaves);
    }

    private static boolean sameSteps(List<Step> left, List<Step> right) {
        return left.size() == right.size() && left.containsAll(right) && right.containsAll(left);
    }

    /** Packs base materials into their blocks plus a remainder; nuggets into ingots first. */
    private static List<Raw> repack(RecipeIndex index, Raw raw) {
        List<Raw> parts = new ArrayList<>();
        String item = raw.item;
        long total = raw.total;
        Set<String> seen = new HashSet<>();
        RecipeIndex.Packing packing;
        while ((packing = index.packing(item)) != null && seen.add(item) && total >= packing.ratio) {
            long remainder = total % packing.ratio;
            if (remainder > 0) parts.add(0, new Raw(item, remainder, raw.steps, raw.results));
            item = packing.target;
            total /= packing.ratio;
        }
        if (total > 0) parts.add(0, new Raw(item, total, raw.steps, raw.results));
        return parts;
    }

    public static JsonElement detailsJson(Result result) {
        JsonArray array = new JsonArray();
        for (Node node : result.details) array.add(baseJson(node));
        return array;
    }

    private static JsonObject baseJson(Node node) {
        JsonObject object = new JsonObject();
        object.addProperty("Item", node.item);
        object.addProperty("Count", node.count);
        String key = node.leaf() ? "RemainingMaterials" : node.recipe.crafting() ? "CraftingMaterials" : "FurnaceMaterials";
        object.add(key, entryJson(node));
        return object;
    }

    private static JsonObject entryJson(Node node) {
        JsonObject object = new JsonObject();
        object.addProperty("Item", node.item);
        object.addProperty("Count", node.count);
        object.addProperty("Type", node.leaf() ? "EMPTY" : node.multiple ? "MULTI" : "ONE");
        if (node.leaf()) return object;
        object.addProperty("PrimaryId", node.recipe.id);
        JsonObject recipe = new JsonObject();
        recipe.addProperty("NetworkId", node.recipe.id);
        recipe.addProperty("Category", node.recipe.category());
        recipe.addProperty("Type", node.recipe.type.name());
        JsonArray ingredients = new JsonArray();
        for (List<String> slot : node.recipe.ingredients) {
            JsonArray alternatives = new JsonArray();
            for (String alternative : slot) alternatives.add(new com.google.gson.JsonPrimitive(alternative));
            ingredients.add(alternatives);
        }
        recipe.add("Ingredients", ingredients);
        JsonArray requirements = new JsonArray();
        for (Node requirement : node.requirements) requirements.add(baseJson(requirement));
        recipe.add("Requirements", requirements);
        JsonArray recipes = new JsonArray();
        recipes.add(recipe);
        object.add("Recipes", recipes);
        return object;
    }

    public static JsonElement rawJson(List<Raw> raws) {
        JsonArray array = new JsonArray();
        for (Raw raw : raws) {
            JsonObject object = new JsonObject();
            object.addProperty("RawItem", raw.item);
            object.addProperty("TotalEstimate", raw.total);
            JsonArray steps = new JsonArray();
            for (Step step : raw.steps) {
                JsonObject json = new JsonObject();
                json.addProperty("StepItem", step.item);
                json.addProperty("StepCount", step.count);
                json.addProperty("RecipeType", step.type.name());
                json.addProperty("RecipeCategory", step.category);
                json.addProperty("RecipeId", step.recipeId);
                steps.add(json);
            }
            object.add("Steps", steps);
            JsonArray results = new JsonArray();
            for (Map.Entry<String, Long> entry : raw.results.entrySet()) {
                JsonObject json = new JsonObject();
                json.addProperty("ResultItem", entry.getKey());
                json.addProperty("ResultTotal", entry.getValue());
                results.add(json);
            }
            object.add("Results", results);
            array.add(object);
        }
        return array;
    }

    /**
     * Writes raw_material_list_recipe_details (when enabled), raw_material_list_recipe_steps and raw_material_list_simplified;
     * returns the written files, or nothing when there are no materials. Counts are the total counts, as in the original.
     */
    public static List<Path> write(Path directory, MaterialListModel<MaterialItemKey> model, boolean missingOnly, boolean craftingOnly,
        boolean details, RecipeIndex index) throws IOException {
        Map<String, Integer> materials = new LinkedHashMap<>();
        for (MaterialListModel.Entry<MaterialItemKey> entry : missingOnly ? model.missingOnly() : model.entries()) {
            materials.merge(RecipeIndex.id(entry.key.stack()), entry.total, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
        }
        List<Path> files = new ArrayList<>();
        if (materials.isEmpty()) return files;
        Result result = export(index, materials);
        String suffix = (missingOnly ? "_missing_only" : "") + "_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        Files.createDirectories(directory);
        if (details) files.add(write(directory.resolve("raw_material_list_recipe_details" + suffix + ".json"), detailsJson(result)));
        files.add(write(directory.resolve("raw_material_list_recipe_steps" + suffix + ".json"), rawJson(result.flat)));
        files.add(write(directory.resolve("raw_material_list_simplified" + suffix + ".json"), rawJson(result.combined)));
        return files;
    }

    private static Path write(Path file, JsonElement json) throws IOException {
        Files.write(file, new GsonBuilder().setPrettyPrinting().create().toJson(json).getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
