package com.markethub.review.service;

import com.markethub.common.exception.BusinessException;
import com.markethub.common.exception.R;
import com.markethub.review.entity.Review;
import com.markethub.review.feign.OrderFeignClient;
import com.markethub.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderFeignClient orderFeignClient;

    public List<Review> findByProductId(Integer productId) {
        return reviewMapper.findByProductId(productId);
    }

    /** 当前用户对这件商品已发表过的评价，用于从评价下拉里剔除已评订单。 */
    public List<Review> findMine(Integer userId, Integer productId) {
        return reviewMapper.findByUserAndProduct(userId, productId);
    }

    /** 发表评价：只有确认收货的买家能评价，且必须指明订单。 */
    @Transactional(rollbackFor = Exception.class)
    public void create(Review review) {
        if (review.getProductId() == null || review.getOrderId() == null) {
            throw new BusinessException("商品和订单不能为空");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new BusinessException("评分必须在 1~5 之间");
        }

        if (!isEligible(review.getOrderId(), review.getUserId(), review.getProductId())) {
            throw new BusinessException("只能评价自己已确认收货的订单里的商品");
        }
        if (reviewMapper.existsByUserOrderProduct(review.getUserId(), review.getOrderId(), review.getProductId())) {
            throw new BusinessException("这件商品你已经评价过了");
        }

        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过了上面的检查，靠 (user_id, order_id, product_id) 唯一键兜住
            throw new BusinessException("这件商品你已经评价过了");
        }
        log.info("评价成功: userId={}, orderId={}, productId={}",
                review.getUserId(), review.getOrderId(), review.getProductId());
    }

    private boolean isEligible(Integer orderId, Integer userId, Integer productId) {
        R<Boolean> response = orderFeignClient.reviewEligibility(orderId, userId, productId);
        return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
    }
}
