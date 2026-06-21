package com.lowcode.page.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Slf4j
@Service
public class EmailService {

    public void sendSimpleEmail(String to, String subject, String content) {
        log.info("【模拟发送邮件】文本邮件");
        log.info("收件人: {}", to);
        log.info("主题: {}", subject);
        log.info("内容: {}", content);
        log.info("------------------------");
    }

    public void sendSimpleEmail(List<String> toList, String subject, String content) {
        log.info("【模拟发送邮件】文本邮件");
        log.info("收件人: {}", String.join(", ", toList));
        log.info("主题: {}", subject);
        log.info("内容: {}", content);
        log.info("------------------------");
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("【模拟发送邮件】HTML格式邮件");
        log.info("收件人: {}", to);
        log.info("主题: {}", subject);
        log.info("HTML内容: {}", htmlContent);
        log.info("------------------------");
    }

    public void sendHtmlEmail(List<String> toList, String subject, String htmlContent) {
        log.info("【模拟发送邮件】HTML格式邮件");
        log.info("收件人: {}", String.join(", ", toList));
        log.info("主题: {}", subject);
        log.info("HTML内容: {}", htmlContent);
        log.info("------------------------");
    }

    public void sendEmailWithAttachment(String to, String subject, String content, File attachment) {
        log.info("【模拟发送邮件】带附件邮件");
        log.info("收件人: {}", to);
        log.info("主题: {}", subject);
        log.info("内容: {}", content);
        log.info("附件: {}", attachment.getName());
        log.info("附件大小: {} bytes", attachment.length());
        log.info("------------------------");
    }

    public void sendEmailWithAttachment(List<String> toList, String subject, String content, File attachment) {
        log.info("【模拟发送邮件】带附件邮件");
        log.info("收件人: {}", String.join(", ", toList));
        log.info("主题: {}", subject);
        log.info("内容: {}", content);
        log.info("附件: {}", attachment.getName());
        log.info("附件大小: {} bytes", attachment.length());
        log.info("------------------------");
    }

    public void sendEmailWithAttachment(List<String> toList, List<String> ccList, String subject, String content, File attachment) {
        log.info("【模拟发送邮件】带附件邮件");
        log.info("收件人: {}", String.join(", ", toList));
        if (ccList != null && !ccList.isEmpty()) {
            log.info("抄送: {}", String.join(", ", ccList));
        }
        log.info("主题: {}", subject);
        log.info("内容: {}", content);
        log.info("附件: {}", attachment.getName());
        log.info("附件大小: {} bytes", attachment.length());
        log.info("------------------------");
    }
}
