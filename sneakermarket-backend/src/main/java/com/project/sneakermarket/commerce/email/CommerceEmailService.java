package com.project.sneakermarket.commerce.email;

import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.member.dto.MemberResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class CommerceEmailService {

    private final JavaMailSender sender;

    public void sendOrderCreatedEmail(
            MemberResponse member,
            Order order
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        // 收件人
        message.setTo(member.getEmail());

        // 信件主旨
        message.setSubject("SneakerMarket｜訂單成立通知");

        // 信件內容
        message.setText(
                "您好，" + member.getName() + "！\n\n"
                        + "您的訂單已付款成功並成立。\n\n"
                        + "訂單編號：" + order.getOrderNumber() + "\n"
                        + "訂單金額：NT$" + order.getTotalAmount().toPlainString() + "\n"
                        + "收件人：" + order.getReceiverName() + "\n"
                        + "收件電話：" + order.getReceiverPhone() + "\n"
                        + "收件地址：" + order.getShippingAddress() + "\n\n"
                        + "感謝您的購買！\n"
                        + "SneakerMarket"
        );

        // 寄出 Email
        sender.send(message);
    }
}

