package com.lowcode.generator.service;

import cn.hutool.core.util.IdUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MobilePreviewService {

    private static final long DEFAULT_EXPIRE_HOURS = 24;
    private static final String PREVIEW_URL_TEMPLATE = "/mobile/preview/%s?appId=%d&pageId=%d&platform=%s";
    private static final String BASE64_PREFIX = "data:image/png;base64,";

    private final ConcurrentHashMap<String, MobilePreview> previewStore = new ConcurrentHashMap<>();
    private final List<DeviceInfo> deviceList = initDeviceList();

    public MobilePreview createPreview(Long appId, Long pageId, String platform) {
        log.info("创建预览会话 - appId: {}, pageId: {}, platform: {}", appId, pageId, platform);

        if (appId == null) {
            throw new IllegalArgumentException("appId 不能为空");
        }
        if (pageId == null) {
            throw new IllegalArgumentException("pageId 不能为空");
        }
        if (!StringUtils.hasText(platform)) {
            throw new IllegalArgumentException("platform 不能为空");
        }

        String previewToken = IdUtil.fastSimpleUUID();
        String previewUrl = String.format(PREVIEW_URL_TEMPLATE, previewToken, appId, pageId, platform);

        MobilePreview preview = new MobilePreview();
        preview.setPreviewToken(previewToken);
        preview.setAppId(appId);
        preview.setPageId(pageId);
        preview.setPlatform(platform);
        preview.setPreviewUrl(previewUrl);
        preview.setCreateTime(LocalDateTime.now());
        preview.setExpireTime(LocalDateTime.now().plusHours(DEFAULT_EXPIRE_HOURS));
        preview.setVisitCount(0);
        preview.setExpired(false);

        try {
            String qrCodeBase64 = generateQRCode(previewUrl, 300, 300, null);
            preview.setQrCodeBase64(qrCodeBase64);
        } catch (Exception e) {
            log.error("生成二维码失败 - previewToken: {}", previewToken, e);
            throw new RuntimeException("生成二维码失败", e);
        }

        previewStore.put(previewToken, preview);
        log.info("预览会话创建成功 - previewToken: {}, expireTime: {}", previewToken, preview.getExpireTime());

        return preview;
    }

    public MobilePreview getPreview(String previewToken) {
        log.debug("获取预览信息 - previewToken: {}", previewToken);

        if (!StringUtils.hasText(previewToken)) {
            throw new IllegalArgumentException("previewToken 不能为空");
        }

        MobilePreview preview = previewStore.get(previewToken);
        if (preview == null) {
            log.warn("预览会话不存在 - previewToken: {}", previewToken);
            throw new IllegalArgumentException("预览会话不存在或已过期");
        }

        if (preview.isExpired() || LocalDateTime.now().isAfter(preview.getExpireTime())) {
            log.warn("预览会话已过期 - previewToken: {}", previewToken);
            previewStore.remove(previewToken);
            throw new IllegalArgumentException("预览会话已过期");
        }

        preview.setVisitCount(preview.getVisitCount() + 1);
        preview.setLastVisitTime(LocalDateTime.now());

        log.debug("预览信息获取成功 - previewToken: {}, visitCount: {}", previewToken, preview.getVisitCount());
        return preview;
    }

    public SimulatorConfig startSimulator(String previewToken) {
        log.info("启动移动端模拟器 - previewToken: {}", previewToken);

        MobilePreview preview = getPreview(previewToken);

        SimulatorConfig config = new SimulatorConfig();
        config.setPreviewToken(previewToken);
        config.setAppId(preview.getAppId());
        config.setPageId(preview.getPageId());
        config.setPlatform(preview.getPlatform());
        config.setPreviewUrl(preview.getPreviewUrl());
        config.setDeviceList(deviceList);
        config.setDefaultDevice(getDefaultDevice(preview.getPlatform()));
        config.setStartTime(LocalDateTime.now());

        log.info("模拟器配置生成成功 - previewToken: {}, defaultDevice: {}", 
                previewToken, config.getDefaultDevice().getName());

        return config;
    }

    public String generateQRCode(String content, int width, int height, String logoPath) {
        log.debug("生成二维码 - content: {}, width: {}, height: {}, logoPath: {}", 
                content, width, height, logoPath);

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("二维码尺寸必须大于0");
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

            if (logoPath != null && !logoPath.isEmpty()) {
                qrImage = addLogoToQRCode(qrImage, logoPath);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            String base64 = BASE64_PREFIX + Base64.getEncoder().encodeToString(imageBytes);
            log.debug("二维码生成成功 - size: {} bytes", imageBytes.length);

            return base64;
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    public List<DeviceInfo> listDevices() {
        log.debug("获取支持的模拟器设备列表 - count: {}", deviceList.size());
        return Collections.unmodifiableList(deviceList);
    }

    public boolean expirePreview(String previewToken) {
        log.info("手动过期预览 - previewToken: {}", previewToken);

        if (!StringUtils.hasText(previewToken)) {
            throw new IllegalArgumentException("previewToken 不能为空");
        }

        MobilePreview preview = previewStore.get(previewToken);
        if (preview == null) {
            log.warn("预览会话不存在 - previewToken: {}", previewToken);
            return false;
        }

        preview.setExpired(true);
        preview.setExpireTime(LocalDateTime.now());
        log.info("预览会话已手动过期 - previewToken: {}", previewToken);

        return true;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupExpiredPreviews() {
        log.info("开始清理过期预览会话 - 当前总数: {}", previewStore.size());

        LocalDateTime now = LocalDateTime.now();
        List<String> expiredTokens = previewStore.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired() || now.isAfter(entry.getValue().getExpireTime()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        expiredTokens.forEach(token -> {
            previewStore.remove(token);
            log.debug("已移除过期预览 - previewToken: {}", token);
        });

        log.info("过期预览会话清理完成 - 清理数量: {}, 剩余数量: {}", 
                expiredTokens.size(), previewStore.size());
    }

    private BufferedImage addLogoToQRCode(BufferedImage qrImage, String logoPath) throws IOException {
        log.debug("添加Logo到二维码 - logoPath: {}", logoPath);

        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();
        int logoSize = Math.min(qrWidth, qrHeight) / 5;
        int logoX = (qrWidth - logoSize) / 2;
        int logoY = (qrHeight - logoSize) / 2;

        BufferedImage logoImage;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(logoPath)) {
            if (is == null) {
                log.warn("Logo文件不存在 - logoPath: {}, 使用默认Logo", logoPath);
                return createDefaultLogo(qrImage, logoSize, logoX, logoY);
            }
            logoImage = ImageIO.read(is);
        }

        if (logoImage == null) {
            log.warn("Logo文件读取失败 - logoPath: {}, 使用默认Logo", logoPath);
            return createDefaultLogo(qrImage, logoSize, logoX, logoY);
        }

        Graphics2D g2 = qrImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(logoX - 5, logoY - 5, logoSize + 10, logoSize + 10, 10, 10);

        Image scaledLogo = logoImage.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);
        g2.drawImage(scaledLogo, logoX, logoY, null);

        g2.dispose();

        log.debug("Logo添加成功");
        return qrImage;
    }

    private BufferedImage createDefaultLogo(BufferedImage qrImage, int logoSize, int logoX, int logoY) {
        Graphics2D g2 = qrImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(logoX - 5, logoY - 5, logoSize + 10, logoSize + 10, 10, 10);

        g2.setColor(new Color(0, 122, 255));
        g2.fillRoundRect(logoX, logoY, logoSize, logoSize, 8, 8);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, logoSize / 2));
        FontMetrics fm = g2.getFontMetrics();
        String text = "M";
        int textX = logoX + (logoSize - fm.stringWidth(text)) / 2;
        int textY = logoY + (logoSize + fm.getAscent()) / 2 - fm.getDescent();
        g2.drawString(text, textX, textY);

        g2.dispose();

        return qrImage;
    }

    private DeviceInfo getDefaultDevice(String platform) {
        if ("ios".equalsIgnoreCase(platform)) {
            return deviceList.stream()
                    .filter(d -> "iPhone 14".equals(d.getName()))
                    .findFirst()
                    .orElse(deviceList.get(0));
        } else if ("android".equalsIgnoreCase(platform)) {
            return deviceList.stream()
                    .filter(d -> "Samsung Galaxy S23".equals(d.getName()))
                    .findFirst()
                    .orElse(deviceList.get(0));
        } else if ("harmony".equalsIgnoreCase(platform)) {
            return deviceList.stream()
                    .filter(d -> "Huawei Mate 60 Pro".equals(d.getName()))
                    .findFirst()
                    .orElse(deviceList.get(0));
        }
        return deviceList.get(0);
    }

    private List<DeviceInfo> initDeviceList() {
        List<DeviceInfo> devices = new ArrayList<>();

        DeviceInfo iPhone14 = new DeviceInfo();
        iPhone14.setId("iphone14");
        iPhone14.setName("iPhone 14");
        iPhone14.setPlatform("ios");
        iPhone14.setWidth(390);
        iPhone14.setHeight(844);
        iPhone14.setResolution("1170 x 2532");
        iPhone14.setPixelRatio(3.0);
        iPhone14.setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        devices.add(iPhone14);

        DeviceInfo iPhone14ProMax = new DeviceInfo();
        iPhone14ProMax.setId("iphone14promax");
        iPhone14ProMax.setName("iPhone 14 Pro Max");
        iPhone14ProMax.setPlatform("ios");
        iPhone14ProMax.setWidth(430);
        iPhone14ProMax.setHeight(932);
        iPhone14ProMax.setResolution("1290 x 2796");
        iPhone14ProMax.setPixelRatio(3.0);
        iPhone14ProMax.setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        devices.add(iPhone14ProMax);

        DeviceInfo samsungS23 = new DeviceInfo();
        samsungS23.setId("samsungs23");
        samsungS23.setName("Samsung Galaxy S23");
        samsungS23.setPlatform("android");
        samsungS23.setWidth(360);
        samsungS23.setHeight(780);
        samsungS23.setResolution("1080 x 2340");
        samsungS23.setPixelRatio(3.0);
        samsungS23.setUserAgent("Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36");
        devices.add(samsungS23);

        DeviceInfo huaweiMate60Pro = new DeviceInfo();
        huaweiMate60Pro.setId("huaweimate60pro");
        huaweiMate60Pro.setName("Huawei Mate 60 Pro");
        huaweiMate60Pro.setPlatform("harmony");
        huaweiMate60Pro.setWidth(422);
        huaweiMate60Pro.setHeight(926);
        huaweiMate60Pro.setResolution("1266 x 2778");
        huaweiMate60Pro.setPixelRatio(3.0);
        huaweiMate60Pro.setUserAgent("Mozilla/5.0 (Linux; HarmonyOS 4.0; LIO-AN00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36");
        devices.add(huaweiMate60Pro);

        DeviceInfo iPadPro11 = new DeviceInfo();
        iPadPro11.setId("ipadpro11");
        iPadPro11.setName("iPad Pro 11");
        iPadPro11.setPlatform("ios");
        iPadPro11.setWidth(834);
        iPadPro11.setHeight(1194);
        iPadPro11.setResolution("1668 x 2388");
        iPadPro11.setPixelRatio(2.0);
        iPadPro11.setUserAgent("Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        devices.add(iPadPro11);

        DeviceInfo tablet = new DeviceInfo();
        tablet.setId("tablet");
        tablet.setName("通用平板");
        tablet.setPlatform("android");
        tablet.setWidth(768);
        tablet.setHeight(1024);
        tablet.setResolution("768 x 1024");
        tablet.setPixelRatio(1.0);
        tablet.setUserAgent("Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
        devices.add(tablet);

        return devices;
    }

    @Data
    public static class MobilePreview {
        private String previewToken;
        private Long appId;
        private Long pageId;
        private String platform;
        private String previewUrl;
        private String qrCodeBase64;
        private LocalDateTime createTime;
        private LocalDateTime expireTime;
        private LocalDateTime lastVisitTime;
        private Integer visitCount;
        private boolean expired;
    }

    @Data
    public static class DeviceInfo {
        private String id;
        private String name;
        private String platform;
        private Integer width;
        private Integer height;
        private String resolution;
        private Double pixelRatio;
        private String userAgent;
    }

    @Data
    public static class SimulatorConfig {
        private String previewToken;
        private Long appId;
        private Long pageId;
        private String platform;
        private String previewUrl;
        private List<DeviceInfo> deviceList;
        private DeviceInfo defaultDevice;
        private LocalDateTime startTime;
    }
}
