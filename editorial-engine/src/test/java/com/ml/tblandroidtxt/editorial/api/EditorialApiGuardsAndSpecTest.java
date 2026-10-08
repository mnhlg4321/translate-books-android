package com.ml.tblandroidtxt.editorial.api;

import com.ml.tblandroidtxt.editorial.pack.EditorialCanonicalJson;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** App guards after the edit call, and the one source of the check answer (prompt = schema = parser). */
public final class EditorialApiGuardsAndSpecTest {
    private static String lines(int count, String prefix) {
        StringBuilder out = new StringBuilder();
        for (int i = 1; i <= count; i++) out.append(prefix).append(' ').append(i).append(i < count ? "\n" : "");
        return out.toString();
    }

    private static EditGuards.Report check(String raw, String draft, String edited, List<EditInputs.GlossaryEntry> glossary) {
        return EditGuards.check(raw, draft, edited, glossary, EditGuards.Config.defaults());
    }

    // ---- guards: one pass and one violation each ----

    @Test public void structureGuard() {
        String draft = lines(100, "Dòng");
        assertFalse(check(draft, draft, lines(95, "Dòng"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
        assertTrue(check(draft, draft, lines(88, "Dòng"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
        // 10 % of 20 lines is 2: two lines of difference pass, three do not
        String small = lines(20, "Câu");
        assertFalse(check(small, small, lines(18, "Câu"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
        assertTrue(check(small, small, lines(17, "Câu"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
        // on a long text the 5-line cap applies: 6 lines out of 200 is 3 % but still too many
        String large = lines(200, "Hàng");
        assertFalse(check(large, large, lines(195, "Hàng"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
        assertTrue(check(large, large, lines(194, "Hàng"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
        // blank lines do not count
        assertFalse(check(draft, draft, draft.replace("\n", "\n\n"), List.of()).has(EditGuards.Code.STRUCTURE_WARN));
    }

    @Test public void rewriteGuard() {
        String draft = lines(50, "Câu số");
        assertFalse(check(draft, draft, draft.replace("Câu số 3", "Câu số ba"), List.of()).has(EditGuards.Code.REWRITE_WARN));
        assertTrue(check(draft, draft, lines(50, "Hoàn toàn khác biệt và dài hơn nhiều"), List.of()).has(EditGuards.Code.REWRITE_WARN));
        assertEquals(0.0, check(draft, draft, draft, List.of()).rewriteRatio(), 0.0);
    }

    @Test public void statusLabelGuardRestoresOnlyCaseOnlyChangesOnTheSameAlignedLine() {
        String raw = "status";
        String draft = "【Cấp độ】 Tên riêng: LaTeX\n【ĐANG MỞ】";
        String edited = "【cấp độ】 Tên riêng: LaTeX\n【đang mở】";
        EditGuards.Report report = check(raw, draft, edited, List.of());
        assertEquals(draft, report.cleaned());
        assertTrue(report.has(EditGuards.Code.STATUS_LABEL_CASE_RESTORED));
        assertEquals("【Cấp mới】", check(raw, "【Cấp độ】", "【Cấp mới】", List.of()).cleaned());
        assertEquals("【Cấp độ】\nkhác dòng", check(raw, "【Cấp độ】\nkhác dòng", "【cấp độ】\nkhác dòng", List.of()).cleaned());
    }

    @Test public void symbolGuardFlagsChangesRawDoesNotExplain() {
        String raw = "「あ」\n「い」";
        String draft = "「A」\n「B」";
        assertFalse(check(raw, draft, "「A」\n「B」", List.of()).has(EditGuards.Code.SYMBOL_WARN));
        // the edit dropped a closing bracket and RAW still has two
        assertTrue(check(raw, draft, "「A」\n「B", List.of()).has(EditGuards.Code.SYMBOL_WARN));
        // the edit changed the count to what RAW has: explained
        assertFalse(check(raw, "「A」", "「A」\n「B」", List.of()).has(EditGuards.Code.SYMBOL_WARN));
    }

    @Test public void glossaryGuardListsMissingTargets() {
        List<EditInputs.GlossaryEntry> glossary = List.of(new EditInputs.GlossaryEntry("太郎", "Taro", "", ""),
                new EditInputs.GlossaryEntry("花子", "Hanako", "", ""), new EditInputs.GlossaryEntry("次郎", "Jiro", "", ""));
        String raw = "太郎と花子が来た。";
        assertFalse(check(raw, "Taro và Hanako đến.", "Taro và Hanako đến.", glossary).has(EditGuards.Code.GLOSSARY_WARN));
        EditGuards.Report r = check(raw, "Taro và Hanako đến.", "Tarô và Hanako đến.", glossary);
        assertTrue(r.has(EditGuards.Code.GLOSSARY_WARN));
        assertEquals(1, r.flags().stream().filter(f -> f.code() == EditGuards.Code.GLOSSARY_WARN).count());
        assertTrue(r.flags().stream().anyMatch(f -> f.detail().contains("太郎")));
    }

    @Test public void metaLeakIsStrippedWhenSeparableAndFlaggedWhenNot() {
        EditGuards.Report lead = check("r", "Văn bản.", "Dưới đây là bản đã sửa:\nVăn bản.", List.of());
        assertEquals("Văn bản.", lead.cleaned());
        assertTrue(lead.has(EditGuards.Code.META_LEAK));
        assertTrue(lead.separable());
        EditGuards.Report tail = check("r", "Văn bản.", "Văn bản.\n\nGhi chú: đã giữ nguyên.\n- giữ tên riêng", List.of());
        assertEquals("Văn bản.", tail.cleaned());
        EditGuards.Report fence = check("r", "Văn bản.", "```\nVăn bản.\n```", List.of());
        assertEquals("Văn bản.", fence.cleaned());
        EditGuards.Report inside = check("r", "Một.\nHai.", "Một.\nGhi chú: giữa chừng\nHai.", List.of());
        assertFalse(inside.separable());
        assertTrue(inside.has(EditGuards.Code.META_UNSEPARABLE));
        assertTrue(check("r", "Văn bản.", "Văn bản.", List.of()).flags().isEmpty());
    }

    // ---- one source for the check answer ----

    @SuppressWarnings("unchecked")
    @Test public void everyFieldOfTheSpecIsRequiredInTheSchemaAndNamedInThePrompt() {
        Map<String, Object> schema = CheckSpec.schema();
        assertEquals(Boolean.FALSE, schema.get("additionalProperties"));
        List<Object> required = (List<Object>) schema.get("required");
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        String prompt = CheckSpec.promptDescription();
        for (CheckSpec.Field field : CheckSpec.topLevel()) {
            assertTrue(field.name(), required.contains(field.name()));
            assertTrue(properties.containsKey(field.name()));
            assertTrue(prompt.contains("- " + field.name() + ":"));
        }
        assertTrue(required.contains(CheckSpec.ISSUES));
        Map<String, Object> items = (Map<String, Object>) ((Map<String, Object>) properties.get(CheckSpec.ISSUES)).get("items");
        List<Object> itemRequired = (List<Object>) items.get("required");
        assertEquals(Boolean.FALSE, items.get("additionalProperties"));
        for (CheckSpec.Field field : CheckSpec.issueFields()) {
            assertTrue(field.name(), itemRequired.contains(field.name()));
            assertTrue(prompt.contains("  - " + field.name() + ":"));
        }
        assertEquals(CheckSpec.issueFields().stream().map(CheckSpec.Field::name).collect(Collectors.toList()),
                itemRequired.stream().map(Object::toString).collect(Collectors.toList()));
        // the kinds in the schema are exactly the contract's kinds
        Map<String, Object> kind = (Map<String, Object>) ((Map<String, Object>) items.get("properties")).get(CheckSpec.KIND);
        assertEquals(EditorialApiContract.IssueKind.values().length, ((List<Object>) kind.get("enum")).size());
    }

    @Test public void aWireThatSatisfiesTheSchemaIsReadByTheParserAndOneWithExtrasOrWithoutFixStillIs() {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("edited_quote", "năm mươi");
        issue.put("raw_quote", "五百");
        issue.put("kind", "NUMBER");
        issue.put("fix", "năm trăm");
        List<Object> issues = new ArrayList<>(List.of(issue));
        Map<String, Object> wire = new LinkedHashMap<>();
        wire.put("verdict", "ISSUES");
        wire.put("wrong_pair_evidence", "");
        wire.put("issues", issues);
        assertTrue(satisfies(CheckSpec.schema(), wire));
        assertEquals(1, CheckResponseParser.parse(EditorialCanonicalJson.canonicalize(wire)).issues().size());
        // extra field: refused by the schema, tolerated by the parser
        issue.put("confidence", "high");
        assertFalse(satisfies(CheckSpec.schema(), wire));
        CheckResponseParser.Parsed extra = CheckResponseParser.parse(EditorialCanonicalJson.canonicalize(wire));
        assertEquals(1, extra.issues().size());
        assertEquals(1, extra.counters().unknownKeys());
        // missing fix: refused by the schema, tolerated by the parser
        issue.remove("confidence");
        issue.remove("fix");
        assertFalse(satisfies(CheckSpec.schema(), wire));
        CheckResponseParser.Parsed missing = CheckResponseParser.parse(EditorialCanonicalJson.canonicalize(wire));
        assertEquals(1, missing.issues().size());
        assertEquals("", missing.issues().get(0).fix());
        assertEquals(1, missing.counters().missingKeys());
    }

    @SuppressWarnings("unchecked")
    private static boolean satisfies(Map<String, Object> schema, Object value) {
        Object type = schema.get("type");
        if ("object".equals(type)) {
            if (!(value instanceof Map)) return false;
            Map<String, Object> m = (Map<String, Object>) value;
            Map<String, Object> props = (Map<String, Object>) schema.get("properties");
            for (Object key : (List<Object>) schema.get("required")) if (!m.containsKey(key)) return false;
            for (String key : m.keySet()) if (!props.containsKey(key)) return false;
            for (Map.Entry<String, Object> e : m.entrySet()) if (!satisfies((Map<String, Object>) props.get(e.getKey()), e.getValue())) return false;
            return true;
        }
        if ("array".equals(type)) {
            if (!(value instanceof List)) return false;
            for (Object item : (List<Object>) value) if (!satisfies((Map<String, Object>) schema.get("items"), item)) return false;
            return true;
        }
        if (!(value instanceof String)) return false;
        String s = (String) value;
        if (schema.containsKey("enum")) return ((List<Object>) schema.get("enum")).contains(s);
        return s.length() >= ((BigDecimal) schema.get("minLength")).intValue() && s.length() <= ((BigDecimal) schema.get("maxLength")).intValue();
    }
}
