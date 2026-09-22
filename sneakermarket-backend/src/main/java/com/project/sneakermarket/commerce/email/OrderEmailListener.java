package com.project.sneakermarket.commerce.email;

import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.repository.OrderRepository;
import com.project.sneakermarket.member.dto.MemberResponse;
import com.project.sneakermarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEmailListener {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final CommerceEmailService commerceEmailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("找不到這張訂單"));

        MemberResponse member = memberService.getMember(event.getMemberId());

        commerceEmailService.sendPaymentSuccessEmail(member, order);
    }
}