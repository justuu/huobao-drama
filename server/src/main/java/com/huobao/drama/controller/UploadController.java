package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    @Value("${storage.local-path}")
    private String localPath;

    @Value("${storage.base-url}")
    private String baseUrl;

    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Path storageDir = Paths.get(localPath).toAbsolutePath().normalize();
            Files.createDirectories(storageDir);

            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : ".jpg";
            String filename = System.currentTimeMillis() + "_" + (int)(Math.random() * 100000) + ext;

            Path dest = storageDir.resolve(filename);
            file.transferTo(dest.toFile());

            Map<String, String> result = new LinkedHashMap<>();
            result.put("url", "/static/" + filename);
            result.put("path", dest.toString());
            return ApiResponse.ok(result);
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
