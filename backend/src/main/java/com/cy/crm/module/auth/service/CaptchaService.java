package com.cy.crm.module.auth.service;

import com.github.bingoohuang.patchca.custom.ConfigurableCaptchaService;
import com.github.bingoohuang.patchca.filter.predefined.CurvesRippleFilterFactory;
import com.github.bingoohuang.patchca.font.RandomFontFactory;
import com.github.bingoohuang.patchca.word.RandomWordFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务
 */
@Slf4j
@Service
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${captcha.expire:300}")
    private long expireSeconds;

    private final ConfigurableCaptchaService captchaService = createCaptchaService();

    /**
     * 创建验证码服务配置
     */
    private ConfigurableCaptchaService createCaptchaService() {
        ConfigurableCaptchaService service = new ConfigurableCaptchaService();

        // 设置随机字体
        RandomFontFactory fontFactory = new RandomFontFactory();
        fontFactory.setMinSize(20);
        fontFactory.setMaxSize(28);
        service.setFontFactory(fontFactory);

        // 设置随机字符
        RandomWordFactory wordFactory = new RandomWordFactory();
        wordFactory.setMinLength(CODE_LENGTH);
        wordFactory.setMaxLength(CODE_LENGTH);
        wordFactory.setCharacters("ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789");
        service.setWordFactory(wordFactory);

        // 设置图片尺寸
        service.setWidth(WIDTH);
        service.setHeight(HEIGHT);

        // 设置滤镜效果（使用曲线波纹效果）
        service.setFilterFactory(new CurvesRippleFilterFactory());

        // 设置颜色
        service.setColorFactory(new RandomColorFactory());

        return service;
    }

    /**
     * 生成验证码
     * @param uuid 验证码唯一标识
     * @return 验证码图片的Base64编码
     */
    public String generateCaptcha(String uuid) {
        // 生成验证码图片
        String imageBase64 = generateCaptchaImage();

        // 获取生成的验证码文本
        String code = captchaService.getCaptcha().getWord();

        // 保存到Redis（如果可用）
        if (redisTemplate != null) {
            String key = CAPTCHA_PREFIX + uuid;
            redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
            log.debug("Captcha generated for uuid: {}, code: {}", uuid, code);
        } else {
            log.warn("Redis unavailable, captcha validation will be disabled");
        }

        return imageBase64;
    }

    /**
     * 生成验证码图片并返回Base64编码
     */
    private String generateCaptchaImage() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(captchaService.getCaptcha().getImage(), "PNG", baos);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate captcha image", e);
            return "";
        }
    }

    /**
     * 验证验证码
     * @param uuid 验证码唯一标识
     * @param inputCode 用户输入的验证码
     * @return 是否验证成功
     */
    public boolean validateCaptcha(String uuid, String inputCode) {
        if (uuid == null || inputCode == null) {
            return false;
        }

        // Redis 不可用时，跳过验证码验证（开发环境）
        if (redisTemplate == null) {
            log.debug("Redis unavailable, skipping captcha validation");
            return true;
        }

        String key = CAPTCHA_PREFIX + uuid;
        String storedCode = (String) redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }

        // 验证后删除验证码（一次性使用）
        redisTemplate.delete(key);

        return storedCode.equalsIgnoreCase(inputCode);
    }

    /**
     * 随机颜色工厂
     */
    private static class RandomColorFactory implements com.github.bingoohuang.patchca.color.ColorFactory {
        private final Random random = new Random();

        @Override
        public Color getColor(int x) {
            // 生成较深的颜色用于文字
            return new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100));
        }
    }
}
