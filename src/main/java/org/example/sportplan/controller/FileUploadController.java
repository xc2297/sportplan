package org.example.sportplan.controller;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传控制器
 * 将图片上传到腾讯云 COS 对象存储（微信云托管），返回可直接访问的 URL。
 * COS 配置信息从 application.yaml 中读取。
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileUploadController {

    /** COS SecretId，从配置文件读取 */
    @Value("${cos.secret-id}")
    private String secretId;

    /** COS SecretKey，从配置文件读取 */
    @Value("${cos.secret-key}")
    private String secretKey;

    /** COS 存储桶名称 */
    @Value("${cos.bucket}")
    private String bucket;

    /** COS 地域 */
    @Value("${cos.region}")
    private String region;

    /** COS 安全域名，用于拼接返回的文件访问 URL */
    @Value("${cos.domain}")
    private String domain;

    /** 允许的图片文件最大大小（5MB） */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传图片文件到腾讯云 COS
     * 生成唯一文件名，上传到 COS 的 sport-images/ 目录下，返回完整的访问 URL。
     *
     * @param file 上传的图片文件（multipart/form-data）
     * @return 图片的完整访问 URL
     * @throws BusinessException 当文件为空、文件过大或格式不支持时抛出
     */
    @PostMapping("/image")
    public ApiResponse<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件不为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }

        // 生成唯一文件名，保留原始扩展名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "sport-images/" + UUID.randomUUID().toString() + ext;

        // 创建 COS 客户端
        COSClient cosClient = createCOSClient();

        try (InputStream inputStream = file.getInputStream()) {
            // 设置对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);

            // 上传文件到 COS
            cosClient.putObject(bucket, fileName, inputStream, metadata);

            // 拼接并返回完整的访问 URL
            String url = "https://" + domain + "/" + fileName;
            return ApiResponse.success(url);
        } catch (IOException e) {
            throw new BusinessException("图片上传失败：" + e.getMessage());
        } finally {
            // 每次请求后关闭客户端，避免连接泄漏
            cosClient.shutdown();
        }
    }

    /**
     * 创建 COS 客户端实例
     * 使用配置文件中的 SecretId/SecretKey 和地域信息初始化。
     *
     * @return COSClient 实例
     */
    private COSClient createCOSClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(credentials, clientConfig);
    }
}
