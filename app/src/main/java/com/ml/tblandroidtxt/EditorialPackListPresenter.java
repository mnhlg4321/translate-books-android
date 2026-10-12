package com.ml.tblandroidtxt;

import com.ml.tblandroidtxt.editorial.pack.EditorialPackManifest;
import com.ml.tblandroidtxt.editorial.pack.EditorialPackRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Pure read-only presenter; all registry failures become an explicit error state. */
public final class EditorialPackListPresenter {
    public enum State { LOADING, EMPTY, CONTENT, ERROR }
    public record Result(State state, List<EditorialPackUiModel> packs, String errorMessage) {
        public Result { packs = List.copyOf(packs == null ? List.of() : packs); errorMessage = errorMessage == null ? "" : errorMessage; }
    }

    private final EditorialPackRegistry registry;
    private final EditorialPackUiMapper mapper;

    public EditorialPackListPresenter(EditorialPackRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("Pack registry is required");
        this.registry = registry;
        this.mapper = new EditorialPackUiMapper();
    }

    public Result load() {
        try {
            ArrayList<EditorialPackUiModel> models = new ArrayList<>();
            for (EditorialPackManifest manifest : registry.list()) models.add(mapper.map(manifest));
            models.sort(PACK_ORDER);
            return new Result(models.isEmpty() ? State.EMPTY : State.CONTENT, models, "");
        } catch (RuntimeException e) {
            return new Result(State.ERROR, List.of(), readable(e));
        }
    }

    public Optional<EditorialPackUiModel> findByHash(String canonicalHash) {
        try { return registry.findByHash(canonicalHash).map(mapper::map); }
        catch (RuntimeException e) { return Optional.empty(); }
    }

    public Optional<EditorialPackUiModel> findByIdentity(String packId, String version) {
        try { return registry.findByIdentity(packId, version).map(mapper::map); }
        catch (RuntimeException e) { return Optional.empty(); }
    }

    private static final Comparator<EditorialPackUiModel> PACK_ORDER = (left, right) -> {
        int byPack = left.packId().compareTo(right.packId());
        if (byPack != 0) return byPack;
        int byVersion = compareVersions(right.version(), left.version());
        if (byVersion != 0) return byVersion;
        return left.canonicalHash().compareTo(right.canonicalHash());
    };

    static int compareVersions(String left, String right) {
        String[] a = left.split("\\."); String[] b = right.split("\\.");
        int count = Math.max(a.length, b.length);
        for (int i = 0; i < count; i++) {
            String av = i < a.length ? a[i] : "0"; String bv = i < b.length ? b[i] : "0";
            try {
                int numeric = Integer.compare(Integer.parseInt(av), Integer.parseInt(bv));
                if (numeric != 0) return numeric;
            } catch (NumberFormatException ignored) {
                int lexical = av.compareTo(bv);
                if (lexical != 0) return lexical;
            }
        }
        return 0;
    }

    private static String readable(Throwable error) { return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(); }
}
