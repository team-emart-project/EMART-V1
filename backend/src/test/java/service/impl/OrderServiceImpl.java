import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emart.dto.request.CheckoutRequest;
import com.emart.dto.response.OrderResponse;
import com.emart.entity.Orders;
import com.emart.mapper.OrderMapper;
import com.emart.repository.OrdersRepository;
import com.emart.service.OrderService;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrdersRepository ordersRepository,
                            OrderMapper orderMapper) {

        this.ordersRepository = ordersRepository;
        this.orderMapper = orderMapper;
    }

    // =========================
    // 1. Checkout Preview
    // =========================
    @Override
    public OrderResponse checkoutPreview(CheckoutRequest request) {

        // Write preview logic here

        return null;
    }

    // =========================
    // 2. Place Order
    // =========================
    @Override
    public OrderResponse placeOrder(CheckoutRequest request) {

        // Write place order logic here

        return null;
    }

    // =========================
    // 3. Get My Orders
    // =========================
    @Override
    public Page<OrderResponse> getMyOrders(Pageable pageable) {

        return null;
    }

    // =========================
    // 4. Get Order By Id
    // =========================
    @Override
    public OrderResponse getOrder(Integer orderId) {

        return null;
    }

    // =========================
    // 5. Cancel Order
    // =========================
    @Override
    public void cancelOrder(Integer orderId) {

    }

    // =========================
    // 6. Invoice PDF
    // =========================
    @Override
    public byte[] generateInvoicePdf(Integer orderId) {

        return null;
    }

}