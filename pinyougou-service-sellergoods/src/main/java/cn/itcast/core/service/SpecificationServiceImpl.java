package cn.itcast.core.service;

import cn.itcast.core.mapper.specification.SpecificationMapper;
import cn.itcast.core.mapper.specification.SpecificationOptionMapper;
import cn.itcast.core.pojo.specification.Specification;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.specification.SpecificationQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pojogroup.SpecificationVo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 规格管理
 */
@Service
@Transactional
public class SpecificationServiceImpl implements  SpecificationService {

    @Autowired
    private SpecificationMapper specificationMapper;
    @Autowired
    private SpecificationOptionMapper specificationOptionMapper;

    @Override
    public List<Specification> findAll() {
        return specificationMapper.selectByExample(null);
    }

    @Override
    public PageResult findPage(Integer page, Integer rows) {
        PageHelper.startPage(page, rows);
        Page<Specification> p = (Page<Specification>) specificationMapper.selectByExample(null);
        return new PageResult(p.getTotal(),p.getResult());
    }

    //查询分页
    @Override
    public PageResult search(Integer page, Integer rows, Specification specification) {
        //分页插件
        PageHelper.startPage(page,rows);
        SpecificationQuery query = new SpecificationQuery();
        SpecificationQuery.Criteria criteria = query.createCriteria();
        //判断是否有值
        if (null != specification.getSpecName() && !"".equals(specification.getSpecName().trim())) {
            criteria.andSpecNameLike("%" + specification.getSpecName().trim() + "%");
        }
        //查询
        Page<Specification> p = (Page<Specification>) specificationMapper.selectByExample(query);
        return new PageResult(p.getTotal(),p.getResult());
    }

    //添加
    @Override
    public void add(SpecificationVo vo) {

        //规格表 主键
        specificationMapper.insertSelective(vo.getSpecification());
        //规格选项结果集 外键
        List<SpecificationOption> specificationOptionList = vo.getSpecificationOptionList();
        for (SpecificationOption specificationOption : specificationOptionList) {
            //外键
            specificationOption.setSpecId(vo.getSpecification().getId());
            //保存
            specificationOptionMapper.insertSelective(specificationOption);
        }

    }

    @Override
    public SpecificationVo findOne(Long id) {

        SpecificationVo vo = new SpecificationVo();

        //规格对象
        Specification specification = specificationMapper.selectByPrimaryKey(id);
        vo.setSpecification(specification);
        //规格选项对象结果集
        SpecificationOptionQuery query = new SpecificationOptionQuery();
        query.createCriteria().andSpecIdEqualTo(id);

        List<SpecificationOption> specificationOptions = specificationOptionMapper.selectByExample(query);
        vo.setSpecificationOptionList(specificationOptions);
        return vo;
    }
    //Ctrl + i

    @Override
    public void update(SpecificationVo vo) {
        //规格表 修改
        specificationMapper.updateByPrimaryKeySelective(vo.getSpecification());

        //规格选项结果集表
        //1:先删除  外键
        SpecificationOptionQuery query = new SpecificationOptionQuery();
        query.createCriteria().andSpecIdEqualTo(vo.getSpecification().getId());
        specificationOptionMapper.deleteByExample(query);

        //2:再添加
        //规格选项结果集 外键
        List<SpecificationOption> specificationOptionList = vo.getSpecificationOptionList();
        for (SpecificationOption specificationOption : specificationOptionList) {
            //外键
            specificationOption.setSpecId(vo.getSpecification().getId());
            //保存
            specificationOptionMapper.insertSelective(specificationOption);
        }
    }

    @Override
    public void delete(Long[] ids) {
        SpecificationQuery query = new SpecificationQuery();
        query.createCriteria().andIdIn(Arrays.asList(ids));
        specificationMapper.deleteByExample(query);
    }

    @Override
    public List<Map> selectOptionList() {

        return specificationMapper.selectOptionList();
    }
}
