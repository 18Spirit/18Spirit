package com.sky.service;

import com.sky.dto.DishDTO;

public interface DishService {

    /**
     * 新增菜品和口味
     * 通过DishDTO来封装前端传过来的数据
     * 添加操作，所以不需要返回值
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
}
