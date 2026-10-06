package com.asdflj.wtct.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The pattern mapping table (样板映射), ported from ExtendedAE Plus' {@code RecipeTypeNameConfig}.
 *
 * <p>
 * Plus stores a flat JSON object in {@code config/<mod>/recipe_type_names.json}:
 *
 * <ul>
 * <li>a key containing {@code ":"} is a full recipe-type id ({@code gtceu:assembler}), matched
 * exactly;
 * <li>a key without one is an alias (case-insensitive) - the keyword that ends up being typed into
 * the provider search box.
 * </ul>
 *
 * The value on either side is the provider search term to use instead. WCWT only ships the UI for
 * this table; this port keeps the same file format and helpers so a table written by Plus keeps
 * working, and adds a manager screen of its own.
 */
public final class PatternMappingStore {

    public record RecipeTypeMapping(String key, String value) {}

    private static final String CONFIG_DIR = "wtct";
    private static final String FILE_NAME = "recipe_type_names.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    /** The key a crafting-table recipe is recorded under - Plus' {@code DEFAULT_CRAFTING_SEARCH_KEY}. */
    public static final String CRAFTING_KEY = "crafting";
    /**
     * The spellings that all mean "this pattern came from a crafting table". Their provider is the
     * molecular assembler the interface sits next to, which is what {@link #molecularAssemblerName()}
     * hands back.
     */
    private static final List<String> CRAFTING_KEYS = List.of(
        CRAFTING_KEY,
        "minecraft:crafting",
        "minecraft:crafting_shaped",
        "minecraft:crafting_shapeless",
        "crafting_shaped",
        "crafting_shapeless",
        "shaped",
        "shapeless");
    /** The molecular assembler's unlocalised name - the term whose translation the provider carries. */
    private static final String MOLECULAR_ASSEMBLER_KEY = "tile.appliedenergistics2.BlockMolecularAssembler";
    /**
     * NEI names a shaped / shapeless workbench recipe with these two lang keys. An older build wrote
     * that localised name into the table as the mapping key, which is why the manager listed
     * "无序合成 → 分子装配" - text pointing at text, under a key nothing can ever be typed against
     * again. Both stand for a crafting-table recipe, so {@link #migrateLegacyCraftingKeys()} folds
     * them onto {@link #CRAFTING_KEY} the next time the file is read.
     */
    private static final String[] LEGACY_CRAFTING_NAME_KEYS = { "nei.recipe.shaped", "nei.recipe.shapeless" };
    /**
     * The provider search word an older build wrote for a crafting row - the half name the molecular
     * assembler's own name used to be shortened to. Only these known-bad values are ever corrected: the
     * rewrite used to run whenever a crafting row's value differed from the assembler's name, which
     * silently put back the old name every time the table was read and made a mapping's Chinese name
     * impossible to change.
     */
    private static final List<String> LEGACY_CRAFTING_VALUES = List.of("分子装配", "分子装配机");

    /**
     * The table a fresh install starts from - the recipe types a GTNH player is most likely to encode
     * against, with the provider search term each one should resolve to. ExtendedAE Plus calls these
     * "配方类型 ID / 别名 → 供应器搜索词"; the alias row ({@code assembler}) and the full id row
     * ({@code gtceu:assembler}) are both listed so either spelling finds the entry.
     */
    private static final Map<String, String> DEFAULT_MAPPINGS = new LinkedHashMap<>();
    /** Full recipe-type ids, keyed by {@code namespace:path}. */
    private static final Map<String, String> CUSTOM_NAMES = new LinkedHashMap<>();
    /** Aliases, keyed lower-case. */
    private static final Map<String, String> CUSTOM_ALIASES = new LinkedHashMap<>();
    private static boolean loaded;

    static {
        // Order matters only for readability - the manager sorts by key.
        DEFAULT_MAPPINGS.put("assembler", "组装机");
        DEFAULT_MAPPINGS.put(CRAFTING_KEY, molecularAssemblerName());
        DEFAULT_MAPPINGS.put("gtceu:arc_furnace", "电弧炉");
        DEFAULT_MAPPINGS.put("gtceu:assembler", "组装机");
        DEFAULT_MAPPINGS.put("gtceu:chemical_reactor", "化学反应器");
        DEFAULT_MAPPINGS.put("minecraft:blasting", "高炉");
        DEFAULT_MAPPINGS.put("minecraft:campfire_cooking", "营火");
        DEFAULT_MAPPINGS.put("minecraft:smelting", "熔炉");
        DEFAULT_MAPPINGS.put("minecraft:smoking", "烟熏");
    }

