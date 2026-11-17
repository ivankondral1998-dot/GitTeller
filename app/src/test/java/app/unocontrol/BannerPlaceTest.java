package app.unocontrol;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BannerPlaceTest {
    @Test public void bannerSitsOnGithubFactsAndPlainExplanations() {
        String[] shown = {
                "overview", "changes", "problems", "branches", "branch-detail",
                "commits", "commit-detail", "tasks", "issue-detail", "pulls", "pull-detail",
                "accuracy", "search",
                "about", "guide", "token-help", "why", "incomplete"
        };
        for (String page : shown) assertTrue(page, SectionActivity.showsBanner(page));
    }

    @Test public void bannerStaysOutOfSettingsMenusAndTheTokenForm() {
        String[] hidden = {
                "settings", "data", "appearance", "access", "projects", "index", "", null
        };
        for (String page : hidden) assertFalse(String.valueOf(page), SectionActivity.showsBanner(page));
    }
}
