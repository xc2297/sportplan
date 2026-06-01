package org.example.sportplan.service.agent.tool;

import com.alibaba.excel.EasyExcel;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Excel 导出工具基类：封装 "生成 Excel → 上传 COS → 返回下载链接" 的公共流程
// 子类只需实现 generateExcel()（填充数据）和 getFileName()（定义文件名），无需关心上传逻辑
@Slf4j
public abstract class ExportBaseTool implements AgentTool {

    @Value("${cos.secret-id}")
    private String secretId;

    @Value("${cos.secret-key}")
    private String secretKey;

    @Value("${cos.bucket}")
    private String bucket;

    @Value("${cos.region}")
    private String region;

    // COS 自定义域名（用于拼接下载链接，而非直接用 bucket 域名）
    @Value("${cos.domain}")
    private String domain;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 子类实现：查询数据并用 EasyExcel 写入字节数组
    protected abstract byte[] generateExcel(Long userId, Map<String, Object> args) throws Exception;

    // 子类实现：返回导出文件的中文名（如 "排行榜.xlsx"），仅用于 AI 回复展示
    protected abstract String getFileName();

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        try {
            // 1. 子类生成 Excel 字节数据
            byte[] excelData = generateExcel(userId, args);
            // 2. 上传到腾讯云 COS，获取下载链接
            String url = uploadToCOS(excelData, getFileName());
            // 3. 返回下载信息（AI 会自动在回复中展示链接）
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", getFileName());
            result.put("message", "文件已生成，请点击链接下载");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("导出Excel失败: {}", e.getMessage());
            return "{\"error\":\"导出失败：" + e.getMessage() + "\"}";
        }
    }

    // 上传 Excel 到腾讯云 COS 的 sport-exports/ 目录，用 UUID 避免多用户同时导出时文件名冲突
    protected String uploadToCOS(byte[] data, String displayName) {
        String key = "sport-exports/" + UUID.randomUUID().toString() + ".xlsx";

        COSClient cosClient = createCOSClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            cosClient.putObject(bucket, key, bis, metadata);

            String url = "https://" + domain + "/" + key;
            log.info("Excel已上传到COS: {}", url);
            return url;
        } finally {
            cosClient.shutdown();
        }
    }

    // 创建 COS 客户端（每次上传创建新实例，用完在 finally 中关闭）
    private COSClient createCOSClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(credentials, clientConfig);
    }
}
