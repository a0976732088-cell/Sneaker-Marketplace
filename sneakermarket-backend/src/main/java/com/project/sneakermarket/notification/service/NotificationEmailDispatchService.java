package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class NotificationEmailDispatchService {

    private static final ZoneId TAIPEI =
            ZoneId.of("Asia/Taipei");

    private final NotificationEmailJobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${notification.email.from:}")
    private String mailFrom;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    // 與建立站內通知的 Transaction 分開
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendJob(Long jobId) {
        NotificationEmailJob job = jobRepository
                .findByIdForUpdate(jobId)
                .orElse(null);

        if (job == null) {
            return;
        }

        if (job.getStatus() != NotificationEmailStatus.PENDING
                && job.getStatus() != NotificationEmailStatus.RETRY_WAIT) {
            return;
        }

        if (job.getStatus() == NotificationEmailStatus.RETRY_WAIT
                && (job.getNextRetryAt() == null
                || job.getNextRetryAt().isAfter(LocalDateTime.now(TAIPEI)))) {
            return;
        }

        Notification notification = notificationRepository
                .findById(job.getNotificationId())
                .orElse(null);

        if (notification == null) {
            markFailed(job);
            return;
        }


        try {
            if (mailFrom == null || mailFrom.isBlank()) {
                throw new IllegalStateException(
                        "未設定通知 Email 寄件地址"
                );
            }

            String subject = "酷客 Kicks - "
                    + notification.getTitle().replaceAll("[\\r\\n]+", " ");


            boolean isEvent = "EVENT".equals(notification.getTargetType());
            String destinationUrl = frontendBaseUrl.replaceAll("/+$", "")
                    + (isEvent ? "/calendar" : "/");
            String linkLabel = isEvent ? "開啟行事曆" : "前往酷客 Kicks";

            String safeTitle = HtmlUtils.htmlEscape(notification.getTitle());
            String safeContent = HtmlUtils.htmlEscape(notification.getContent());
            String safeUrl = HtmlUtils.htmlEscape(destinationUrl);
            String safeLinkLabel = HtmlUtils.htmlEscape(linkLabel);

            String html = """
        <!doctype html>
        <html>
          <body style="margin:0;padding:32px 12px;background:#f5f7fa;
                       font-family:Arial,'Microsoft JhengHei',sans-serif;
                       color:#1f2937;">

            <table role="presentation"
                   cellpadding="0"
                   cellspacing="0"
                   style="width:100%%;max-width:600px;margin:0 auto;
                          background:#ffffff;border-radius:14px;">

              <tr>
                <td style="padding:36px 38px;">

                  <h1 style="margin:0 0 28px;
                             font-size:24px;
                             line-height:1.4;">
                    酷客 Kicks - %s
                  </h1>

                  <p style="margin:0 0 28px;
                            font-size:16px;
                            line-height:1.8;">
                    %s
                  </p>

                  <p style="margin:0 0 12px;
                            font-size:15px;
                            font-weight:bold;">
                    查看活動：
                  </p>

                  <a href="%s"
                     style="display:inline-block;
                            padding:12px 20px;
                            background:#26313d;
                            color:#ffffff;
                            text-decoration:none;
                            border-radius:6px;
                            font-size:14px;
                            font-weight:bold;">
                    開啟行事曆
                  </a>

                  <div style="margin-top:38px;
                              border-top:1px solid #dfe3e8;
                              padding-top:20px;">

                    <table role="presentation"
                           cellpadding="0"
                           cellspacing="0"
                           style="width:100%%;">

                      <tr>
                        <td style="width:48%%;
                                   padding-right:22px;
                                   vertical-align:middle;">

                          <img src="cid:brandLogo"
                               alt="酷客 Kicks"
                               width="300"
                               style="display:block;
                                      width:100%%;
                                      max-width:220px;
                                      height:auto;
                                      border:0;">
                        </td>

                        <td style="vertical-align:middle;
                                   text-align:center;
                                   color:#111827;">

                          <p style="margin:0 0 14px;
                                    font-size:18px;
                                    font-weight:bold;
                                    line-height:1.5;">
                            ※ 防詐騙提醒 ※
                          </p>

                          <p style="margin:0;
                                    font-size:14px;
                                    line-height:1.8;
                                    color:#374151;">
                            酷客 Kicks 不會透過 Email
                            要求您提供密碼、驗證碼或匯款。<br>
                            如收到可疑訊息，請勿點擊不明連結。
                          </p>
                        </td>
                      </tr>

                    </table>
                  </div>

                </td>
              </tr>
            </table>

          </body>
        </html>
        """.formatted(
                    safeTitle,
                    safeContent,
                    safeUrl,
                    safeLinkLabel
            );

            String plainText =
                    "酷客 Kicks - " + notification.getTitle()
                            + "\n\n"
                            + notification.getContent()
                            + "\n\n查看詳情："
                            + destinationUrl
                            + "\n\n※ 防詐騙提醒 ※"
                            + "\n酷客 Kicks 不會透過 Email"
                            + "要求您提供密碼、驗證碼或匯款。"
                            + "\n如收到可疑訊息，請勿點擊不明連結。";

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(job.getRecipientEmail());
            helper.setSubject(subject);
            helper.setText(plainText, html);

            helper.addInline(
                    "brandLogo",
                    new ClassPathResource(
                            "logoPhoto/newLogo_rectangle.png"
                    ),
                    "image/png"
            );

            mailSender.send(mimeMessage);

            job.setStatus(NotificationEmailStatus.SENT);
            job.setSentAt(LocalDateTime.now(TAIPEI));
            job.setLastError(null);
            job.setNextRetryAt(null);

        } catch (Exception exception) {
            // 不把 SMTP 例外原文寫入資料庫，避免洩漏憑證
            markFailed(job);
        }

        jobRepository.save(job);
    }

    private void markFailed(NotificationEmailJob job) {
        int failedAttempts = job.getRetryCount() + 1;
        job.setRetryCount(failedAttempts);
        job.setLastError("Email 準備或寄送失敗，請檢查後端設定與日誌");

        if (failedAttempts >= 3) {
            job.setStatus(NotificationEmailStatus.FAILED);
            job.setNextRetryAt(null);
            return;
        }

        job.setStatus(NotificationEmailStatus.RETRY_WAIT);
        long delayMinutes = failedAttempts == 1 ? 1 : 5;
        job.setNextRetryAt(LocalDateTime.now(TAIPEI).plusMinutes(delayMinutes));
    }
}
