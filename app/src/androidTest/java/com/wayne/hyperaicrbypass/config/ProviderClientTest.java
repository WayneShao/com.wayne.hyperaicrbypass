package com.wayne.hyperaicrbypass.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProviderClientTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final Uri uri = Uri.parse("content://" + ConfigContract.AUTHORITY);

    @Test
    public void moduleCanMutateReadAndRescanThroughStableProvider() {
        Bundle reset = new Bundle();
        reset.putBoolean(ConfigContract.KEY_SELECTED, true);
        assertNotNull(context.getContentResolver().call(
                uri, ConfigContract.METHOD_SET_ALL, null, reset));

        Bundle change = new Bundle();
        change.putString(ConfigContract.KEY_POLICY, Policy.CHARGING.getKey());
        change.putBoolean(ConfigContract.KEY_SELECTED, false);
        context.getContentResolver().call(uri, ConfigContract.METHOD_SET_POLICY, null, change);
        context.getContentResolver().call(uri, ConfigContract.METHOD_RESCAN, null, null);

        Bundle snapshot = context.getContentResolver().call(
                uri, ConfigContract.METHOD_GET_SNAPSHOT, null, null);
        assertNotNull(snapshot);
        assertFalse(snapshot.getBoolean(ConfigContract.policyKey(Policy.CHARGING)));
        assertTrue(snapshot.getBoolean(ConfigContract.policyKey(Policy.TEMPERATURE)));
        assertTrue(snapshot.getLong(ConfigContract.KEY_CONFIG_REVISION) > 0);
        assertTrue(snapshot.getLong(ConfigContract.KEY_RESCAN_GENERATION) > 0);
    }

    @Test
    public void clientRetainsLastSnapshotWhenRefreshFails() {
        ConfigClient client = new ConfigClient(context);
        client.refresh();
        BypassConfig before = client.snapshot();

        client.acceptSnapshotForTest(new Bundle());

        assertEquals(before, client.snapshot());
        client.close();
    }

    @Test
    public void clientWorksDuringAttachBeforeApplicationContextIsPublished() {
        Context earlyAttachContext = new ContextWrapper(context) {
            @Override
            public Context getApplicationContext() {
                return null;
            }
        };

        ConfigClient client = new ConfigClient(earlyAttachContext);

        assertNotNull(client.snapshot());
        client.close();
    }
}
