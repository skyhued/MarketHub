package com.markethub.product.service;

import com.markethub.common.exception.BusinessException;
import com.markethub.common.security.CurrentUser;
import com.markethub.product.entity.Category;
import com.markethub.product.entity.Product;
import com.markethub.product.mapper.CategoryMapper;
import com.markethub.product.mapper.ProductMapper;
import com.markethub.product.mapper.StockLedgerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final StockLedgerMapper stockLedgerMapper;

    /* 账本里的操作类型，唯一键是 (order_no, product_id, op) */
    private static final String OP_DEDUCT = "DEDUCT";
    private static final String OP_RESTORE = "RESTORE";
    private static final String OP_SALES_INC = "SALES_INC";
    private static final String OP_SALES_DEC = "SALES_DEC";

    /** 带缓存地按 id 取商品；必须由 controller 直接调用，自调用会绕过缓存代理。 */
    @Cacheable(value = "product", key = "#id")
    public Product findById(Integer id) {
        return productMapper.findById(id);
    }

    /** 详情页可见性：在售商品谁都能看；未上架只有卖家本人和管理员能看。 */
    public Product requireVisible(Product product, Integer viewerId) {
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (isOnSale(product) || CurrentUser.isAdmin() || Objects.equals(product.getUserId(), viewerId)) {
            return product;
        }
        throw new BusinessException(404, "商品不存在");
    }

    /** 在售 = 已过审且上架，和 ProductMapper 里扣库存的条件保持一致 */
    private boolean isOnSale(Product product) {
        return Integer.valueOf(1).equals(product.getStatus())
                && Integer.valueOf(1).equals(product.getAuditStatus());
    }

    public List<Product> findAll() {
        return productMapper.findAll();
    }

    public List<Product> findOnSale() {
        return productMapper.findOnSale();
    }

    public List<Product> findForAdmin(Integer auditStatus) {
        return productMapper.findForAdmin(auditStatus);
    }

    public List<Product> findByUserId(Integer userId) {
        return productMapper.findByUserId(userId);
    }

    public List<Product> search(String keyword, Integer categoryId) {
        return productMapper.search(keyword, categoryId);
    }

    public String findSellerName(Integer userId) {
        return userId == null ? null : productMapper.findSellerName(userId);
    }

    /** 收回商家资格时下架该卖家全部在售商品，可重复调用。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", allEntries = true)
    public int takeDownBySeller(Integer sellerId) {
        int count = productMapper.takeDownBySeller(sellerId);
        log.info("按卖家下架商品: sellerId={}, count={}", sellerId, count);
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#product.id")
    public void create(Product product) {
        product.setStatus(0);
        product.setAuditStatus(0);
        product.setSalesCount(0);
        product.setViewCount(0);
        productMapper.insert(product);
        log.info("商品创建成功: productId={}, name={}", product.getId(), product.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#product.id")
    public void update(Product product, Integer operatorId) {
        requireOwned(product.getId(), operatorId);
        // 审核状态、上下架、销量、归属只能走专门的接口改，不能通过编辑商品提交
        product.setUserId(null);
        product.setStatus(null);
        product.setAuditStatus(null);
        product.setSalesCount(null);
        product.setViewCount(null);
        // 库存也不能直接覆盖：stock 里已经扣掉了待付款订单占用的部分，
        // 商家看到 0 就"改回 1"会让同一件货被卖两次。补货走 restock 只增不减。
        product.setStock(null);
        productMapper.update(product);
        log.info("商品更新成功: productId={}", product.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#id")
    public void delete(Integer id, Integer operatorId) {
        requireOwned(id, operatorId);
        productMapper.delete(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#id")
    public void toggleStatus(Integer id, Integer operatorId) {
        Product product = requireOwned(id, operatorId);
        boolean goingOnSale = !Integer.valueOf(1).equals(product.getStatus());
        if (goingOnSale && !Integer.valueOf(1).equals(product.getAuditStatus())) {
            throw new BusinessException("商品未通过审核，不能上架");
        }
        Product change = new Product();
        change.setId(id);
        change.setStatus(goingOnSale ? 1 : 0);
        productMapper.update(change);
    }

    /** 管理员不受归属限制。 */
    private Product requireOwned(Integer id, Integer operatorId) {
        if (id == null) throw new BusinessException("商品ID不能为空");
        Product product = productMapper.findById(id);
        if (product == null) throw new BusinessException("商品不存在");
        if (!CurrentUser.isAdmin() && !Objects.equals(product.getUserId(), operatorId)) {
            throw new BusinessException(403, "只能操作自己发布的商品");
        }
        return product;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productId")
    public void audit(Integer productId, Integer auditStatus) {
        Product product = productMapper.findById(productId);
        if (product == null) throw new BusinessException("商品不存在");
        product.setAuditStatus(auditStatus);
        product.setStatus(auditStatus == 1 ? 1 : 0);
        productMapper.update(product);
        log.info("商品审核: productId={}, auditStatus={}", productId, auditStatus);
    }

    public List<Category> findAllCategories() {
        return categoryMapper.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Integer productId) {
        productMapper.updateViewCount(productId);
    }

    /* 内部接口：库存与销量，只由 order-service 调用；四个方法都靠 stock_ledger 幂等。 */

    /** 记一笔变更；返回 true 表示这笔记过了，调用方应当跳过整段逻辑 */
    private boolean alreadyApplied(String orderNo, Integer productId, String op, Integer quantity) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("订单号不能为空");
        }
        if (stockLedgerMapper.tryRecord(orderNo, productId, op, quantity) == 0) {
            log.info("该操作已生效过，跳过: orderNo={}, productId={}, op={}", orderNo, productId, op);
            return true;
        }
        return false;
    }

    /** 扣库存用条件更新防超卖；影响 0 行时回读区分「已下架」和「库存不足」。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productId")
    public void deductStock(String orderNo, Integer productId, Integer quantity) {
        requirePositiveQuantity(quantity);
        if (productMapper.findById(productId) == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (alreadyApplied(orderNo, productId, OP_DEDUCT, quantity)) {
            return;
        }
        if (productMapper.deductStock(productId, quantity) == 0) {
            throw new BusinessException(describeDeductFailure(productMapper.findById(productId)));
        }
        log.info("扣减库存: orderNo={}, productId={}, quantity={}", orderNo, productId, quantity);
    }

    /** 条件更新的 WHERE 同时管"在售"和"库存够"，影响 0 行时回读一次才能说清是哪种 */
    private String describeDeductFailure(Product latest) {
        if (latest == null) {
            // 回读的时候商品已经被删掉了
            return "商品不存在";
        }
        if (!isOnSale(latest)) {
            return "商品「" + latest.getName() + "」已下架";
        }
        return "商品「" + latest.getName() + "」库存不足";
    }

    /** 还库存：商品已删除只记日志；服务不可用时抛异常让调用方回滚重试。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productId")
    public void restoreStock(String orderNo, Integer productId, Integer quantity) {
        requirePositiveQuantity(quantity);
        if (alreadyApplied(orderNo, productId, OP_RESTORE, quantity)) {
            return;
        }
        if (productMapper.addStock(productId, quantity) == 0) {
            log.warn("归还库存失败，商品可能已被删除: orderNo={}, productId={}, quantity={}",
                    orderNo, productId, quantity);
            return;
        }
        log.info("归还库存: orderNo={}, productId={}, quantity={}", orderNo, productId, quantity);
    }

    /** 商家补货：只加不减，所以不会把已被待付款订单扣走的那部分覆盖掉 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productId")
    public void restock(Integer productId, Integer quantity, Integer operatorId) {
        requirePositiveQuantity(quantity);
        requireOwned(productId, operatorId);
        productMapper.addStock(productId, quantity);
        log.info("商品补货: productId={}, quantity={}, operatorId={}", productId, quantity, operatorId);
    }

    /** 付款成功时累加销量。商品已被删除时只记日志。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productId")
    public void increaseSales(String orderNo, Integer productId, Integer quantity) {
        requirePositiveQuantity(quantity);
        if (alreadyApplied(orderNo, productId, OP_SALES_INC, quantity)) {
            return;
        }
        if (productMapper.increaseSales(productId, quantity) == 0) {
            log.warn("累加销量失败，商品可能已被删除: orderNo={}, productId={}, quantity={}",
                    orderNo, productId, quantity);
            return;
        }
        log.info("累加销量: orderNo={}, productId={}, quantity={}", orderNo, productId, quantity);
    }

    /** 扣回销量：减不动只记日志，不让取消失败。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#productId")
    public void decreaseSales(String orderNo, Integer productId, Integer quantity) {
        requirePositiveQuantity(quantity);
        if (alreadyApplied(orderNo, productId, OP_SALES_DEC, quantity)) {
            return;
        }
        if (productMapper.decreaseSales(productId, quantity) == 0) {
            log.error("扣回销量失败，销量与订单对不上，需人工核对: orderNo={}, productId={}, quantity={}",
                    orderNo, productId, quantity);
            return;
        }
        log.info("扣回销量: orderNo={}, productId={}, quantity={}", orderNo, productId, quantity);
    }

    private void requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("数量必须大于 0");
        }
    }

    public int countAll() {
        return productMapper.countAll();
    }
}