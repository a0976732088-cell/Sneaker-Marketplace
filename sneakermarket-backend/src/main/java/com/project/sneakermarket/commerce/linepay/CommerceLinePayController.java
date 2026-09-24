package com.project.sneakermarket.commerce.linepay;

import com.project.sneakermarket.commerce.dto.OrderDetailResponse;
import com.project.sneakermarket.commerce.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 商城 LINE Pay 的三支 API，前端依序使用：
 * 1. 建好訂單後 → /request 拿付款頁網址並跳轉
 * 2. 付款完被導回 → /confirm 完成扣款
 * 3. 在付款頁按取消被導回 → /cancel 取消訂單
 */
@RestController
@RequiredArgsConstructor
public class CommerceLinePayController {

    private final CommerceLinePayService linePayService;
    private final OrderService orderService;

    /** 前端 confirm 時送來的 body；transactionId 用字串接，避免 19 位數在前端被 JS 弄壞 */
    public record ConfirmRequest(String transactionId) {
    }

    @PostMapping("/api/orders/{orderId}/linepay/request")
    public Map<String, String> requestPayment(@PathVariable Long orderId,
                                              HttpServletRequest request) {
        Long memberId = currentMemberId(request);
        String paymentUrl = linePayService.startPayment(memberId, orderId);
        return Map.of("paymentUrl", paymentUrl);
    }

    @PostMapping("/api/orders/{orderId}/linepay/confirm")
    public OrderDetailResponse confirmPayment(@PathVariable Long orderId,
                                              @RequestBody ConfirmRequest body,
                                              HttpServletRequest request) {
        Long memberId = currentMemberId(request);
        linePayService.finishPayment(memberId, orderId, body.transactionId());
        // 付款完成後直接回傳最新的訂單內容，前端可以直接顯示結果
        return orderService.getOrderDetail(memberId, orderId);
    }

    @PostMapping("/api/orders/{orderId}/linepay/cancel")
    public OrderDetailResponse cancelPayment(@PathVariable Long orderId,
                                             HttpServletRequest request) {
        Long memberId = currentMemberId(request);
        linePayService.cancelPayment(memberId, orderId);
        return orderService.getOrderDetail(memberId, orderId);
    }

    /** 從 Session 取出登入會員；沒登入就回 401 */
    private Long currentMemberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object memberId = (session == null) ? null : session.getAttribute("memberId");
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "尚未登入");
        }
        return (Long) memberId;
    }
}