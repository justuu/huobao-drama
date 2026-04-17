package com.ark.sample;

import com.volcengine.ark.runtime.model.content.generation.*;
import com.volcengine.ark.runtime.model.content.generation.CreateContentGenerationTaskRequest.Content;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ContentGenerationTaskExample {

    // Client initialization
    static String apiKey = System.getenv("ARK_API_KEY");
    static ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
    static Dispatcher dispatcher = new Dispatcher();
    static ArkService service = ArkService.builder()
            .baseUrl("https://ark.cn-beijing.volces.com/api/v3") // The base URL for model invocation
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .apiKey(apiKey)
            .build();

    public static void main(String[] args) {

        // Model ID
        final String modelId = "doubao-seedance-2-0-260128";
        // Text prompt
        final String prompt = "固定机位，近景镜头，清新自然风格。在室内自然光下，图片1中美妆博主面带笑容，向镜头介绍图片2中的面霜。博主将手里的面霜展示给镜头，开心地说“挖到本命面霜了！”；接着她一边用手指轻轻蘸取面霜展示那种软糯感，一边说“质地像云朵一样软糯，一抹就吸收”；最后她把面霜涂抹在脸颊上，展示着水润透亮的皮肤，同时自信地说“熬夜急救、补水保湿全搞定”。要求画面中人物居中，完整展示人物的整个脑袋和上半身，始终对焦人脸，人脸始终清晰，纯净无任何字幕。";

        // Example resource URLs
        final String refImage1 = "asset://asset-20260401123823-6d4x2";
        final String refImage2 = "https://ark-project.tos-cn-beijing.volces.com/doc_image/r2v_edit_pic1.jpg";

        // Output video parameters
        final boolean generateAudio = true;
        final String videoRatio = "adaptive";
        final long videoDuration = 11L;
        final boolean showWatermark = true;

        System.out.println("----- create request -----");
        // Build request content
        List<Content> contents = new ArrayList<>();

        // 1. Text prompt
        contents.add(Content.builder()
                .type("text")
                .text(prompt)
                .build());

        // 2. Reference image 1
        contents.add(Content.builder()
                .type("image_url")
                .imageUrl(CreateContentGenerationTaskRequest.ImageUrl.builder()
                        .url(refImage1)
                        .build())
                .role("reference_image")
                .build());

        // 3. Reference image 2
        contents.add(Content.builder()
                .type("image_url")
                .imageUrl(CreateContentGenerationTaskRequest.ImageUrl.builder()
                        .url(refImage2)
                        .build())
                .role("reference_image")
                .build());

        // Create video generation task
        CreateContentGenerationTaskRequest createRequest = CreateContentGenerationTaskRequest.builder()
                .generateAudio(generateAudio)
                .model(modelId)
                .content(contents)
                .ratio(videoRatio)
                .duration(videoDuration)
                .watermark(showWatermark)
                .build();

        CreateContentGenerationTaskResult createResult = service.createContentGenerationTask(createRequest);
        System.out.println("Task Created: " + createResult);

        // Get task details and poll status
        String taskId = createResult.getId();
        pollTaskStatus(taskId);
    }

    /**
     * Poll task status
     * 
     * @param taskId Task ID
     */

    private static void pollTaskStatus(String taskId) {
        GetContentGenerationTaskRequest getRequest = GetContentGenerationTaskRequest.builder()
                .taskId(taskId)
                .build();

        System.out.println("----- polling task status -----");
        try {
            while (true) {
                GetContentGenerationTaskResponse getResponse = service.getContentGenerationTask(getRequest);
                String status = getResponse.getStatus();

                if ("succeeded".equalsIgnoreCase(status)) {
                    System.out.println("----- task succeeded -----");
                    System.out.println(getResponse);
                    break;
                } else if ("failed".equalsIgnoreCase(status)) {
                    System.out.println("----- task failed -----");
                    if (getResponse.getError() != null) {
                        System.out.println("Error: " + getResponse.getError().getMessage());
                    }
                    break;
                } else {
                    System.out.printf("Current status: %s, Retrying in 10 seconds...%n", status);
                    TimeUnit.SECONDS.sleep(10);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("Polling interrupted");
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
        } finally {
            service.shutdownExecutor();
        }
    }
}