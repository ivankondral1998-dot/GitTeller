package app.unocontrol;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

/** Typed GitHub events. These are model tests, not a device check. */
public class EventModelTest {
    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";
    private static final String SHA2 = "abcdef0123456789abcdef0123456789abcdef01";

    private static JSONObject snap(JSONArray issues, JSONArray pulls) throws Exception {
        return new JSONObject().put("repo", "demo/proj").put("branchesComplete", true)
                .put("issuesComplete", true).put("pullsComplete", true)
                .put("branches", new JSONArray().put(new JSONObject().put("name", "main").put("sha", SHA)))
                .put("issues", issues).put("pulls", pulls);
    }

    private static JSONObject issue(int n, String title, String state) throws Exception {
        return new JSONObject().put("number", n).put("title", title).put("state", state);
    }

    private static String kinds(JSONArray events) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e != null) b.append(e.optString("kind")).append(' ');
        }
        return b.toString();
    }

    @Test public void matchedBranchChangeAppearsEvenWithPartialLists() throws Exception {
        JSONObject before = snap(new JSONArray(),new JSONArray()).put("branchesComplete",false);
        JSONObject after = snap(new JSONArray(),new JSONArray()).put("branchesComplete",false);
        after.put("branches",new JSONArray().put(new JSONObject().put("name","main")
                .put("sha", SHA2)));
        assertEquals(1,Summary.of(after,before).advanced);
        assertTrue(kinds(ChangeSet.between(before,after)).contains("branch_moved"));
    }

    @Test public void branchAbsenceInCompleteSnapshotsIsNotCalledDeletion() throws Exception {
        JSONObject before = snap(new JSONArray(),new JSONArray());
        JSONObject after = snap(new JSONArray(),new JSONArray()).put("branches",new JSONArray());
        JSONArray events = ChangeSet.between(before,after);
        assertTrue(kinds(events).contains("branch_absent"));
        assertTrue(events.getJSONObject(0).getString("event").contains("не обнаружено"));
        assertTrue(events.getJSONObject(0).getString("basis").contains("не подтверждено"));
    }

    @Test public void partialCurrentWithProvenNewNameStillShowsHomeChange() throws Exception {
        JSONObject before = snap(new JSONArray(),new JSONArray());
        JSONObject after = snap(new JSONArray(),new JSONArray()).put("branchesComplete",false);
        after.put("branches",new JSONArray().put(new JSONObject().put("name","main").put("sha",SHA))
                .put(new JSONObject().put("name","experimental").put("sha", SHA2)));
        Summary s = Summary.of(after,before);
        assertEquals(1,s.added);
        assertTrue(HumanReport.whatChanged(after,before,s).contains("впервые"));
    }

    @Test public void newIssueCountsWhileBranchPointersStay() throws Exception {
        JSONObject base = snap(new JSONArray(), new JSONArray());
        JSONObject created = snap(new JSONArray().put(issue(7, "Fix crash", "open")), new JSONArray());
        Summary s = Summary.of(created, base);
        assertTrue(s.issueChanges > 0);
        assertEquals(0, s.advanced);
        String line = HumanReport.whatChanged(created, base, s);
        assertTrue(line.contains("Появилась задача #7"));
        assertFalse(line.contains("не сдвинулись"));
        assertFalse(line.toLowerCase().contains("sha"));
        JSONArray events = ChangeSet.between(base, created);
        assertTrue(kinds(events).contains("issue_new"));
        assertTrue(events.getJSONObject(0).getString("url").startsWith("https://github.com/demo/proj/issues/7"));
        assertTrue(events.getJSONObject(0).getString("unknown").contains("работоспособность"));
    }

    @Test public void mergeCountsWhenStateStaysClosed() throws Exception {
        JSONObject prior = snap(new JSONArray(), new JSONArray().put(new JSONObject()
                .put("number", 5).put("title", "proposal").put("state", "closed").put("merged", false)));
        JSONObject now = snap(new JSONArray(), new JSONArray().put(new JSONObject()
                .put("number", 5).put("title", "proposal").put("state", "closed").put("merged", true)
                .put("merged_at", "2026-03-01T12:00:00Z")));
        Summary s = Summary.of(now, prior);
        assertTrue(s.pullChanges > 0);
        JSONArray events = ChangeSet.between(prior, now);
        assertTrue(kinds(events).contains("pull_merged"));
        assertTrue(events.getJSONObject(0).getString("event").contains("слито"));
        assertTrue(events.getJSONObject(0).getString("basis").contains("2026-03-01T12:00:00Z"));
        assertFalse(events.getJSONObject(0).getString("event").contains("завершена работа"));
    }

    @Test public void titleChangeIsNotCompletion() throws Exception {
        JSONObject base = snap(new JSONArray().put(issue(7, "Fix crash", "open")), new JSONArray());
        JSONObject renamed = snap(new JSONArray().put(issue(7, "New title", "open")), new JSONArray());
        Summary s = Summary.of(renamed, base);
        assertTrue(s.issueChanges > 0);
        String line = HumanReport.whatChanged(renamed, base, s);
        assertTrue(line.contains("название"));
        assertTrue(line.contains("не завершение") || line.contains("не доказательство"));
        assertFalse(line.contains("закрыта"));
    }

    @Test public void reopenIsItsOwnTransition() throws Exception {
        JSONObject open = snap(new JSONArray().put(issue(7, "Fix crash", "open")), new JSONArray());
        JSONObject closed = snap(new JSONArray().put(issue(7, "Fix crash", "closed")), new JSONArray());
        JSONObject again = snap(new JSONArray().put(issue(7, "Fix crash", "open")), new JSONArray());
        assertTrue(kinds(ChangeSet.between(open, closed)).contains("issue_state"));
        JSONArray back = ChangeSet.between(closed, again);
        assertTrue(kinds(back).contains("issue_state"));
        assertTrue(back.getJSONObject(0).getString("event").contains("снова открыта"));
        assertTrue(back.getJSONObject(0).getString("basis").contains("closed → open"));
    }

    @Test public void updatedAtWithoutBodyIsAnEditNotAFix() throws Exception {
        JSONObject old = snap(new JSONArray().put(issue(7, "Fix crash", "open").put("updated_at", "2026-01-01T00:00:00Z")), new JSONArray());
        JSONObject now = snap(new JSONArray().put(issue(7, "Fix crash", "open").put("updated_at", "2026-02-01T00:00:00Z")), new JSONArray());
        assertEquals(1, Summary.of(now, old).issueChanges);
        JSONArray events = ChangeSet.between(old, now);
        assertTrue(kinds(events).contains("issue_touch"));
        assertTrue(events.getJSONObject(0).getString("basis").contains("updated_at"));
        assertTrue(events.getJSONObject(0).getString("event").contains("неизвестно"));
        assertFalse(events.getJSONObject(0).getString("event").contains("исправлен"));
    }

    @Test public void incompleteListDoesNotInventCreationOrDeletion() throws Exception {
        JSONObject old = snap(new JSONArray(), new JSONArray());
        JSONObject now = snap(new JSONArray().put(issue(9, "Later", "open")), new JSONArray());
        now.put("issuesComplete", false);
        assertEquals(0, Summary.of(now, old).issueChanges);
        assertEquals(0, ChangeSet.between(old, now).length());
        JSONObject fullOld = snap(new JSONArray().put(issue(3, "Was", "open")), new JSONArray());
        JSONObject fullNow = snap(new JSONArray(), new JSONArray());
        assertEquals(0, Summary.of(fullNow, fullOld).issueChanges);
        assertTrue(kinds(ChangeSet.between(fullOld, fullNow)).contains("issue_absent"));
        fullOld.put("issuesComplete", false);
        assertEquals(0, ChangeSet.between(fullOld, fullNow).length());
    }

    @Test public void firstSnapshotDoesNotInventABaseline() throws Exception {
        JSONObject now = snap(new JSONArray().put(issue(1, "A", "open")), new JSONArray());
        Summary s = Summary.of(now, null);
        assertEquals(0, s.issueChanges);
        assertEquals(0, s.pullChanges);
        String line = HumanReport.whatChanged(now, null, s);
        assertTrue(line.contains("Базы") || line.contains("первая"));
        assertFalse(line.contains("не сдвинулись"));
    }

    @Test public void compareWordingDoesNotCallBehindProgress() {
        String ahead = GithubClient.explainCompareStatus("ahead", 3, 0, false);
        String behind = GithubClient.explainCompareStatus("behind", 0, 2, false);
        String diverged = GithubClient.explainCompareStatus("diverged", 1, 4, false);
        String same = GithubClient.explainCompareStatus("identical", 0, 0, false);
        String unknown = GithubClient.explainCompareStatus("weird", 0, 0, false);
        String cut = GithubClient.explainCompareStatus("ahead", 300, 0, true);
        assertTrue(ahead.contains("новые записи"));
        assertTrue(ahead.contains("Проверка работоспособности отсутствует"));
        assertTrue(behind.contains("откатилась"));
        assertTrue(behind.contains("не продвижение"));
        assertFalse(behind.contains("новые записи"));
        assertTrue(diverged.contains("разошлась"));
        assertTrue(diverged.contains("обычным продвижением"));
        assertTrue(same.contains("совпадают"));
        assertFalse(same.contains("готово"));
        assertTrue(unknown.contains("Недостаточно данных"));
        assertTrue(cut.contains("неполный"));
        assertFalse(ahead.contains("готово"));
    }

    @Test public void compareHttpFailureStaysInsufficient() {
        String missing = GithubClient.compareFailure(new GithubClient.HttpFailure(404, GithubClient.errorForStatus(404, "5")));
        String conflict = GithubClient.compareFailure(new GithubClient.HttpFailure(409, GithubClient.errorForStatus(409, null)));
        assertTrue(missing.contains("Недостаточно данных"));
        assertTrue(missing.contains("404"));
        assertTrue(conflict.contains("Недостаточно данных"));
        assertTrue(conflict.contains("409"));
        assertTrue(missing.contains("сохранены"));
        assertFalse(missing.contains("ghp_"));
    }

    @Test public void backupKeepsMergeAndDropsSecrets() throws Exception {
        JSONObject raw = snap(new JSONArray().put(issue(7, "Fix crash", "open").put("body", "secret note").put("updated_at", "2026-01-02T00:00:00Z")),
                new JSONArray().put(new JSONObject().put("number", 5).put("title", "proposal").put("state", "closed")
                        .put("merged", true).put("merged_at", "2026-03-01T12:00:00Z").put("token", "ghp_SECRET")));
        raw.put("restoredFromBackup", true);
        raw.put("authorization", "Bearer nope");
        JSONObject clean = SnapshotCodec.sanitizeSnapshot(raw, "demo/proj");
        assertTrue(clean.getJSONArray("pulls").getJSONObject(0).getBoolean("merged"));
        assertEquals("2026-03-01T12:00:00Z", clean.getJSONArray("pulls").getJSONObject(0).getString("merged_at"));
        assertEquals("2026-01-02T00:00:00Z", clean.getJSONArray("issues").getJSONObject(0).getString("updated_at"));
        assertEquals("IMPORTED", clean.getString("origin"));
        String text = clean.toString().toLowerCase();
        assertFalse(text.contains("secret"));
        assertFalse(text.contains("ghp_"));
        assertFalse(text.contains("authorization"));
        assertFalse(clean.getJSONArray("issues").getJSONObject(0).has("body"));
    }

    @Test public void failedTokenInstallKeepsPreviousCiphertext() throws Exception {
        java.io.File dir = Files.createTempDirectory("gt-vault").toFile();
        java.io.File dest = new java.io.File(dir, "token.bin");
        java.io.File tmp = new java.io.File(dir, "token.bin.tmp");
        byte[] oldBytes = new byte[] {7, 1, 2, 3, 4};
        byte[] newBytes = new byte[] {8, 8, 8};
        Files.write(dest.toPath(), oldBytes);
        Files.write(tmp.toPath(), newBytes);
        TokenVault.testFailInstall = true;
        try {
            try {
                TokenVault.replaceAtomically(tmp, dest);
                fail("install must fail");
            } catch (TokenVault.VaultException expected) {
                assertTrue(expected.getMessage().contains("сохранена"));
            }
            assertArrayEquals(oldBytes, Files.readAllBytes(dest.toPath()));
            assertFalse(tmp.exists());
            assertFalse(new String(Files.readAllBytes(dest.toPath()), StandardCharsets.ISO_8859_1).contains("ghp_"));
            TokenVault.testFailInstall = false;
            Files.write(tmp.toPath(), newBytes);
            TokenVault.replaceAtomically(tmp, dest);
            assertArrayEquals(newBytes, Files.readAllBytes(dest.toPath()));
            assertFalse(new java.io.File(dir, "token.bin.bak").exists());
        } finally {
            TokenVault.testFailInstall = false;
            tmp.delete();
            dest.delete();
            new java.io.File(dir, "token.bin.bak").delete();
            dir.delete();
        }
    }

    @Test public void repeatedBranchMoveStaysInTheFeed() throws Exception {
        JSONArray prior = new JSONArray().put(new JSONObject().put("kind", "branch_moved").put("id", "branch:main")
                .put("event", "Указатель изменился").put("at", 100L));
        JSONArray again = new JSONArray().put(new JSONObject().put("kind", "branch_moved").put("id", "branch:main")
                .put("event", "Указатель изменился"));
        JSONArray merged = EventFeed.merge(prior, again, 200L);
        assertEquals(2, merged.length());
        assertEquals(200L, merged.getJSONObject(0).getLong("at"));
    }

    @Test public void pageOneEtagIsDroppedWhenAnotherPageExists() {
        String next = "<https://api.github.com/repositories/1/issues?page=2>; rel=\"next\"";
        assertEquals("", GithubClient.etagIfCompletePage("\"page-1\"", next));
        assertEquals("\"page-1\"", GithubClient.etagIfCompletePage("\"page-1\"", null));
        assertEquals("", GithubClient.etagIfCompletePage("", null));
    }

    @Test public void legacyEtagWithoutSinglePageProofIsIgnored() throws Exception {
        java.lang.reflect.Method method = GithubClient.class.getDeclaredMethod("etagIfSafe",
                JSONObject.class, String.class, String.class, String.class);
        method.setAccessible(true);
        JSONObject legacy = new JSONObject().put("repo", "demo/proj").put("issuesComplete", true)
                .put("issues", new JSONArray().put(issue(7, "Fix crash", "open")))
                .put("issuesEtag", "\"page-1\"");
        assertNull(method.invoke(null, legacy, "issues", "issuesComplete", "issuesEtag"));
        legacy.put("issuesEtagSinglePage", true);
        assertEquals("\"page-1\"", method.invoke(null, legacy, "issues", "issuesComplete", "issuesEtag"));
    }

    @Test public void interruptedRenameLeavesRecoverableCiphertext() throws Exception {
        java.io.File dir = Files.createTempDirectory("gt-bak").toFile();
        java.io.File dest = new java.io.File(dir, "token.bin");
        java.io.File bak = new java.io.File(dir, "token.bin.bak");
        byte[] oldBytes = new byte[] {7, 1, 2};
        Files.write(bak.toPath(), oldBytes);
        assertTrue(AtomicCipherFile.hasRecord(dest));
        assertFalse(dest.isFile());
        java.io.File restored = AtomicCipherFile.recover(dest);
        assertTrue(restored.isFile());
        assertFalse(bak.exists());
        assertArrayEquals(oldBytes, Files.readAllBytes(restored.toPath()));
        java.io.File tmp = new java.io.File(dir, "token.bin.tmp");
        Files.write(tmp.toPath(), new byte[] {9});
        AtomicCipherFile.replace(tmp, dest);
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(dest.toPath()));
        assertFalse(tmp.exists());
        AtomicCipherFile.delete(dest);
        assertFalse(AtomicCipherFile.hasRecord(dest));
        dest.delete();
        dir.delete();
    }
}
