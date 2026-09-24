package com.project.sneakermarket.commerce.linepay;

import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.ApiResult;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.ConfirmInfo;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.PackageItem;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.PaymentRequestBody;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.ProductLine;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.RedirectUrls;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.RequestInfo;
import com.project.sneakermarket.commerce.repository.OrderRepository;
import com.project.sneakermarket.commerce.service.OrderService;
import com.project.sneakermarket.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 商城 LINE Pay 付款流程。
 * 訂單狀態的變更一律交給 OrderService（confirmPayment / cancelOrder），跟綠界走同一套，
 * 這裡只負責「什麼時候該呼叫它們」。
 */
@Service
@RequiredArgsConstructor
public class CommerceLinePayService {

    public static final String PAYMENT_METHOD = "LINE_PAY";

    private final CommerceLinePayGateway gateway;
    private final CommerceLinePayProperties props;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * 訂單建好之後呼叫：向 LINE Pay 申請付款，回傳付款頁網址讓前端跳轉。
     * 申請失敗就直接取消訂單（cancelOrder 會把庫存加回去）。
     */
    public String startPayment(Long memberId, Long orderId) {
        Order order = findMyOrder(memberId, orderId);

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("這張訂單不是待付款狀態");
        }
        if (!PAYMENT_METHOD.equals(order.getPaymentMethod())) {
            throw new RuntimeException("這張訂單的付款方式不是 LINE Pay");
        }

        long amount = toLinePayAmount(order);
        String orderNo = order.getOrderNumber();
        String frontend = props.getFrontendUrl();

        // 整張訂單包成一個品項送出，金額才不會因為運費、折價券對不起來
        ProductLine line = new ProductLine("SneakerMarket 訂單 " + orderNo, 1, amount);
        PackageItem pkg = new PackageItem("PKG-" + orderNo, amount, List.of(line));

        // 訂單 id 放在網址路徑裡，LINE Pay 導回時只會自己附上 transactionId
        RedirectUrls urls = new RedirectUrls(
                frontend + "/linepay/confirm/" + orderId,
                frontend + "/linepay/cancel/" + orderId);

        PaymentRequestBody body = new PaymentRequestBody(
                amount, gateway.currency(), orderNo, List.of(pkg), urls);

        ApiResult<RequestInfo> result;
        try {
            result = gateway.requestPayment(body);
        } catch (RestClientException e) {
            orderService.cancelOrder(orderId);
            throw new RuntimeException("無法連線到 LINE Pay，請稍後再試");
        }

        if (result == null || !result.isSuccess()
                || result.info() == null || result.info().paymentUrl() == null) {
            orderService.cancelOrder(orderId);
            String reason = (result == null) ? "沒有回應" : result.returnMessage();
            throw new RuntimeException("LINE Pay 付款申請失敗：" + reason);
        }

        return result.info().paymentUrl().web();
    }

    /**
     * 使用者在 LINE Pay 付完款、被導回前端確認頁後呼叫：向 LINE Pay 確認扣款。
     */
    public void finishPayment(Long memberId, Long orderId, String transactionId) {
        Order order = findMyOrder(memberId, orderId);

        // 使用者重新整理確認頁時會再打一次，已付款就不要再跟 LINE Pay 確認
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("訂單狀態不正確，無法完成付款");
        }

        Long txId = parseTransactionId(transactionId);
        ApiResult<ConfirmInfo> result = gateway.confirmPayment(txId, toLinePayAmount(order));

        if (result != null && result.isSuccess()) {
            orderService.confirmPayment(orderId);
            return;
        }

        orderService.cancelOrder(orderId);
        String reason = (result == null) ? "沒有回應" : result.returnMessage();
        throw new RuntimeException("LINE Pay 付款確認失敗：" + reason);
    }

    /** 使用者在 LINE Pay 付款頁按取消 → 取消訂單、還原庫存 */
    public void cancelPayment(Long memberId, Long orderId) {
        findMyOrder(memberId, orderId);
        orderService.cancelOrder(orderId);
    }

    // ---------- 以下為內部工具 ----------

    private Order findMyOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到這張訂單"));
        if (!order.getMemberId().equals(memberId)) {
            throw new ForbiddenException("無權操作他人的訂單");
        }
        return order;
    }

    /** LINE Pay 台幣金額只收整數；總金額有小數時 longValueExact 會直接報錯，不會偷偷四捨五入 */
    private long toLinePayAmount(Order order) {
        return order.getTotalAmount().longValueExact();
    }

    /** transactionId 從前端網址傳進來是字串，這裡才轉成數字 */
    private Long parseTransactionId(String transactionId) {
        try {
            return Long.valueOf(transactionId);
        } catch (NumberFormatException | NullPointerException e) {
            throw new RuntimeException("transactionId 格式不正確");
        }
    }
}