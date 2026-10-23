package com.markethub.order.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Integer id;
    private String orderNo;
    private Integer userId;
    private Integer sellerId;
    private java.math.BigDecimal totalAmount;
    private java.math.BigDecimal actualAmount;
    private java.math.BigDecimal freight;
    private Integer status;
    private Integer paymentType;
    private LocalDateTime paymentTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrderItem> items;
    private String statusText;
    private String buyerName;
    private String sellerName;
}
