package cn.itcast.core.service;

import cn.itcast.core.mapper.item.ItemCatMapper;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.item.ItemCatQuery;
import cn.itcast.core.pojo.item.ItemQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class ItemCatServiceImpl implements ItemCatService {

    @Autowired
    private ItemCatMapper itemCatMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<ItemCat> findByParentId(Long parentId) {
        // 1：从Mysql中查询所有分类结果集
        List<ItemCat> itemCats = findAll();
        // 2：保存缓存一份
        for (ItemCat itemCat : itemCats) {
            redisTemplate.boundHashOps("itemcat").put(itemCat.getName(),itemCat.getTypeId());
        }

        ItemCatQuery query = new ItemCatQuery();
        ItemCatQuery.Criteria criteria = query.createCriteria();
        criteria.andParentIdEqualTo(parentId);
        return itemCatMapper.selectByExample(query);
    }

    @Override
    public PageResult search(Integer page, Integer rows, ItemCat itemCat) {
        PageHelper.startPage(page, rows);
        //PageHelper.orderBy("id desc");
        ItemCatQuery query = new ItemCatQuery();
        ItemCatQuery.Criteria criteria = query.createCriteria();
        if (null != itemCat.getName() && !"".equals(itemCat.getName().trim())) {
            criteria.andNameLike("%"+itemCat.getName().trim()+"%");
        }
        if (itemCat.getParentId() != null) {
            criteria.andParentIdEqualTo(itemCat.getParentId());
        }
        Page<ItemCat> p = (Page<ItemCat>) itemCatMapper.selectByExample(query);
        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void add(ItemCat itemCat) {
        itemCatMapper.insertSelective(itemCat);
    }

    @Override
    public ItemCat findOne(Long id) {
        return itemCatMapper.selectByPrimaryKey(id);
    }

    @Override
    public void update(ItemCat itemCat) {
        itemCatMapper.updateByPrimaryKeySelective(itemCat);
    }

    @Override
    public void delete(Long[] ids) {
        ItemCatQuery query = new ItemCatQuery();
        query.createCriteria().andIdIn(Arrays.asList(ids));
        itemCatMapper.deleteByExample(query);
    }

    @Override
    public List<ItemCat> findAll() {
        return itemCatMapper.selectByExample(null);
    }


}
