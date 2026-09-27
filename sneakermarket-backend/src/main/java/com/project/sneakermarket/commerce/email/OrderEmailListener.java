package com.project.sneakermarket.commerce.email;

import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.repository.OrderRepository;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEmailListener {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final CommerceEmailService commerceEmailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        // 專案沒有開 @EnableAsync（@Async 會被忽略），這裡自己丟到背景執行，
        // 付款確認 API 不用等寄信完成就能回應前端
        CompletableFuture.runAsync(() -> {
            try {
                Order order = orderRepository.findById(event.getOrderId())
                        .orElseThrow(() -> new RuntimeException("找不到這張訂單"));

                MemberResponse member = memberService.getMember(event.getMemberId());

                commerceEmailService.sendPaymentSuccessEmail(member, order);

            } catch (Exception exception) {
                log.error("付款成功通知信背景寄送失敗，orderId={}", event.getOrderId(), exception);
            }
        });
    }
}