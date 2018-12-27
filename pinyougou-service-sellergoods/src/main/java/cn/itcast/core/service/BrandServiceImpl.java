package cn.itcast.core.service;

import cn.itcast.core.mapper.ad.ContentCategoryMapper;
import cn.itcast.core.mapper.good.BrandMapper;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import com.alibaba.dubbo.config.annotation.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 品牌管理
 */
@Service
@Transactional
public class BrandServiceImpl implements  BrandService {

    @Autowired
    private BrandMapper brandMapper;
    //查询
    public List<Brand> findAll(){
        return brandMapper.selectByExample(null);
    }

    @Override
    public PageResult findPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<Brand> page = (Page<Brand>) brandMapper.selectByExample(null);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public void add(Brand brand) {
        brandMapper.insertSelective(brand);
    }

    @Override
    public Brand findOne(Long id) {

        return brandMapper.selectByPrimaryKey(id);
    }

    @Override
    public void update(Brand brand) {
        brandMapper.updateByPrimaryKeySelective(brand);
    }

    @Override
    public void delete(Long[] ids) {
        BrandQuery brandQuery = new BrandQuery();
        brandQuery.createCriteria().andIdIn(Arrays.asList(ids));
        brandMapper.deleteByExample(brandQuery);
    }

    @Override
    public PageResult search(Integer pageNum, Integer pageSize, Brand brand) {
        PageHelper.startPage(pageNum, pageSize);
        BrandQuery brandQuery = new BrandQuery();
        BrandQuery.Criteria criteria = brandQuery.createCriteria();
        if (null != brand.getName() && !"".equals(brand.getName().trim())) {
            criteria.andNameLike("%" + brand.getName().trim() + "%");
        }
        if (null != brand.getFirstChar() && !"".equals(brand.getFirstChar().trim())) {
            criteria.andFirstCharEqualTo(brand.getFirstChar().trim());
        }
        Page<Brand> page = (Page<Brand>) brandMapper.selectByExample(brandQuery);
        return new PageResult(page.getTotal(),page.getResult());
    }

    // 查询所有品牌结果集  List<Map>
    public List<Map> selectOptionList() {
        return brandMapper.selectOptionList();
    }
}
