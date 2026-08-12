package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public final class CopyWebsiteBrowser {
    public static final String DEFAULT_BROWSER = "";

    private CopyWebsiteBrowser() {
    }

    public static List<BrowserChoice> queryChoices(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Map<String, ResolveInfo> http = query(packageManager, "http:");
        Map<String, ResolveInfo> https = query(packageManager, "https:");
        Map<String, ResolveInfo> declaredBrowsers = queryDeclaredBrowsers(packageManager);
        List<BrowserCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, ResolveInfo> entry : http.entrySet()) {
            ResolveInfo info = entry.getValue();
            String packageName = entry.getKey();
            if ("android".equals(packageName) || context.getPackageName().equals(packageName)) {
                continue;
            }
            if (!declaredBrowsers.containsKey(packageName)) {
                continue;
            }
            CharSequence label = info.loadLabel(packageManager);
            candidates.add(new BrowserCandidate(
                    label == null ? packageName : label.toString(),
                    packageName,
                    true,
                    https.containsKey(packageName)
            ));
        }
        return buildChoices(candidates);
    }

    public static List<BrowserChoice> buildChoices(List<BrowserCandidate> candidates) {
        LinkedHashMap<String, BrowserChoice> unique = new LinkedHashMap<>();
        for (BrowserCandidate candidate : candidates) {
            if (!candidate.supportsHttp() || !candidate.supportsHttps()
                    || candidate.packageName() == null || candidate.packageName().isBlank()) {
                continue;
            }
            unique.putIfAbsent(candidate.packageName(),
                    new BrowserChoice(candidate.label(), candidate.packageName()));
        }
        Collator collator = Collator.getInstance(Locale.getDefault());
        List<BrowserChoice> result = new ArrayList<>(unique.values());
        result.sort((left, right) -> collator.compare(left.label(), right.label()));
        result.add(0, new BrowserChoice("系统默认浏览器", DEFAULT_BROWSER));
        return List.copyOf(result);
    }

    public static String resolveSelectedPackage(
            String packageName,
            Predicate<String> canHandle
    ) {
        if (packageName == null || packageName.isBlank() || DEFAULT_BROWSER.equals(packageName)) {
            return null;
        }
        return canHandle.test(packageName) ? packageName : null;
    }

    public static boolean canHandle(Context context, String packageName) {
        Intent intent = browserIntent("https://www.example.com").setPackage(packageName);
        return !context.getPackageManager().queryIntentActivities(
                intent, PackageManager.MATCH_ALL
        ).isEmpty();
    }

    public static Intent browserIntent(String value) {
        String url = value.startsWith("http://") || value.startsWith("https://")
                ? value : "https://" + value;
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static void applyPackage(Context context, Intent intent, String configuredPackage) {
        intent.setPackage(resolveSelectedPackage(
                configuredPackage, packageName -> canHandle(context, packageName)
        ));
    }

    public static void open(Context context, String url, String configuredPackage) {
        Intent intent = browserIntent(url);
        applyPackage(context, intent, configuredPackage);
        try {
            context.startActivity(intent);
        } catch (RuntimeException first) {
            if (intent.getPackage() == null) {
                throw first;
            }
            intent.setPackage(null);
            context.startActivity(intent);
        }
    }

    private static Map<String, ResolveInfo> query(PackageManager manager, String scheme) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addCategory(Intent.CATEGORY_DEFAULT);
        LinkedHashMap<String, ResolveInfo> result = new LinkedHashMap<>();
        try {
            for (ResolveInfo info : manager.queryIntentActivities(
                    intent, PackageManager.MATCH_ALL | PackageManager.MATCH_DEFAULT_ONLY
            )) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    result.putIfAbsent(info.activityInfo.packageName, info);
                }
            }
        } catch (RuntimeException ignored) {
        }
        return result;
    }

    private static Map<String, ResolveInfo> queryDeclaredBrowsers(PackageManager manager) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER);
        LinkedHashMap<String, ResolveInfo> result = new LinkedHashMap<>();
        try {
            for (ResolveInfo info : manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    result.putIfAbsent(info.activityInfo.packageName, info);
                }
            }
        } catch (RuntimeException ignored) {
        }
        return result;
    }

    public record BrowserCandidate(
            String label,
            String packageName,
            boolean supportsHttp,
            boolean supportsHttps
    ) {
    }

    public record BrowserChoice(String label, String packageName) {
    }
}
