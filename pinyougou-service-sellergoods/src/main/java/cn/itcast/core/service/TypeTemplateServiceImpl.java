package cn.itcast.core.service;

import cn.itcast.core.mapper.specification.SpecificationOptionMapper;
import cn.itcast.core.mapper.template.TypeTemplateMapper;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.template.TypeTemplate;
import cn.itcast.core.pojo.template.TypeTemplateQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 模板管理
 */
@Service
@Transactional
public class TypeTemplateServiceImpl implements TypeTemplateService {

    @Autowired
    private TypeTemplateMapper typeTemplateMapper;
    @Autowired
    private SpecificationOptionMapper specificationOptionMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    //查询分页 条件
    public PageResult search(Integer page, Integer rows, TypeTemplate typeTemplate) {
        // 1：查询所有模板结果集
        List<TypeTemplate> typeTemplates = typeTemplateMapper.selectByExample(null);
        // 2： 保存到缓存中
        for (TypeTemplate template : typeTemplates) {
            // [{"id":43,"text":"全友"},{"id":44,"text":"光明"},{"id":45,"text":"摩天"},{"id":46,"text":"极客"}]
            List<Map> brands = JSON.parseArray(template.getBrandIds(), Map.class);
            redisTemplate.boundHashOps("brandList").put(template.getId(), brands);
            List<Map> bySpecList = findBySpecList(template.getId());
            redisTemplate.boundHashOps("specList").put(template.getId(), bySpecList);

        }

        // 分页插件
        PageHelper.startPage(page, rows);
        // 排序
        //PageHelper.orderBy("id desc");
        // 创建商品模板条件对象
        TypeTemplateQuery typeTemplateQuery = new TypeTemplateQuery();
        // 降序排列
        typeTemplateQuery.setOrderByClause("id desc");
        TypeTemplateQuery.Criteria criteria = typeTemplateQuery.createCriteria();
        // 判断名称
        if (null != typeTemplate.getName() && !"".equals(typeTemplate.getName().trim())) {
            criteria.andNameLike("%" + typeTemplate.getName().trim() + "%");
        }
        // 查询
        Page<TypeTemplate> p = (Page<TypeTemplate>) typeTemplateMapper.selectByExample(typeTemplateQuery);
        return new PageResult(p.getTotal(), p.getResult());
    }

    // 添加
    public void add(TypeTemplate typeTemplate) {
        typeTemplateMapper.insertSelective(typeTemplate);
    }

    // 查询一个
    @Override
    public TypeTemplate findOne(Long id) {
        return typeTemplateMapper.selectByPrimaryKey(id);
    }

    // 更新
    @Override
    public void update(TypeTemplate typeTemplate) {
        typeTemplateMapper.updateByPrimaryKeySelective(typeTemplate);
    }

    // 删除
    @Override
    public void delete(Long[] ids) {
        TypeTemplateQuery query = new TypeTemplateQuery();
        query.createCriteria().andIdIn(Arrays.asList(ids));
        typeTemplateMapper.deleteByExample(query);
    }

    @Override
    public List<Map> findBySpecList(Long id) {
        // 通过模板ID查询模板对象
        TypeTemplate typeTemplate = typeTemplateMapper.selectByPrimaryKey(id);
        String specIds = typeTemplate.getSpecIds();
        List<Map> mapList = JSON.parseArray(specIds, Map.class);
        for (Map map : mapList) {
            SpecificationOptionQuery query = new SpecificationOptionQuery();
            query.createCriteria().andSpecIdEqualTo((long) (Integer) map.get("id"));
            List<SpecificationOption> specificationOptionList = specificationOptionMapper.selectByExample(query);
            map.put("options", specificationOptionList);
        }
        return mapList;
    }
}