    private PatternMappingStore() {}

    /**
     * Seeds the built-in table when the config has nothing in it - a fresh install gets a usable
     * mapping instead of an empty screen, and the file is written immediately so it is editable.
     * Once the table is non-empty the file is the only source, so deleting an entry sticks.
     */
    private static void seedDefaultsIfEmpty() {
        if (!CUSTOM_NAMES.isEmpty() || !CUSTOM_ALIASES.isEmpty()) {
            return;
        }
        DEFAULT_MAPPINGS.forEach(PatternMappingStore::put);
        write();
    }

    // ---------------------------------------------------------------- file io

    private static File file() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDataDir == null) {
            return null;
        }
        return new File(new File(new File(mc.mcDataDir, "config"), CONFIG_DIR), FILE_NAME);
    }

    public static synchronized void load() {
        CUSTOM_NAMES.clear();
        CUSTOM_ALIASES.clear();
        loaded = true;
        final File f = file();
        if (f == null || !f.isFile()) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            final JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            for (final Map.Entry<String, JsonElement> e : parsed.getAsJsonObject()
                .entrySet()) {
                final String key = e.getKey();
                final JsonElement value = e.getValue();
                if (key == null || value == null || !value.isJsonPrimitive()) {
                    continue;
                }
                final String text = value.getAsString();
                if (text == null || text.isBlank()) {
                    continue;
                }
                put(key, text);
            }
        } catch (final Exception ignored) {
            // A broken file only costs the mapping table - never the game.
        }
        migrateLegacyCraftingKeys();
        sanitize();
        seedDefaultsIfEmpty();
    }

    /**
     * Cleans up entries that can never do anything, the way ExtendedAE Plus' {@code
     * removeMappingsByCnValue} does for the rows its own manager left behind:
     *
     * <ul>
     * <li>a row whose key is a localised display name and whose value is a recipe-type id is the
     * inverted pair an older build wrote ("分子装配 → au") - the lookup only ever asks with ids, so it
     * can never match;</li>
     * <li>the crafting row's own search word, when it still holds one of the old half names, is
     * corrected to the molecular assembler's name the provider actually carries.</li>
     * </ul>
     *
     * <p>
     * Only those known-dead shapes are touched: a row the player authored - including one whose key is
     * Chinese - is theirs, and rewriting rows on every read is what made an edited entry look like it
     * had been deleted the next time the manager was opened.
     *
     * <p>
     * Written back immediately when anything changed, so the fix survives the session.
     */
    private static void sanitize() {
        boolean changed = false;
        // A row whose key is a localised display name *and* whose value is a recipe-type id is the
        // inverted pair an older build wrote ("分子装配 → au"): the lookup only ever asks with ids
        // ({@code crafting}, {@code gtceu:assembler}), so such a row can never match and is what the
        // manager used to list as junk. A Chinese key with a Chinese value is a pair the player typed
        // (a recipe type they know by name, bound to the provider word to search with) and is kept -
        // dropping every non-ASCII key is what made an edited row vanish from the manager.
        final List<String> deadAliases = new ArrayList<>();
        for (final Map.Entry<String, String> e : CUSTOM_ALIASES.entrySet()) {
            if (!isAscii(e.getKey()) && isAscii(e.getValue())) {
                deadAliases.add(e.getKey());
            }
        }
        for (final String key : deadAliases) {
            CUSTOM_ALIASES.remove(key);
            changed = true;
        }
        // One crafting row, pointing at the assembler's real name - but only the known stale spellings
        // get corrected. A name the player typed is theirs, and rewriting it on every read is what made
        // "change the Chinese name of a mapping" impossible: the edit stuck in the file and was undone
        // in memory the next time the table was loaded.
        final String assembler = molecularAssemblerName();
        final String craftingOwnRow = CUSTOM_ALIASES.get(CRAFTING_KEY);
        if (craftingOwnRow == null || craftingOwnRow.isBlank() || LEGACY_CRAFTING_VALUES.contains(craftingOwnRow)) {
            if (craftingOwnRow != null) {
                CUSTOM_ALIASES.put(CRAFTING_KEY, assembler);
                changed = true;
            }
        }
        for (final String key : CRAFTING_KEYS) {
            final String value = CUSTOM_ALIASES.get(key);
            if (value == null || !LEGACY_CRAFTING_VALUES.contains(value)) {
                continue;
            }
            CUSTOM_ALIASES.put(key, assembler);
            changed = true;
        }
        if (changed) {
            write();
        }
    }

    private static boolean isAscii(final String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    /**
     * The provider search term a crafting pattern resolves to: the molecular assembler's own name, so
     * the pattern lands on the interface the player put next to one. Read from the language file the
     * providers are named out of, with the Chinese name as the fallback for the times the table is
     * asked before the language file is up.
     */
    public static String molecularAssemblerName() {
        final String langKey = MOLECULAR_ASSEMBLER_KEY + ".name";
        final String name = net.minecraft.util.StatCollector.translateToLocal(langKey);
        return name == null || name.isEmpty() || name.equals(langKey) ? "分子装配室" : name;
    }

    /**
     * Folds the {@link #LEGACY_CRAFTING_NAME_KEYS} entries - localised recipe names an older build
     * stored as keys - onto {@link #CRAFTING_KEY}. An entry the player has since made under the real
     * key is left alone.
     */
    private static void migrateLegacyCraftingKeys() {
        boolean changed = false;
        for (final String langKey : LEGACY_CRAFTING_NAME_KEYS) {
            final String localized = net.minecraft.util.StatCollector.translateToLocal(langKey);
            if (localized == null || localized.isEmpty() || localized.equals(langKey)) {
                continue;
            }
            if (CUSTOM_ALIASES.remove(localized.toLowerCase(Locale.ROOT)) == null) {
                continue;
            }
            // Both spellings stand for a crafting-table recipe, whose provider is the molecular
            // assembler - the folded entry gets the assembler's own name rather than the half-filled
            // search word the old build happened to record.
            CUSTOM_ALIASES.putIfAbsent(CRAFTING_KEY, molecularAssemblerName());
            changed = true;
        }
        if (changed) {
            write();
        }
    }

    private static void put(final String key, final String value) {
        if (key.contains(":")) {
            CUSTOM_NAMES.put(key, value);
        } else {
            CUSTOM_ALIASES.put(key.toLowerCase(Locale.ROOT), value);
        }
    }

    private static void putAndForget(final String key, final String value) {
        // A key can only live in one of the maps - drop it from the other first.
        CUSTOM_NAMES.remove(key);
        CUSTOM_ALIASES.remove(key.toLowerCase(Locale.ROOT));
        put(key, value);
    }

    private static void write() {
        final File f = file();
        if (f == null) {
            return;
        }
        final JsonObject root = new JsonObject();
        // Full ids first so the file stays readable, then the aliases.
        CUSTOM_NAMES.forEach(root::addProperty);
        CUSTOM_ALIASES.forEach(root::addProperty);
        try {
            final File parent = f.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (Writer writer = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (final IOException ignored) {
            // Same as above: a failed write must not break the GUI.
        }
    }

    // ------------------------------------------------------------------ reads

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    /** Snapshot of the whole table, sorted by key - what the manager screen lists. */
    public static synchronized List<RecipeTypeMapping> getRecipeTypeMappings() {
        ensureLoaded();
        final List<RecipeTypeMapping> out = new ArrayList<>();
        CUSTOM_NAMES.forEach((key, value) -> out.add(new RecipeTypeMapping(key, value)));
        CUSTOM_ALIASES.forEach((key, value) -> out.add(new RecipeTypeMapping(key, value)));
        out.sort(Comparator.comparing(RecipeTypeMapping::key, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    /** Kept for the terminal's own table view. */
    public static synchronized List<RecipeTypeMapping> all() {
        return getRecipeTypeMappings();
    }

    /** Plus' {@code resolveSearchKeyAlias}: an alias wins, otherwise the input goes through. */
    public static synchronized String resolveSearchKeyAlias(final String rawKey) {
        if (rawKey == null) {
            return null;
        }
        final String normalized = rawKey.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        ensureLoaded();
        final String alias = CUSTOM_ALIASES.get(normalized.toLowerCase(Locale.ROOT));
        return alias != null && !alias.isBlank() ? alias : normalized;
    }

    /** Plus' {@code resolveProviderSearchKey}: alias first, then the recipe-type id table. */
    public static synchronized String resolveProviderSearchKey(final String rawKey) {
        final String normalized = rawKey == null ? "" : rawKey.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        final String alias = resolveSearchKeyAlias(normalized);
        if (alias != null && !normalized.equals(alias)) {
            return alias;
        }
        if (normalized.contains(":")) {
            final String mapped = CUSTOM_NAMES.get(normalized);
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }
        // Every crafting-table spelling lands on the molecular assembler even when the table has no
        // entry for it, which is the case for anyone whose file predates this build: the workbench
        // overlay reports "crafting", and typing it has to find the assembler like everything else.
        if (isCraftingKey(normalized)) {
            return molecularAssemblerName();
        }
        return normalized;
    }

    /** True for the spellings that all mean "a crafting-table recipe"; see {@link #CRAFTING_KEYS}. */
    public static boolean isCraftingKey(final String key) {
        return CRAFTING_KEYS.contains(
            key.trim()
                .toLowerCase(Locale.ROOT));
    }

    /**
     * Provider search term bound to the given key, or "" when nothing matches. Exact hit first (id
     * or alias), then a containing match in either direction - the terminal's upload uses this to
     * pick the provider from whatever the user typed.
     */
    public static synchronized String providerFor(final String source) {
        if (source == null || source.trim()
            .isEmpty()) {
            return "";
        }
        ensureLoaded();
        final String needle = source.trim();
        final String direct = resolveProviderSearchKey(needle);
        if (!direct.isEmpty() && !direct.equals(needle)) {
            return direct;
        }
        for (final RecipeTypeMapping e : getRecipeTypeMappings()) {
            if (e.key()
                .equalsIgnoreCase(needle)) {
                return e.value();
            }
        }
        for (final RecipeTypeMapping e : getRecipeTypeMappings()) {
            if (needle.contains(e.key()) || e.key()
                .contains(needle)) {
                return e.value();
            }
        }
        return "";
    }

    // ----------------------------------------------------------------- writes

    /** Plus' {@code addOrUpdateAliasMapping}: writes one key and persists the file. */
    public static synchronized boolean addOrUpdateAliasMapping(final String mappingKey, final String value) {
        if (mappingKey == null || value == null
            || mappingKey.trim()
                .isEmpty()
            || value.trim()
                .isEmpty()) {
            return false;
        }
        ensureLoaded();
        putAndForget(mappingKey.trim(), value.trim());
        write();
        return true;
    }

    /** Alias kept for the terminal's own mapping field. */
    public static synchronized boolean add(final String mappingKey, final String value) {
        return addOrUpdateAliasMapping(mappingKey, value);
    }

    /** Plus' {@code removeRecipeTypeMapping}: drops one key (case-insensitive for aliases). */
    public static synchronized boolean removeRecipeTypeMapping(final String mappingKey) {
        if (mappingKey == null || mappingKey.trim()
            .isEmpty()) {
            return false;
        }
        ensureLoaded();
        final String requested = mappingKey.trim();
        boolean removed = CUSTOM_NAMES.remove(requested) != null;
        removed |= CUSTOM_ALIASES.remove(requested.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            write();
        }
        return removed;
    }

    /** Plus' {@code removeMappingsByCnValue}: drops every entry whose value matches exactly. */
    public static synchronized int removeMappingsByCnValue(final String value) {
        if (value == null || value.trim()
            .isEmpty()) {
            return 0;
        }
        ensureLoaded();
        final int beforeNames = CUSTOM_NAMES.size();
        final int beforeAliases = CUSTOM_ALIASES.size();
        CUSTOM_NAMES.entrySet()
            .removeIf(e -> value.equals(e.getValue()));
        CUSTOM_ALIASES.entrySet()
            .removeIf(e -> value.equals(e.getValue()));
        final int removed = (beforeNames - CUSTOM_NAMES.size()) + (beforeAliases - CUSTOM_ALIASES.size());
        if (removed > 0) {
            write();
        }
        return removed;
    }

    /** Terminal-side helper: drops entries whose key or value contains the text. */
    public static synchronized int removeByText(final String text) {
        if (text == null || text.trim()
            .isEmpty()) {
            return 0;
        }
        ensureLoaded();
        final String needle = text.trim();
        final int before = CUSTOM_NAMES.size() + CUSTOM_ALIASES.size();
        CUSTOM_NAMES.entrySet()
            .removeIf(e -> matches(e.getKey(), e.getValue(), needle));
        CUSTOM_ALIASES.entrySet()
            .removeIf(e -> matches(e.getKey(), e.getValue(), needle));
        final int removed = before - (CUSTOM_NAMES.size() + CUSTOM_ALIASES.size());
        if (removed > 0) {
            write();
        }
        return removed;
    }

    private static boolean matches(final String key, final String value, final String needle) {
        return key.contains(needle) || value.contains(needle) || needle.contains(key);
    }

    public static synchronized void clear() {
        ensureLoaded();
        CUSTOM_NAMES.clear();
        CUSTOM_ALIASES.clear();
        write();
    }
}
