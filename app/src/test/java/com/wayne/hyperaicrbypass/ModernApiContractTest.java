package com.wayne.hyperaicrbypass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ModernApiContractTest {
    @Test
    public void moduleEntryUsesModernApi102() {
        Class<?> entry;
        try {
            entry = Class.forName("com.wayne.hyperaicrbypass.MainHook");
        } catch (Throwable ignored) {
            entry = null;
        }

        assertNotNull("modern module entry should load", entry);
        assertEquals("io.github.libxposed.api.XposedModule", entry.getSuperclass().getName());
    }

    @Test
    public void modernMetadataDeclaresStaticRecommendedScope() throws Exception {
        List<String> moduleProperties = resourceLines("META-INF/xposed/module.prop");
        assertEquals(List.of(
                "minApiVersion=102",
                "targetApiVersion=102",
                "staticScope=true"
        ), moduleProperties);
        assertEquals(List.of(
                "com.miui.gallery",
                "com.xiaomi.aicr",
                "com.xiaomi.aiservice"
        ), resourceLines("META-INF/xposed/scope.list"));
    }

    private static List<String> resourceLines(String name) throws Exception {
        InputStream stream = ModernApiContractTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull("missing resource " + name, stream);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> !line.isBlank()).toList();
        }
    }
}
