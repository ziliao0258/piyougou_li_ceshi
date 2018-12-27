package cn.itcast.core.service;

import cn.itcast.core.pojo.item.ItemCat;
import entity.PageResult;

import java.util.List;

public interface ItemCatService {
    List<ItemCat> findByParentId(Long parentId);

    PageResult search(Integer page, Integer rows, ItemCat itemCat);

    void add(ItemCat itemCat);

    ItemCat findOne(Long id);

    void update(ItemCat itemCat);

    void delete(Long[] ids);

    List<ItemCat> findAll();
}
