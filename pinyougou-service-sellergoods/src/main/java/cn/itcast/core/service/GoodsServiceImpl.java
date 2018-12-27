package cn.itcast.core.service;

import cn.itcast.core.mapper.good.BrandMapper;
import cn.itcast.core.mapper.good.GoodsDescMapper;
import cn.itcast.core.mapper.good.GoodsMapper;
import cn.itcast.core.mapper.item.ItemCatMapper;
import cn.itcast.core.mapper.item.ItemMapper;
import cn.itcast.core.mapper.seller.SellerMapper;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.good.GoodsQuery;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.item.ItemQuery;
import cn.itcast.core.pojo.seller.Seller;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Transactional;
import pojogroup.GoodsVo;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.*;

/**
 * 商品管理
 */
@SuppressWarnings("ALL")
@Service
@Transactional
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private GoodsDescMapper goodsDescMapper;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private ItemCatMapper itemCatMapper;
    @Autowired
    private SellerMapper sellerMapper;
    @Autowired
    private BrandMapper brandMapper;

    //三个表
    @Override
    public void add(GoodsVo vo) {
        //商品表 返回ID
        //审核状态
        vo.getGoods().setAuditStatus("0");
        //保存
        goodsMapper.insertSelective(vo.getGoods());

        //商品详情表 ID
        vo.getGoodsDesc().setGoodsId(vo.getGoods().getId());
        goodsDescMapper.insertSelective(vo.getGoodsDesc());

        //判断是否启用规格
        if ("1".equals(vo.getGoods().getIsEnableSpec())) {
            //启用
            //库存集合  外键
            List<Item> itemList = vo.getItemList();
            for (Item item : itemList) {
                //标题 = SPU名称 + 规格
                String title = vo.getGoods().getGoodsName();
                //{"机身内存":"16G","网络":"联通3G",..,...}
                String spec = item.getSpec();
                Map<String, String> specMap = JSON.parseObject(spec, Map.class);

                Set<Map.Entry<String, String>> entries = specMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    title += " " + entry.getValue();
                }
                item.setTitle(title);
                //保存商品第一张图片
                String itemImages = vo.getGoodsDesc().getItemImages();
                List<Map> images = JSON.parseArray(itemImages, Map.class);
                if (null != images && images.size() > 0) {
                    item.setImage((String) images.get(0).get("url"));
                }
                //第三个商品分类ID
                item.setCategoryid(vo.getGoods().getCategory3Id());
                //第三个商品分类的名称
                ItemCat itemCat = itemCatMapper.selectByPrimaryKey(vo.getGoods().getCategory3Id());
                item.setCategory(itemCat.getName());
                //时间
                item.setCreateTime(new Date());
                item.setUpdateTime(new Date());
                //商品表的ID
                item.setGoodsId(vo.getGoods().getId());
                //商家ID
                item.setSellerId(vo.getGoods().getSellerId());
                //商家名称
                Seller seller = sellerMapper.selectByPrimaryKey(vo.getGoods().getSellerId());
                item.setSeller(seller.getName());

                //品牌名称
                Brand brand = brandMapper.selectByPrimaryKey(vo.getGoods().getBrandId());
                item.setBrand(brand.getName());
                //库存表
                itemMapper.insertSelective(item);
            }
        } else {
            //不启用   默认值              tb_item (默认值 )
        }
    }

    @Override
    public PageResult search(Integer page, Integer rows, Goods goods) {
        //分页插件
        PageHelper.startPage(page, rows);
        //排序
        PageHelper.orderBy("id desc");

        GoodsQuery goodsQuery = new GoodsQuery();
        GoodsQuery.Criteria criteria = goodsQuery.createCriteria();
        //条件
        if (null != goods.getAuditStatus() && !"".equals(goods.getAuditStatus())) {
            criteria.andAuditStatusEqualTo(goods.getAuditStatus());
        }
        //名称  前端去空格 校验 都不安全 处理值
        if (null != goods.getGoodsName() && !"".equals(goods.getGoodsName().trim())) {
            criteria.andGoodsNameLike("%" + goods.getGoodsName().trim() + "%");
        }
        //只查询不删除
        criteria.andIsDeleteIsNull();
        //判断 是否有商家ID　如果有：商家后台要查询商品列表　　　如果没有：运营商后台要查询商品列表
        if (null != goods.getSellerId()) {
            //只查询当前商家的
            criteria.andSellerIdEqualTo(goods.getSellerId());
        }
        Page<Goods> p = (Page<Goods>) goodsMapper.selectByExample(goodsQuery);

        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public GoodsVo findOne(Long id) {
        GoodsVo vo = new GoodsVo();
        //商品对象
        vo.setGoods(goodsMapper.selectByPrimaryKey(id));
        //商品详情对象
        vo.setGoodsDesc(goodsDescMapper.selectByPrimaryKey(id));
        //库存对象结果集
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id);
        vo.setItemList(itemMapper.selectByExample(itemQuery));

        return vo;
    }

    @Override
    public void update(GoodsVo vo) {
        //商品表
        goodsMapper.updateByPrimaryKeySelective(vo.getGoods());
        //商品详情表
        goodsDescMapper.updateByPrimaryKeySelective(vo.getGoodsDesc());
        //库存表
        //1:先删除
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(vo.getGoods().getId());
        itemMapper.deleteByExample(itemQuery);
        //2:再添加
        //判断是否启用规格
        if("1".equals(vo.getGoods().getIsEnableSpec())){
            //启用
            //库存集合  外键
            List<Item> itemList = vo.getItemList();
            for (Item item : itemList) {
                //标题 = SPU名称 + 规格
                String title = vo.getGoods().getGoodsName();
                //{"机身内存":"16G","网络":"联通3G",..,...}
                String spec = item.getSpec();
                Map<String,String> specMap = JSON.parseObject(spec, Map.class);

                Set<Map.Entry<String, String>> entries = specMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    title += " " + entry.getValue();
                }
                item.setTitle(title);
                //保存商品第一张图片
                String itemImages = vo.getGoodsDesc().getItemImages();
                List<Map> images = JSON.parseArray(itemImages, Map.class);
                if(null != images && images.size() > 0){
                    item.setImage((String) images.get(0).get("url"));
                }
                //第三个商品分类ID
                item.setCategoryid(vo.getGoods().getCategory3Id());
                //第三个商品分类的名称
                ItemCat itemCat = itemCatMapper.selectByPrimaryKey(vo.getGoods().getCategory3Id());
                item.setCategory(itemCat.getName());
                //时间
                item.setCreateTime(new Date());
                item.setUpdateTime(new Date());
                //商品表的ID
                item.setGoodsId(vo.getGoods().getId());
                //商家ID
                item.setSellerId(vo.getGoods().getSellerId());
                //商家名称
                Seller seller = sellerMapper.selectByPrimaryKey(vo.getGoods().getSellerId());
                item.setSeller(seller.getName());

                //品牌名称
                Brand brand = brandMapper.selectByPrimaryKey(vo.getGoods().getBrandId());
                item.setBrand(brand.getName());
                //库存表
                itemMapper.insertSelective(item);
            }
        }else{
            //不启用   默认值              tb_item (默认值 )
        }
    }

    @Override
    public void delete(Long[] ids) {
        // 彻底从数据库中删除，但这里的删除只是修改goods表的delete字段
        /*if (null != ids && ids.length > 0) {
            for (Long id : ids) {
                goodsMapper.deleteByPrimaryKey(id);
                goodsDescMapper.deleteByPrimaryKey(id);
                ItemQuery itemQuery = new ItemQuery();
                itemQuery.createCriteria().andGoodsIdEqualTo(id);
                itemMapper.deleteByExample(itemQuery);
            }
        }*/
        // 修改goods表的isDelete字段
        Goods goods = new Goods();
        goods.setIsDelete("1");
        if (null != ids && ids.length > 0) {
            for (Long id : ids) {
                // 修改goods表的isDelete字段
                goods.setId(id);
                goodsMapper.updateByPrimaryKeySelective(goods);
                //2:发消息
                jmsTemplate.send(queueSolrDeleteDestination, new MessageCreator() {
                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        return session.createTextMessage(String.valueOf(id));
                    }
                });
            }
        }
    }

    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private Destination topicPageAndSolrDestination;
    @Autowired
    private Destination queueSolrDeleteDestination;  //一发 一接  点对点  队列模式

    public void updateStatus(Long[] ids, String status) {
        Goods goods = new Goods();
        goods.setAuditStatus(status);
        if (null != ids && ids.length > 0) {
            for (Long id : ids) { // 商品表的ID
                goods.setId(id);
                // 更新审核状态
                goodsMapper.updateByPrimaryKeySelective(goods);
                // 只有审核通过才会完成下面两项
                if ("1".equals(status)) {
                    // 发消息
                    jmsTemplate.send(topicPageAndSolrDestination, new MessageCreator() {
                        @Override
                        public Message createMessage(Session session) throws JMSException {
                            return session.createTextMessage(String.valueOf(id));
                        }
                    });
                }
            }
        }
    }
}
