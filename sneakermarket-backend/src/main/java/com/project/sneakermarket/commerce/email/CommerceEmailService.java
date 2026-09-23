package com.project.sneakermarket.commerce.email;

import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.entity.OrderItem;
import com.project.sneakermarket.commerce.repository.OrderItemRepository;
import com.project.sneakermarket.member.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class CommerceEmailService {

    private final JavaMailSender sender;
    private final OrderItemRepository orderItemRepository;

    public void sendPaymentSuccessEmail(
            MemberResponse member,
            Order order
    ) {

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderOrderId(order.getOrderId());

        StringBuilder itemText = new StringBuilder();

        itemText.append("【訂購商品】\n");

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItem item : orderItems) {

            itemText.append("商品名稱：")
                    .append(item.getProductName())
                    .append("\n");

            itemText.append("規格：")
                    .append(item.getSpecification())
                    .append("\n");

            itemText.append("數量：")
                    .append(item.getQuantity())
                    .append("\n");

            itemText.append("小計：NT$")
                    .append(item.getLineTotal().toPlainString())
                    .append("\n\n");

            subtotal = subtotal.add(item.getLineTotal());
        }

        // ------------------------------------------------------
        // 商品小計 / 運費 / 優惠折抵（有折抵才顯示這行）
        // ------------------------------------------------------

        StringBuilder totalsText = new StringBuilder();

        totalsText.append("商品小計：NT$")
                .append(subtotal.toPlainString())
                .append("\n");

        totalsText.append("運費：NT$")
                .append(order.getShippingFee().toPlainString())
                .append("\n");

        if (order.getDiscountAmount() != null
                && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {

            totalsText.append("優惠折抵：-NT$")
                    .append(order.getDiscountAmount().toPlainString())
                    .append("\n");
        }

        String shippingText;

        if ("CVS".equals(order.getShippingMethod())) {

            shippingText =
                    "收件方式：超商取貨\n"
                            + "取貨門市：" + order.getCvsStoreName() + "\n"
                            + "門市地址：" + order.getCvsAddress();

        } else {

            shippingText =
                    "收件方式：宅配到府\n"
                            + "收件地址：" + order.getShippingAddress();
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(member.getEmail());

        message.setSubject("酷客 Kicks｜付款成功通知");

        message.setText(
                "您好，" + member.getName() + "！\n\n"

                        + "您的訂單已付款成功。\n\n"

                        + "訂單編號："
                        + order.getOrderNumber()
                        + "\n"

                        + "訂單金額：NT$"
                        + order.getTotalAmount().toPlainString()
                        + "\n\n"

                        + itemText

                        + totalsText
                        + "\n"

                        + "【收件資訊】\n"
                        + "收件人："
                        + order.getReceiverName()
                        + "\n"

                        + "收件電話："
                        + order.getReceiverPhone()
                        + "\n"

                        + shippingText
                        + "\n\n"

                        + "感謝您的購買！\n"
                        + "酷客 Kicks"
        );

        sender.send(message);
    }
}
