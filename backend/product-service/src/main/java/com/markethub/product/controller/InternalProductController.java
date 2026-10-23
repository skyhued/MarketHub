package com.markethub.product.controller;

import com.markethub.common.exception.R;
import com.markethub.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 服务间内部库存接口；四个操作都用 orderNo 做幂等键。 */
@Tag(name = "商品内部接口", description = "库存扣减/归还、销量增减，仅供服务间调用")
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @Operation(summary = "扣减库存")
    @PostMapping("/{id}/stock/deduct")
    public R<String> deductStock(@PathVariable Integer id,
                                 @RequestParam String orderNo,
                                 @RequestParam Integer quantity) {
        productService.deductStock(orderNo, id, quantity);
        return R.success("库存已扣减");
    }

    @Operation(summary = "归还库存")
    @PostMapping("/{id}/stock/restore")
    public R<String> restoreStock(@PathVariable Integer id,
                                  @RequestParam String orderNo,
                                  @RequestParam Integer quantity) {
        productService.restoreStock(orderNo, id, quantity);
        return R.success("库存已归还");
    }

    @Operation(summary = "累加销量")
    @PostMapping("/{id}/sales/increase")
    public R<String> increaseSales(@PathVariable Integer id,
                                   @RequestParam String orderNo,
                                   @RequestParam Integer quantity) {
        productService.increaseSales(orderNo, id, quantity);
        return R.success("销量已累加");
    }

    @Operation(summary = "扣回销量（已付款订单被取消）")
    @PostMapping("/{id}/sales/decrease")
    public R<String> decreaseSales(@PathVariable Integer id,
                                   @RequestParam String orderNo,
                                   @RequestParam Integer quantity) {
        productService.decreaseSales(orderNo, id, quantity);
        return R.success("销量已扣回");
    }
}
