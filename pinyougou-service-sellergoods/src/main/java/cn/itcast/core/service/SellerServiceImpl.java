package cn.itcast.core.service;

import cn.itcast.core.mapper.seller.SellerMapper;
import cn.itcast.core.pojo.seller.Seller;
import cn.itcast.core.pojo.seller.SellerQuery;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SellerServiceImpl implements SellerService {

    @Autowired
    private SellerMapper sellerMapper;

    // 添加商家
    public void add(Seller seller) {
        // 登录名
        //登陆密码
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        seller.setPassword(encoder.encode(seller.getPassword()));
        // 店铺名称
        // 公司名称
        //审核状态
        seller.setStatus("0");
        sellerMapper.insertSelective(seller);
    }

    @Override
    public Seller findOne(String sellerId) {
        return sellerMapper.selectByPrimaryKey(sellerId);
    }

    // 分页查询  条件
    public PageResult search(Integer page, Integer rows, Seller seller) {
        // 分页插件
        PageHelper.startPage(page, rows);
        //PageHelper.orderBy("id desc");
        // 创建分页对象
        SellerQuery query = new SellerQuery();
        SellerQuery.Criteria criteria = query.createCriteria();
        if (null != seller.getStatus() && !"".equals(seller.getStatus())) {
            criteria.andStatusEqualTo(seller.getStatus());
        }
        // 判断是否有值
        if (null != seller.getName() && !"".equals(seller.getName().trim())) {
            criteria.andNameLike("%" + seller.getName().trim() + "%");
        }
        if (null != seller.getNickName() && !"".equals(seller.getNickName().trim())) {
            criteria.andNickNameLike("%" + seller.getNickName().trim() + "%");
        }
        Page<Seller> p = (Page<Seller>) sellerMapper.selectByExample(query);
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void updateStatus(String sellerId, String status) {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setStatus(status);
        sellerMapper.updateByPrimaryKeySelective(seller);
    }


}
