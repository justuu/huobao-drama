package com.assets.example;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONObject;

/**
 * Copyright (year) Beijing Volcano Engine Technology Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class CreateAsset_Demo {

    private static final BitSet URLENCODER = new BitSet(256);
    private static final String CONST_ENCODE = "0123456789ABCDEF";
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    // 轮询配置
    private static final long POLL_INTERVAL_MS = 3000; // 3 秒
    private static final long POLL_TIMEOUT_MS = 120000; // 2 分钟

    private final String region;
    private final String service;
    private final String schema;
    private final String host;
    private final String path;
    private final String ak;
    private final String sk;

    static {
        int i;
        for (i = 97; i <= 122; ++i)
            URLENCODER.set(i);
        for (i = 65; i <= 90; ++i)
            URLENCODER.set(i);
        for (i = 48; i <= 57; ++i)
            URLENCODER.set(i);
        URLENCODER.set('-');
        URLENCODER.set('_');
        URLENCODER.set('.');
        URLENCODER.set('~');
    }

    /**
     * 构造函数，初始化必要的请求参数
     */
    public CreateAsset_Demo(String region, String service, String schema, String host, String path, String ak, String sk) {
        this.region = region;
        this.service = service;
        this.host = host;
        this.schema = schema;
        this.path = path;
        this.ak = ak;
        this.sk = sk;
    }

public static void main(String[] args) throws Exception {
        // TODO: 替换为真实的 AK/SK
        String AccessKeyID = "your_access_key_id";
        String SecretAccessKey = "your_secret_access_key";

        String endpoint = "ark.cn-beijing.volcengineapi.com";
        String path = "/";
        String service = "ark";
        String region = "cn-beijing";
        String schema = "https";
        CreateAsset_Demo sign = new CreateAsset_Demo(region, service, schema, endpoint, path, AccessKeyID, SecretAccessKey);

        // 1. 创建资产
        String createAction = "CreateAsset";
        String version = "2