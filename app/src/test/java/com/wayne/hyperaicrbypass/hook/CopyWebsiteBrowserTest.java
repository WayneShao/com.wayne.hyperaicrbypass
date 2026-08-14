package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class CopyWebsiteBrowserTest {
    @Test
    public void browserChoicesKeepDefaultAndDeduplicatePackages() {
        List<CopyWebsiteBrowser.BrowserCandidate> candidates = List.of(
                new CopyWebsiteBrowser.BrowserCandidate("Vivaldi", "com.vivaldi.browser", true, true),
                new CopyWebsiteBrowser.BrowserCandidate("Vivaldi duplicate", "com.vivaldi.browser", true, true),
                new CopyWebsiteBrowser.BrowserCandidate("HTTP only", "http.only", true, false),
                new CopyWebsiteBrowser.BrowserCandidate("Firefox", "org.mozilla.firefox", true, true)
        );

        List<CopyWebsiteBrowser.BrowserChoice> choices =
                CopyWebsiteBrowser.buildChoices(candidates);

        assertEquals(CopyWebsiteBrowser.DEFAULT_BROWSER, choices.get(0).packageName());
        assertEquals(List.of("org.mozilla.firefox", "com.vivaldi.browser"),
                choices.subList(1, choices.size()).stream()
                        .map(CopyWebsiteBrowser.BrowserChoice::packageName).toList());
    }

    @Test
    public void selectedPackageFallsBackToSystemWhenMissingOrInvalid() {
        assertNull(CopyWebsiteBrowser.resolveSelectedPackage(
                CopyWebsiteBrowser.DEFAULT_BROWSER, packageName -> true));
        assertNull(CopyWebsiteBrowser.resolveSelectedPackage(
                "missing.browser", packageName -> false));
        assertEquals("com.vivaldi.browser", CopyWebsiteBrowser.resolveSelectedPackage(
                "com.vivaldi.browser", packageName -> true));
    }

    @Test
    public void branchCatalogSeparatesExecutionChainsAndAllowsKnownOwners() {
        assertEquals(2, CopyWebsiteBrowserHookCatalog.forBranch(AicrVersionBranch.V3).size());
        assertEquals(1, CopyWebsiteBrowserHookCatalog.forBranch(AicrVersionBranch.V4).size());
        assertTrue(CopyWebsiteBrowserHookCatalog.isExpectedOwner(
                AicrVersionBranch.V3, "op6"));
        assertFalse(CopyWebsiteBrowserHookCatalog.isExpectedOwner(
                AicrVersionBranch.V3, "com.thirdparty.Library"));
        assertTrue(CopyWebsiteBrowserHookCatalog.isExpectedOwner(
                AicrVersionBranch.V4,
                "com.xiaomi.aicr.copydirect.util.SmartPasswordUtils"));
        assertFalse(CopyWebsiteBrowserHookCatalog.isExpectedOwner(
                AicrVersionBranch.V4, "com.thirdparty.SmartPasswordUtils"));
    }

    @Test
    public void compactV4UsesTheTwoStageObfuscatedBrowserChain() {
        List<CopyWebsiteBrowserHookCatalog.Spec> specs =
                CopyWebsiteBrowserHookCatalog.forLayout(AicrRuntimeLayout.V4_COMPACT);

        assertEquals(2, specs.size());
        assertEquals(List.of(
                        CopyWebsiteBrowserHookCatalog.Kind.RETURN_INTENT,
                        CopyWebsiteBrowserHookCatalog.Kind.OPEN_URL
                ), specs.stream().map(CopyWebsiteBrowserHookCatalog.Spec::kind).toList());
        assertEquals(List.of("java.lang.String"), specs.get(0).parameterTypes());
        assertEquals(List.of(
                "com.xiaomi.aicr.copydirect.IntentActivity", "java.lang.String"
        ), specs.get(1).parameterTypes());
        assertTrue(CopyWebsiteBrowserHookCatalog.isExpectedOwner(
                AicrRuntimeLayout.V4_COMPACT, "bw8"));
        assertFalse(CopyWebsiteBrowserHookCatalog.isExpectedOwner(
                AicrRuntimeLayout.V4_COMPACT, "com.thirdparty.SmartPasswordUtils"));
    }
}
