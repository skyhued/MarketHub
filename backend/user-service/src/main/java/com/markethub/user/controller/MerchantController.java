package com.markethub.user.controller;

import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import com.markethub.user.dto.MerchantApplyRequest;
import com.markethub.user.entity.MerchantApplication;
import com.markethub.user.entity.User;
import com.markethub.user.service.MerchantApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "开店申请", description = "用户提交开店申请、管理员审核与收回商家资格")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantApplicationService merchantApplicationService;


    @Operation(summary = "提交开店申请（仅普通用户）")
    @PostMapping("/apply")
    @RequirePermission(PermissionConstants.MERCHANT_APPLY)
    public R<String> apply(@RequestBody MerchantApplyRequest request, @CurrentUserId Integer userId) {
        merchantApplicationService.apply(userId, request);
        return R.success("申请已提交，请等待管理员审核");
    }

    @Operation(summary = "查看我最近一次的申请")
    @GetMapping("/my-application")
    public R<MerchantApplication> myApplication(@CurrentUserId Integer userId) {
        return R.success(merchantApplicationService.findMyLatest(userId));
    }

    @Operation(summary = "标记已看过审核结果，之后不再重复提示")
    @PostMapping("/result-seen")
    public R<String> resultSeen(@CurrentUserId Integer userId) {
        merchantApplicationService.markResultSeen(userId);
        return R.success("已标记");
    }


    @Operation(summary = "开店申请列表（status 省略表示全部，待审核排在前面）")
    @GetMapping("/applications")
    @RequirePermission(PermissionConstants.MERCHANT_AUDIT)
    public R<List<MerchantApplication>> applications(@RequestParam(required = false) Integer status) {
        return R.success(merchantApplicationService.listApplications(status));
    }

    @Operation(summary = "待审核申请数量（后台角标用）")
    @GetMapping("/pending-count")
    @RequirePermission(PermissionConstants.MERCHANT_AUDIT)
    public R<Integer> pendingCount() {
        return R.success(merchantApplicationService.countPending());
    }

    @Operation(summary = "当前所有商家")
    @GetMapping("/merchants")
    @RequirePermission(PermissionConstants.MERCHANT_AUDIT)
    public R<List<User>> merchants() {
        return R.success(merchantApplicationService.listMerchants());
    }

    @Operation(summary = "通过开店申请")
    @PostMapping("/applications/{id}/approve")
    @RequirePermission(PermissionConstants.MERCHANT_AUDIT)
    public R<String> approve(@PathVariable Integer id, @CurrentUserId Integer reviewerId) {
        merchantApplicationService.approve(id, reviewerId);
        return R.success("已通过，该用户需要重新登录才能获得商家权限");
    }

    @Operation(summary = "驳回开店申请")
    @PostMapping("/applications/{id}/reject")
    @RequirePermission(PermissionConstants.MERCHANT_AUDIT)
    public R<String> reject(@PathVariable Integer id, @RequestParam String reason,
                            @CurrentUserId Integer reviewerId) {
        merchantApplicationService.reject(id, reviewerId, reason);
        return R.success("已驳回");
    }

    @Operation(summary = "收回商家资格（有未完成订单时会被拒绝）")
    @PostMapping("/merchants/{merchantUserId}/revoke")
    @RequirePermission(PermissionConstants.MERCHANT_AUDIT)
    public R<String> revoke(@PathVariable Integer merchantUserId, @RequestParam String reason,
                            @CurrentUserId Integer revokerId) {
        merchantApplicationService.revoke(merchantUserId, revokerId, reason);
        return R.success("已收回商家资格");
    }
}
