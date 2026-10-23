package com.markethub.product.controller;

import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUserId;
import com.markethub.product.entity.Favorite;
import com.markethub.product.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;

    @GetMapping
    public R<List<Favorite>> list(@CurrentUserId Integer userId) {
        return R.success(favoriteMapper.findByUserId(userId));
    }

    @PostMapping("/{productId}")
    public R<String> add(@PathVariable Integer productId, @CurrentUserId Integer userId) {
        favoriteMapper.insert(userId, productId);
        return R.success("收藏成功");
    }

    @DeleteMapping("/{productId}")
    public R<String> remove(@PathVariable Integer productId, @CurrentUserId Integer userId) {
        favoriteMapper.delete(userId, productId);
        return R.success("已取消收藏");
    }

    @GetMapping("/check/{productId}")
    public R<Boolean> check(@PathVariable Integer productId, @CurrentUserId Integer userId) {
        return R.success(favoriteMapper.exists(userId, productId) > 0);
    }

    @GetMapping("/count")
    public R<Integer> count(@CurrentUserId Integer userId) {
        return R.success(favoriteMapper.countByUserId(userId));
    }
}