package com.asdflj.wtct.client.gui;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.client.gui.widget.GuiWcwtBackButton;
import com.asdflj.wtct.network.CPacketTerminalBtns;
import com.asdflj.wtct.util.NeCharUtil;
import com.asdflj.wtct.util.PatternMappingStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * WCWT/ExtendedAE Plus' provider chooser ("供应器选择"), rebuilt for 1.7.10.
 *
 * <p>
 * Plus lists the pattern providers the player's terminal can see - the very list the pattern access
 * terminal (二合一接口终端) shows - grouped by name with their free-slot totals, and clicking one
 * uploads the encoded pattern straight into it. The query is run through the mapping table first, so
 * a recipe-type key or alias is turned into the provider search term before matching.
 *
 * <p>
 * The list arrives as plain {@link Entry}s instead of coming from a server request: this port already
 * streams the same providers to the client over the interface-terminal protocol, so the caller
 * (the comprehensive work terminal) hands them straight over.
 *
 * <p>
 * Two modes:
 *
 * <ul>
 * <li><b>upload</b> (no {@code nameSink}): clicking a provider sends the upload request for it, and
 * a query with exactly one match may auto-upload (Plus' "自动上传唯一匹配");
 * <li><b>pick</b> (with {@code nameSink}): clicking a provider reports its name back to the caller -
 * the mapping manager uses this to fill its value field from the real provider list.
 * </ul>
 */
public class GuiProviderSelectScreen extends GuiScreen {

    /** One provider as the terminal sees it: server id, display name, free pattern slots. */
    public record Entry(long id, String name, int emptySlots) {}

    private static final int BTN_H = 20;
    private static final int GAP = 5;
    private static final int CONTENT_W = 320;
    private static final int ID_ENTRY = 100;
    private static final int ID_PREV = 200;
    private static final int ID_NEXT = 201;
    private static final int ID_AUTO = 202;
    private static final int ID_BACK = 203;
    private static final int HINT_COLOR = 0xFFA0A0A0;
    private static final int MAP_COLOR = 0xFF55FFFF;
    private static final int STATUS_COLOR = 0xFFFF5555;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    /** Provider names pinned to the top of the list (Plus' pinned_providers.json). */
    private static final Set<String> PINNED = new LinkedHashSet<>();
    private static boolean autoUploadUniqueMatch = true;
    private static boolean configLoaded;

    private final GuiScreen parent;
    private final List<Entry> providers;
    /** Non-null turns the screen into "pick a provider name" mode. */
    private final Consumer<String> nameSink;
    /**
     * Upload mode only: told the chosen provider's name as the request goes out. The terminal uses it
     * to put that name back into its provider search box, so the list shows where the pattern landed.
     */
    private Consumer<String> uploadedNameSink;
    /** Raw text in the search box; {@link #query} is what it resolved to. */
    private String typed;
    private String query = "";

    // Grouped by name: representative id, free-slot total and provider count per name.
    private final List<Long> gIds = new ArrayList<>();
    private final List<String> gNames = new ArrayList<>();
    private final List<Integer> gSlots = new ArrayList<>();
    private final List<Integer> gCount = new ArrayList<>();
    // The grouped list after filtering and sorting.
    private final List<Long> fIds = new ArrayList<>();
    private final List<String> fNames = new ArrayList<>();
    private final List<Integer> fSlots = new ArrayList<>();
    private final List<Integer> fCount = new ArrayList<>();

    private GuiTextField searchField;
    private GuiButton autoButton;
    private final List<GuiButton> entryButtons = new ArrayList<>();
    private int page;
    private int pageSize = 6;
    private int startY;
    private int titleY;
    private int hintY;
    private int exactMatches;
    private boolean fellBack;
    private boolean autoUploadAttempted;
    private boolean pendingAutoUpload;
    private String status = "";

    /** Provider chooser: picking an entry uploads the encoded pattern there. */
    public GuiProviderSelectScreen(final GuiScreen parent, final List<Entry> providers, final String presetQuery) {
        this(parent, providers, presetQuery, null);
    }

    /** With a {@code nameSink} the screen only reports the chosen provider's name back. */
    public GuiProviderSelectScreen(final GuiScreen parent, final List<Entry> providers, final String presetQuery,
        final Consumer<String> nameSink) {
        this.parent = parent;
        this.providers = new ArrayList<>(providers == null ? List.of() : providers);
        this.nameSink = nameSink;
        loadConfig();
        this.typed = presetQuery == null ? "" : presetQuery.trim();
        final String resolved = PatternMappingStore.resolveProviderSearchKey(this.typed);
        this.query = resolved == null ? "" : resolved;
        buildGroups();
        applyFilter();
        // Only a query that came in with the screen (i.e. from the mapping flow) is allowed to fire
        // an upload by itself - never something the player just typed while looking at the list.
        this.pendingAutoUpload = nameSink == null && !this.query.isEmpty();
    }

    /** Upload mode only: hooked with the chosen provider's name just before the upload request. */
    public void setUploadedNameSink(final Consumer<String> sink) {
        this.uploadedNameSink = sink;
    }

    // ------------------------------------------------------------- layout

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.entryButtons.clear();

        // Search box (30) + entries + pager (30) + toggle (30) + cancel (20) + margins (40), the
        // original's own accounting, which keeps the dialog usable at any GUI scale.
        final int reserved = 30 + 30 + 30 + 20 + 40;
        this.pageSize = Math.max(2, (this.height - reserved) / (BTN_H + GAP));
        final int contentH = 30 + this.pageSize * (BTN_H + GAP) + 30 + 30 + 20;
        final int top = Math.max(30, (this.height - contentH) / 2);
        final int centerX = this.width / 2;
        final int left = centerX - CONTENT_W / 2;

        this.titleY = top - 26;
        this.hintY = top - 14;

        this.searchField = new GuiTextField(this.fontRendererObj, left, top, CONTENT_W, BTN_H);
        this.searchField.setMaxStringLength(64);
        this.searchField.setText(this.typed == null ? "" : this.typed);
        this.searchField.setFocused(true);

        this.startY = top + 30;
        for (int i = 0; i < this.pageSize; i++) {
            final GuiButton entry = new GuiButton(
                ID_ENTRY + i,
                left,
                this.startY + i * (BTN_H + GAP),
                CONTENT_W,
                BTN_H,
                "");
            this.entryButtons.add(entry);
            this.buttonList.add(entry);
        }

        final int navY = this.startY + this.pageSize * (BTN_H + GAP);
        this.buttonList.add(new GuiButton(ID_PREV, centerX - 105, navY, 24, BTN_H, "<"));
        this.buttonList.add(new GuiButton(ID_NEXT, centerX + 81, navY, 24, BTN_H, ">"));

        final int toggleY = navY + 30;
        this.autoButton = new GuiButton(ID_AUTO, left, toggleY, CONTENT_W, BTN_H, "");
        this.autoButton.visible = this.nameSink == null;
        this.autoButton.enabled = this.nameSink == null;
        this.buttonList.add(this.autoButton);

        // Closing is AE2 1.21's back tab in the content's top-right corner, the same art the terminal's
        // own dialogs close with.
        this.buttonList.add(new GuiWcwtBackButton(ID_BACK, left + CONTENT_W - 20, top - 26));

        clampPage();
        refreshEntryButtons();
    }

    private void clampPage() {
        final int pages = Math.max(1, (this.fIds.size() + this.pageSize - 1) / this.pageSize);
        if (this.page >= pages) {
            this.page = pages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }
    }

    // -------------------------------------------------------------- data

    /** Plus' grouping step: same-named providers collapse into one entry. */
    private void buildGroups() {
        this.gIds.clear();
        this.gNames.clear();
        this.gSlots.clear();
        this.gCount.clear();
        final Map<String, Long> bestId = new LinkedHashMap<>();
        final Map<String, Integer> bestSlots = new LinkedHashMap<>();
        final Map<String, Integer> total = new LinkedHashMap<>();
        final Map<String, Integer> count = new LinkedHashMap<>();
        for (final Entry provider : this.providers) {
            // Only providers that can still take a pattern are worth listing, like the original.
            if (provider.name() == null || provider.name()
                .isEmpty() || provider.emptySlots() <= 0) {
                continue;
            }
            final String name = provider.name();
            final Integer previous = bestSlots.get(name);
            if (previous == null || provider.emptySlots() > previous) {
                bestSlots.put(name, provider.emptySlots());
                bestId.put(name, provider.id());
            }
            total.merge(name, provider.emptySlots(), Integer::sum);
            count.merge(name, 1, Integer::sum);
        }
        for (final Map.Entry<String, Integer> e : total.entrySet()) {
            this.gNames.add(e.getKey());
            this.gIds.add(bestId.get(e.getKey()));
            this.gSlots.add(e.getValue());
            this.gCount.add(count.get(e.getKey()));
        }
    }

    /** Filters by the mapping-resolved query, then sorts pinned-first / by name. */
    private void applyFilter() {
        this.fIds.clear();
        this.fNames.clear();
        this.fSlots.clear();
        this.fCount.clear();
        this.exactMatches = 0;
        this.fellBack = false;

        final String resolved = PatternMappingStore.resolveProviderSearchKey(this.query);
        final String needle = resolved == null ? ""
            : resolved.trim()
                .toLowerCase(Locale.ROOT);
        for (int i = 0; i < this.gIds.size(); i++) {
            // NeCharUtil rather than a plain contains: a pinyin query narrows the list here too.
            if (needle.isEmpty() || NeCharUtil.INSTANCE.contains(
                needle,
                this.gNames.get(i)
                    .toLowerCase(Locale.ROOT))) {
                keep(i);
                this.exactMatches++;
            }
        }
        // An unmatched query shows everything rather than an empty list, so the player is never left
        // staring at nothing wondering where the providers went.
        if (!needle.isEmpty() && this.fIds.isEmpty()) {
            this.fellBack = true;
            for (int i = 0; i < this.gIds.size(); i++) {
                keep(i);
            }
        }

        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < this.fNames.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> {
            final boolean pinnedA = PINNED.contains(this.fNames.get(a));
            final boolean pinnedB = PINNED.contains(this.fNames.get(b));
            if (pinnedA != pinnedB) {
                return pinnedA ? -1 : 1;
            }
            return this.fNames.get(a)
                .compareToIgnoreCase(this.fNames.get(b));
        });
        final List<Long> ids = new ArrayList<>(this.fIds);
        final List<String> names = new ArrayList<>(this.fNames);
        final List<Integer> slots = new ArrayList<>(this.fSlots);
        final List<Integer> counts = new ArrayList<>(this.fCount);
        this.fIds.clear();
        this.fNames.clear();
        this.fSlots.clear();
        this.fCount.clear();
        for (final int index : order) {
            this.fIds.add(ids.get(index));
            this.fNames.add(names.get(index));
            this.fSlots.add(slots.get(index));
            this.fCount.add(counts.get(index));
        }
    }

    private void keep(final int index) {
        this.fIds.add(this.gIds.get(index));
        this.fNames.add(this.gNames.get(index));
        this.fSlots.add(this.gSlots.get(index));
        this.fCount.add(this.gCount.get(index));
    }

    /** Plus' entry label: name, free slots in total, and how many providers share the name. */
    private String label(final int index) {
        final String name = this.fNames.get(index);
        final String star = PINNED.contains(name) ? "* " : "";
        return star + name + "  (" + this.fSlots.get(index) + ")  x" + this.fCount.get(index);
    }

    private void refreshEntryButtons() {
        clampPage();
        final int start = this.page * this.pageSize;
        for (int i = 0; i < this.entryButtons.size(); i++) {
            final GuiButton entry = this.entryButtons.get(i);
            final int index = start + i;
            if (index < this.fIds.size()) {
                entry.visible = true;
                entry.enabled = true;
                entry.displayString = label(index);
            } else {
                entry.visible = false;
                entry.enabled = false;
            }
        }
        for (final Object o : this.buttonList) {
            if (!(o instanceof final GuiButton button)) {
                continue;
            }
            if (button.id == ID_PREV) {
                button.enabled = this.page > 0;
            } else if (button.id == ID_NEXT) {
                button.enabled = (this.page + 1) * this.pageSize < this.fIds.size();
            }
        }
        if (this.autoButton != null) {
            this.autoButton.displayString = StatCollector.translateToLocalFormatted(
                "wtct.gui.provider_select.auto_upload",
                StatCollector.translateToLocal(
                    autoUploadUniqueMatch ? "wtct.gui.provider_select.on"
                        : "wtct.gui.provider_select.off"));
        }
    }

    // ------------------------------------------------------------ actions

    private void choose(final int index) {
        if (index < 0 || index >= this.fIds.size()) {
            return;
        }
        if (this.nameSink != null) {
            this.nameSink.accept(this.fNames.get(index));
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (this.uploadedNameSink != null) {
            this.uploadedNameSink.accept(this.fNames.get(index));
        }
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("provider", this.fIds.get(index));
        tag.setString("search", "");
        Wtct.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternManagement.UploadPattern", "", tag));
        this.mc.displayGuiScreen(this.parent);
    }

    /**
     * Plus' {@code tryAutoUploadIfUniqueMatch}: a query that came with the screen and matches exactly
     * one provider uploads without asking.
     */
    private void tryAutoUploadIfUniqueMatch() {
        if (this.nameSink != null || !autoUploadUniqueMatch || this.autoUploadAttempted) {
            return;
        }
        this.autoUploadAttempted = true;
        if (this.query == null || this.query.trim()
            .isEmpty()) {
            return;
        }
        if (this.fellBack || this.exactMatches != 1 || this.fIds.size() != 1) {
            return;
        }
        this.status = StatCollector.translateToLocal("wtct.gui.provider_select.auto_uploading");
        choose(0);
    }

    private void search() {
        this.typed = this.searchField == null ? this.typed : this.searchField.getText();
        final String resolved = PatternMappingStore.resolveProviderSearchKey(this.typed);
        this.query = resolved == null ? "" : resolved;
        this.page = 0;
        applyFilter();
        refreshEntryButtons();
        this.tryAutoUploadIfUniqueMatch();
    }

    @Override
    protected void actionPerformed(final GuiButton button) {
        if (button.id >= ID_ENTRY && button.id < ID_ENTRY + this.pageSize) {
            choose(this.page * this.pageSize + (button.id - ID_ENTRY));
            return;
        }
        switch (button.id) {
            case ID_PREV -> {
                this.page = Math.max(0, this.page - 1);
                refreshEntryButtons();
            }
            case ID_NEXT -> {
                this.page++;
                refreshEntryButtons();
            }
            case ID_AUTO -> {
                autoUploadUniqueMatch = !autoUploadUniqueMatch;
                saveConfig();
                refreshEntryButtons();
            }
            case ID_BACK -> this.mc.displayGuiScreen(this.parent);
            default -> {}
        }
    }

    // -------------------------------------------------------------- input

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (this.searchField != null && this.searchField.textboxKeyTyped(character, key)) {
            this.typed = this.searchField.getText();
            final String resolved = PatternMappingStore.resolveProviderSearchKey(this.typed);
            this.query = resolved == null ? "" : resolved;
            this.page = 0;
            applyFilter();
            refreshEntryButtons();
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            search();
            return;
        }
        super.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
        // Shift-click pins a provider to the top; the same trick Plus uses for its ★ entries.
        if (isShiftKeyDown() && mouseButton == 0) {
            for (int i = 0; i < this.entryButtons.size(); i++) {
                final GuiButton entry = this.entryButtons.get(i);
                if (!entry.visible) {
                    continue;
                }
                if (mouseX < entry.xPosition || mouseX >= entry.xPosition + entry.width
                    || mouseY < entry.yPosition
                    || mouseY >= entry.yPosition + entry.height) {
                    continue;
                }
                final int index = this.page * this.pageSize + i;
                if (index < this.fNames.size()) {
                    final String name = this.fNames.get(index);
                    if (!PINNED.remove(name)) {
                        PINNED.add(name);
                    }
                    saveConfig();
                    applyFilter();
                    refreshEntryButtons();
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.searchField != null) {
            this.searchField.updateCursorCounter();
        }
        if (this.pendingAutoUpload) {
            this.pendingAutoUpload = false;
            tryAutoUploadIfUniqueMatch();
        }
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        drawDefaultBackground();
        final String titleKey = this.nameSink == null ? "wtct.gui.provider_select.title"
            : "wtct.gui.provider_select.pick_title";
        this.drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal(titleKey),
            this.width / 2,
            this.titleY,
            0xFFFFFF);
        if (this.searchField != null) {
            this.searchField.drawTextBox();
        }
        drawHints();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.status != null && !this.status.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj, this.status, this.width / 2, this.hintY + 12, STATUS_COLOR);
        }
    }

    /** Line under the title: what the mapping resolved to, the page, and the empty-list warning. */
    private void drawHints() {
        final int centerX = this.width / 2;
        final String typedNow = this.searchField == null ? this.typed : this.searchField.getText();
        final String resolved = PatternMappingStore.resolveProviderSearchKey(typedNow);
        if (resolved != null && !resolved.isEmpty() && typedNow != null && !resolved.equals(typedNow.trim())) {
            // Show the mapping at work: the key the player typed (or the preset) resolved to the
            // provider search term on the right. ASCII arrow: the default font has no U+2192.
            this.drawCenteredString(this.fontRendererObj, "-> " + resolved, centerX, this.hintY, MAP_COLOR);
        } else {
            this.drawCenteredString(
                this.fontRendererObj,
                StatCollector.translateToLocal("wtct.gui.provider_select.hint"),
                centerX,
                this.hintY,
                HINT_COLOR);
        }
        this.drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocalFormatted(
                "wtct.gui.provider_select.page",
                this.page + 1,
                Math.max(1, (this.fIds.size() + this.pageSize - 1) / this.pageSize),
                this.fIds.size()),
            centerX,
            this.startY + this.pageSize * (BTN_H + GAP) + 3,
            HINT_COLOR);
    }

    // ------------------------------------------------------------- config

    private static File configFile() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDataDir == null) {
            return null;
        }
        return new File(new File(new File(mc.mcDataDir, "config"), "wtct"), "provider_select.json");
    }

    private static synchronized void loadConfig() {
        if (configLoaded) {
            return;
        }
        configLoaded = true;
        final File file = configFile();
        if (file == null || !file.isFile()) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            final JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            final JsonObject root = parsed.getAsJsonObject();
            if (root.has("pinned") && root.get("pinned")
                .isJsonArray()) {
                for (final JsonElement e : root.getAsJsonArray("pinned")) {
                    final String name = e.getAsString();
                    if (name != null && !name.isEmpty()) {
                        PINNED.add(name);
                    }
                }
            }
            if (root.has("auto_upload_unique_match")) {
                autoUploadUniqueMatch = root.get("auto_upload_unique_match")
                    .getAsBoolean();
            }
        } catch (final Exception ignored) {
            // A broken file only costs the pin list.
        }
    }

    private static synchronized void saveConfig() {
        final File file = configFile();
        if (file == null) {
            return;
        }
        final JsonObject root = new JsonObject();
        final JsonArray pinned = new JsonArray();
        for (final String name : PINNED) {
            pinned.add(new JsonPrimitive(name));
        }
        root.add("pinned", pinned);
        root.addProperty("auto_upload_unique_match", autoUploadUniqueMatch);
        try {
            final File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (final Exception ignored) {
            // Losing the pin list is not worth a crash.
        }
    }
}
