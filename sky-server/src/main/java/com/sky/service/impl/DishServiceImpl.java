package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetMealDishMapper setMealDishMapper;

    /**
     * 新增菜品和口味
     *
     * @param dishDTO
     * @Transactional 事务注解，保证数据的一致性，要么全部成功，要么全部失败
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        // 通过对象的属性拷贝，把从DishDTO中的属性拷贝到Dish对象中.前提是两个对象中的属性命名是一致的
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表（dishMapper）插入1条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        // 向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 判断口味数据是否为null，用户允许口味为null
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }


    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 分页查询
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 调用mapper进行数据库查询操作，返回类型应该是VO类型，也就是后端传给前端的
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        // 根据接口文档可知，返回数据被封装到Data和Total中，也就是所有数据和分页数据
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 菜品的批量删除
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除 --> 是否存在起售中的菜品?
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                // 当前菜品处于起售中，不能删除,否则抛出异常
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断当前菜品是否能够删除 --> 是否被套餐关联了?
        List<Long> setMealIds = setMealDishMapper.getSetMealIdsByDishIds(ids);
        if (setMealIds != null && setMealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表中的菜品数据--下面对该代码进行优化改造：减少SQL的数量以减轻服务器的压力
        /*for (Long id : ids) {
            dishMapper.deleteById(id);
            // 删除菜品关联的口味数据,可以不用查，不管有没有都直接删
            dishFlavorMapper.deleteByDishId(id);
        }*/


        // delete from dish where id in (?,?,?)
        // 根据菜品id集合，批量删除菜品数量
        dishMapper.deleteByIds(ids);

        // delete from dish_flavor where dish_id in (?,?,?)
        // 根据菜品id集合，批量删除关联的口味数据
        dishFlavorMapper.deleteByDishByIds(ids);
    }


    /**
     * 根据id查询菜品和对应的口味数据
     * 分两次查询，发送两次SQL
     * 分别查询dish表和dish_flavor表
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 将查询到的数据封装到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);

        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }


    /**
     * 根据id修改菜品基本信息和对应的口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 修改菜品表基本信息
        dishMapper.update(dish);

        // 删除原有的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            // 像口味表中插入 n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }
}
