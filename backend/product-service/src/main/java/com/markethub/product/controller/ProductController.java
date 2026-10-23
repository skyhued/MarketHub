package com.markethub.product.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import com.markethub.common.util.FileUploadUtil;
import com.markethub.product.entity.Category;
import com.markethub.product.entity.Product;
import com.markethub.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "商品接口", description = "商品CRUD、分类管理、搜索")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final FileUploadUtil fileUploadUtil;

    @Operation(summary = "管理端获取商品列表")
    @GetMapping("/admin/products")
    @RequirePermission(PermissionConstants.PRODUCT_AUDIT)
    public R<List<Product>> getAdminProducts(@RequestParam(required = false) Integer auditStatus) {
        return R.success(productService.findForAdmin(auditStatus));
    }

    @Operation(summary = "获取所有商品（管理端）")
    @GetMapping("/products")
    @RequirePermission(PermissionConstants.PRODUCT_LIST)
    public R<List<Product>> getAllProducts() {
        return R.success(productService.findAll());
    }

    @Operation(summary = "获取在售商品列表")
    @GetMapping("/products/on-sale")
    public R<List<Product>> getOnSaleProducts() {
        return R.success(productService.findOnSale());
    }

    @Operation(summary = "获取用户自己的商品")
    @GetMapping("/products/my")
    @RequirePermission(PermissionConstants.PRODUCT_LIST)
    public R<List<Product>> getMyProducts(@CurrentUserId Integer userId) {
        return R.success(productService.findByUserId(userId));
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/products/{id}")
    public R<Product> getProductById(@PathVariable Integer id,
                                     @CurrentUserId(required = false) Integer userId) {
        // findById 带 @Cacheable，必须直接调才走得到缓存代理，不能塞进 service 内部自调用
        Product product = productService.requireVisible(productService.findById(id), userId);
        productService.incrementViewCount(id);
        return R.success(product);
    }

    @Operation(summary = "搜索商品")
    @GetMapping("/products/search")
    public R<List<Product>> searchProducts(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer categoryId) {
        return R.success(productService.search(keyword, categoryId));
    }

    @Operation(summary = "创建商品")
    @PostMapping("/products")
    @RequirePermission(PermissionConstants.PRODUCT_CREATE)
    public R<String> createProduct(@RequestBody Product product, @CurrentUserId Integer userId) {
        product.setUserId(userId);
        productService.create(product);
        return R.success("商品发布成功");
    }

    @Operation(summary = "更新商品")
    @PutMapping("/products")
    @RequirePermission(PermissionConstants.PRODUCT_UPDATE)
    public R<String> updateProduct(@RequestBody Product product, @CurrentUserId Integer userId) {
        productService.update(product, userId);
        return R.success("更新成功");
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/products/{id}")
    @RequirePermission(PermissionConstants.PRODUCT_DELETE)
    public R<String> deleteProduct(@PathVariable Integer id, @CurrentUserId Integer userId) {
        productService.delete(id, userId);
        return R.success("删除成功");
    }

    @Operation(summary = "补货（只增不减，不会覆盖掉待付款订单占用的库存）")
    @PutMapping("/products/{id}/restock")
    @RequirePermission(PermissionConstants.PRODUCT_UPDATE)
    public R<String> restockProduct(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                    @CurrentUserId Integer userId) {
        Object raw = body.get("quantity");
        if (raw == null) {
            return R.error("quantity 不能为空");
        }
        productService.restock(id, Integer.valueOf(String.valueOf(raw)), userId);
        return R.success("补货成功");
    }

    @Operation(summary = "上架/下架商品")
    @PutMapping("/products/{id}/toggle-status")
    @RequirePermission(PermissionConstants.PRODUCT_UPDATE)
    public R<String> toggleProductStatus(@PathVariable Integer id, @CurrentUserId Integer userId) {
        productService.toggleStatus(id, userId);
        return R.success("状态已切换");
    }

    @Operation(summary = "上传商品图片")
    @PostMapping("/products/upload/image")
    @RequirePermission(PermissionConstants.PRODUCT_CREATE)
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            return R.success(fileUploadUtil.uploadFile(file, "products"));
        } catch (Exception e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有分类")
    @GetMapping("/categories")
    public R<List<Category>> getCategories() {
        return R.success(productService.findAllCategories());
    }

    @Operation(summary = "审核商品")
    @PostMapping("/admin/product/audit")
    @RequirePermission(PermissionConstants.PRODUCT_AUDIT)
    public R<String> auditProduct(@RequestParam Integer productId, @RequestParam Integer status) {
        productService.audit(productId, status);
        return R.success("审核完成");
    }
}