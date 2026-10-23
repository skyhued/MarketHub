package com.markethub.user.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.common.exception.R;
import com.markethub.user.dto.MerchantApplyRequest;
import com.markethub.user.entity.MerchantApplication;
import com.markethub.user.entity.User;
import com.markethub.user.feign.OrderFeignClient;
import com.markethub.user.mapper.MerchantApplicationMapper;
import com.markethub.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 开店申请流程：提交 → 审核 → 升为商家 → 可收回；任何改角色的操作都必须作废旧 token。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantApplicationService {

    private static final int SHOP_NAME_MAX = 20;
    private static final int DESCRIPTION_MAX = 500;
    private static final int REASON_MAX = 255;

    private final MerchantApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final OrderFeignClient orderFeignClient;
    private final TransactionTemplate transactionTemplate;


    @Transactional(rollbackFor = Exception.class)
    public void apply(Integer userId, MerchantApplyRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String roleCode = user.getRole() == null ? ApiConstants.ROLE_USER : user.getRole().getRoleCode();
        if (ApiConstants.ROLE_MERCHANT.equals(roleCode)) {
            throw new BusinessException("你已经是商家，不需要再申请");
        }
        if (ApiConstants.ROLE_ADMIN.equals(roleCode)) {
            throw new BusinessException("管理员账号无需开店");
        }
        if (applicationMapper.findPendingByUserId(userId) != null) {
            throw new BusinessException("你有一份申请正在审核中，请等待审核结果");
        }

        String shopName = trim(request.getShopName());
        String phone = trim(request.getContactPhone());
        String description = trim(request.getDescription());

        if (shopName.isEmpty()) {
            throw new BusinessException("请填写店铺名");
        }
        if (shopName.length() > SHOP_NAME_MAX) {
            throw new BusinessException("店铺名不能超过 " + SHOP_NAME_MAX + " 个字");
        }
        if (request.getCategoryId() == null) {
            throw new BusinessException("请选择主营类目");
        }
        if (!phone.matches("[0-9+\\- ]{6,20}")) {
            throw new BusinessException("联系电话格式不正确");
        }
        if (description.isEmpty()) {
            throw new BusinessException("请填写申请说明");
        }
        if (description.length() > DESCRIPTION_MAX) {
            throw new BusinessException("申请说明不能超过 " + DESCRIPTION_MAX + " 个字");
        }
        if (shopNameTaken(shopName, userId)) {
            throw new BusinessException("店铺名「" + shopName + "」已被占用，换一个吧");
        }

        MerchantApplication application = new MerchantApplication();
        application.setUserId(userId);
        application.setShopName(shopName);
        application.setCategoryId(request.getCategoryId());
        application.setContactPhone(phone);
        application.setDescription(description);
        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException e) {
            // uk_pending_user 兜住"连点两次提交"这类并发：上面查过一次，但两个请求可能同时通过检查
            throw new BusinessException("你有一份申请正在审核中，请等待审核结果");
        }
        log.info("开店申请已提交: userId={}, shopName={}", userId, shopName);
    }

    public MerchantApplication findMyLatest(Integer userId) {
        return applicationMapper.findLatestByUserId(userId);
    }

    /** 申请人看过"审核通过"的提示后打标记，之后不再重复弹。 */
    public void markResultSeen(Integer userId) {
        applicationMapper.markResultSeen(userId);
    }


    public List<MerchantApplication> listApplications(Integer status) {
        return applicationMapper.findAll(status);
    }

    public int countPending() {
        return applicationMapper.countPending();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer applicationId, Integer reviewerId) {
        MerchantApplication application = requirePending(applicationId);
        // 提交到审核之间可能有人先通过了同名申请，这里再查一次，否则会撞 user.shop_name 的唯一键
        if (userMapper.countByShopName(application.getShopName(), application.getUserId()) > 0) {
            throw new BusinessException("店铺名「" + application.getShopName() + "」已被占用，请驳回并让申请人换一个");
        }
        if (applicationMapper.approve(applicationId, reviewerId) == 0) {
            throw new BusinessException("这份申请刚被其他管理员处理过了");
        }
        userMapper.promoteToMerchant(application.getUserId(), application.getShopName(), ApiConstants.ROLE_ID_MERCHANT);
        userService.revokeTokens(application.getUserId());
        log.info("开店申请通过: applicationId={}, userId={}, shopName={}",
                applicationId, application.getUserId(), application.getShopName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer applicationId, Integer reviewerId, String reason) {
        MerchantApplication application = requirePending(applicationId);
        String trimmed = requireReason(reason, "驳回原因");
        if (applicationMapper.reject(applicationId, reviewerId, trimmed) == 0) {
            throw new BusinessException("这份申请刚被其他管理员处理过了");
        }
        log.info("开店申请驳回: applicationId={}, userId={}, reason={}",
                applicationId, application.getUserId(), trimmed);
    }

    public List<User> listMerchants() {
        List<User> merchants = userMapper.findMerchants();
        merchants.forEach(merchant -> merchant.setPassword(null));
        return merchants;
    }

    /** 收回商家资格：先远程查订单，再开本地事务。 */
    public void revoke(Integer merchantUserId, Integer revokerId, String reason) {
        User user = userMapper.findById(merchantUserId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getRole() == null || !ApiConstants.ROLE_MERCHANT.equals(user.getRole().getRoleCode())) {
            throw new BusinessException("该用户当前不是商家");
        }
        String trimmed = requireReason(reason, "收回原因");

        int unfinished = countUnfinishedOrders(merchantUserId);
        if (unfinished > 0) {
            throw new BusinessException("该商家还有 " + unfinished + " 笔订单未完成（已付款待发货或已发货），"
                    + "处理完才能收回资格");
        }

        transactionTemplate.executeWithoutResult(status -> {
            applicationMapper.revokeByUserId(merchantUserId, revokerId, trimmed);
            userMapper.demoteToUser(merchantUserId, ApiConstants.ROLE_ID_USER);
            userService.revokeTokens(merchantUserId);
        });
        log.info("商家资格已收回: userId={}, reason={}", merchantUserId, trimmed);
    }

    private int countUnfinishedOrders(Integer merchantUserId) {
        try {
            R<Integer> response = orderFeignClient.countUnfinishedBySeller(merchantUserId);
            if (response == null || response.getData() == null) {
                throw new BusinessException(503, "订单服务没有返回未完成订单数，请稍后重试");
            }
            return response.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("查询未完成订单失败: userId={}, {}", merchantUserId, e.getMessage());
            throw new BusinessException(503, "无法确认该商家的订单是否处理完，暂时不能收回，请稍后重试");
        }
    }

    private MerchantApplication requirePending(Integer applicationId) {
        MerchantApplication application = applicationMapper.findById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "申请不存在");
        }
        if (!Integer.valueOf(ApiConstants.APPLICATION_PENDING).equals(application.getStatus())) {
            throw new BusinessException("这份申请已经处理过了");
        }
        return application;
    }

    /** 查两张表：已通过的占着 user.shop_name，待审核的还在申请表里。两者都排除申请人自己。 */
    private boolean shopNameTaken(String shopName, Integer applicantUserId) {
        return userMapper.countByShopName(shopName, applicantUserId) > 0
                || applicationMapper.countPendingByShopName(shopName, applicantUserId) > 0;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** reject_reason / revoke_reason 都是 VARCHAR(255)，超了会在严格模式下直接报数据库错。 */
    private String requireReason(String raw, String fieldName) {
        String trimmed = trim(raw);
        if (trimmed.isEmpty()) {
            throw new BusinessException("请填写" + fieldName);
        }
        if (trimmed.length() > REASON_MAX) {
            throw new BusinessException(fieldName + "不能超过 " + REASON_MAX + " 个字");
        }
        return trimmed;
    }
}
