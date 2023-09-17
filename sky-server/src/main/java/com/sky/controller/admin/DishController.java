package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;




    /**
     * 新增菜品
     * 通过文档可知，该返回值中得Data数据，不是必须返回，所以泛型可以不写
     * 通过DishDTO来封装前端传过来的数据
     * @RequestBody 用来封装json格式的数据
     *
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品:{}",dishDTO);
        // 调用业务层来操作数据库
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }


    /**
     * 这个方法并不需要加入@RequestBody注解，因为前端通过Query的方式进行传输，地址栏中通过？来传输，所以不是JSON格式
     * @param dishPageQueryDTO 前端传过来的数据被封装到DTO中
     * @return 返回类型为 PageResult
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询:{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 加上 @RequestParam 注解后，由SpringMVC自动解析字符串并拆分，存放到List集合中
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品的批量删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除{}",ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }
}
