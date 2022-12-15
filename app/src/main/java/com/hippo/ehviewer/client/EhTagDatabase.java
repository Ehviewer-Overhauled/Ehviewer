/*
 * Copyright 2019 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.HashCodeUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class EhTagDatabase {

    private static final Map<String, String> NAMESPACE_TO_PREFIX = new HashMap<>();
    // TODO more lock for different language
    private static final Lock lock = new ReentrantLock();
    private static volatile EhTagDatabase instance;

    static {
        NAMESPACE_TO_PREFIX.put("artist", "a:");
        NAMESPACE_TO_PREFIX.put("cosplayer", "cos:");
        NAMESPACE_TO_PREFIX.put("character", "c:");
        NAMESPACE_TO_PREFIX.put("female", "f:");
        NAMESPACE_TO_PREFIX.put("group", "g:");
        NAMESPACE_TO_PREFIX.put("language", "l:");
        NAMESPACE_TO_PREFIX.put("male", "m:");
        NAMESPACE_TO_PREFIX.put("mixed", "x:");
        NAMESPACE_TO_PREFIX.put("other", "o:");
        NAMESPACE_TO_PREFIX.put("parody", "p:");
        NAMESPACE_TO_PREFIX.put("reclass", "r:");
    }

    private final String name;
    private final JSONObject tags;
    private final ArrayList<Pair<String, String>> tagList;

    public EhTagDatabase(String name, BufferedSource source) throws IOException {
        this.name = name;
        JSONObject tmpTags = null;
        try {
            tmpTags = new JSONObject(source.readString(StandardCharsets.UTF_8));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tags = tmpTags;
        if (tags != null) {
            tagList = new ArrayList<>();
            tags.keys().forEachRemaining(k -> {
                try {
                    tagList.add(new Pair<>(k, tags.getString(k)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } else {
            tagList = null;
        }
    }

    @Nullable
    public static EhTagDatabase getInstance() {
        return instance;
    }

    @Nullable
    public static String namespaceToPrefix(String namespace) {
        return NAMESPACE_TO_PREFIX.get(namespace);
    }

    private static String[] getMetadata(Context context) {
        String[] metadata = context.getResources().getStringArray(R.array.tag_translation_metadata);
        if (metadata.length == 4) {
            return metadata;
        } else {
            return null;
        }
    }

    public static boolean isTranslatable(Context context) {
        return context.getResources().getBoolean(R.bool.tag_translatable);
    }

    @Nullable
    private static String getFileContent(File file) {
        try (BufferedSource source = Okio.buffer(Okio.source(file))) {
            return source.readString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private static String getFileSha1(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            int n;
            byte[] buffer = new byte[4 * 1024];
            while ((n = is.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            return HashCodeUtils.bytesToHexString(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static boolean checkData(File sha1File, File dataFile) {
        var s1 = getFileContent(sha1File);
        if (s1 == null) {
            return false;
        }

        var s2 = getFileSha1(dataFile);
        if (s2 == null) {
            return false;
        }

        return s1.equals(s2);
    }

    private static boolean save(OkHttpClient client, String url, File file) {
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                return false;
            }
            ResponseBody body = response.body();

            try (InputStream is = body.byteStream(); OutputStream os = new FileOutputStream(file)) {
                IOUtils.copy(is, os);
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            ExceptionUtils.throwIfFatal(t);
            return false;
        }
    }

    public static void update(Context context) {
        String[] urls = getMetadata(context);
        if (urls == null || urls.length != 4) {
            // Clear tags if it's not possible
            instance = null;
            return;
        }

        String sha1Name = urls[0];
        String sha1Url = urls[1];
        String dataName = urls[2];
        String dataUrl = urls[3];

        // Clear tags if name if different
        EhTagDatabase tmp = instance;
        if (tmp != null && !tmp.name.equals(dataName)) {
            instance = null;
        }

        IoThreadPoolExecutor.getInstance().execute(() -> {
            if (!lock.tryLock()) {
                return;
            }

            try {
                File dir = AppConfig.getFilesDir("tag-translations");
                if (dir == null) {
                    return;
                }

                // Check current sha1 and current data
                File sha1File = new File(dir, sha1Name);
                File dataFile = new File(dir, dataName);
                if (!checkData(sha1File, dataFile)) {
                    FileUtils.delete(sha1File);
                    FileUtils.delete(dataFile);
                }

                // Read current EhTagDatabase
                if (instance == null && dataFile.exists()) {
                    try (BufferedSource source = Okio.buffer(Okio.source(dataFile))) {
                        instance = new EhTagDatabase(dataName, source);
                    } catch (IOException e) {
                        FileUtils.delete(sha1File);
                        FileUtils.delete(dataFile);
                    }
                }

                OkHttpClient client = EhApplication.getOkHttpClient();

                // Save new sha1
                File tempSha1File = new File(dir, sha1Name + ".tmp");
                if (!save(client, sha1Url, tempSha1File)) {
                    FileUtils.delete(tempSha1File);
                    return;
                }

                // Check new sha1 and current data
                if (checkData(tempSha1File, dataFile)) {
                    // The data is the same
                    FileUtils.delete(tempSha1File);
                    return;
                }

                // Save new data
                File tempDataFile = new File(dir, dataName + ".tmp");
                if (!save(client, dataUrl, tempDataFile)) {
                    FileUtils.delete(tempDataFile);
                    return;
                }

                // Check new sha1 and new data
                if (!checkData(tempSha1File, tempDataFile)) {
                    FileUtils.delete(tempSha1File);
                    FileUtils.delete(tempDataFile);
                    return;
                }

                // Replace current sha1 and current data with new sha1 and new data
                FileUtils.delete(sha1File);
                FileUtils.delete(dataFile);
                tempSha1File.renameTo(sha1File);
                tempDataFile.renameTo(dataFile);

                // Read new EhTagDatabase
                try (BufferedSource source = Okio.buffer(Okio.source(dataFile))) {
                    instance = new EhTagDatabase(dataName, source);
                } catch (IOException e) {
                    // Ignore
                }
            } finally {
                lock.unlock();
            }
        });
    }

    public String getTranslation(String tag) {
        try {
            return tags.getString(tag);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Pair<String, String>> suggest(String keyword, boolean translate) {
        ArrayList<Pair<String, String>> searchHints = new ArrayList<>();
        for (int i = 0; i < tagList.size(); i++) {
            var tmp = tagList.get(i);
            String tag = tmp.first;
            String hint = translate ? tmp.second : null;
            int index = tag.indexOf(':');
            boolean keywordMatches;
            if (index == -1 || index >= tag.length() - 1 || keyword.length() > 2) {
                keywordMatches = containsIgnoreSpace(tag, keyword);
            } else {
                keywordMatches = containsIgnoreSpace(tag.substring(index + 1), keyword);
            }

            if (keywordMatches || containsIgnoreSpace(hint, keyword)) {
                Pair<String, String> pair = new Pair<>(hint, tag);
                if (!searchHints.contains(pair)) {
                    searchHints.add(pair);
                }
            }
            if (searchHints.size() > 20) {
                break;
            }
        }
        return searchHints;
    }

    private boolean containsIgnoreSpace(String text, String key) {
        return text != null && text.replace(" ", "").contains(key.replace(" ", ""));
    }
}
