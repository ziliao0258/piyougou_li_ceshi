package cn.itcast.core.service;

import cn.itcast.core.mapper.good.GoodsDescMapper;
import cn.itcast.core.mapper.good.GoodsMapper;
import cn.itcast.core.mapper.item.ItemCatMapper;
import cn.itcast.core.mapper.item.ItemMapper;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.good.GoodsDesc;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import com.alibaba.dubbo.config.annotation.Service;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 静态化程序
 */
@Service
public class StaticPageServiceImpl implements StaticPageService,ServletContextAware {

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;
    @Autowired
    private GoodsDescMapper goodsDescMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private ItemCatMapper itemCatMapper;

    //静态化程序
    public void index(Long id) {
        //创建
        Configuration conf = freeMarkerConfigurer.getConfiguration();
        //输出的路径
        String allPath = getPath("/"+id+".html");
        //数据
        Map<String,Object> root = new HashMap<>();


        //商品详情对象
        GoodsDesc goodsDesc = goodsDescMapper.selectByPrimaryKey(id);
        root.put("goodsDesc",goodsDesc);

        //商品对象
        Goods goods = goodsMapper.selectByPrimaryKey(id);
        root.put("goods",goods);
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id).andStatusEqualTo("1");
        List<Item> itemList = itemMapper.selectByExample(itemQuery);
        root.put("itemList",itemList);

        //一级

        root.put("itemCat1", itemCatMapper.selectByPrimaryKey(goods.getCategory1Id()).getName());
        root.put("itemCat2", itemCatMapper.selectByPrimaryKey(goods.getCategory2Id()).getName());
        root.put("itemCat3", itemCatMapper.selectByPrimaryKey(goods.getCategory3Id()).getName());
        //模板对象
        Writer out = null;
        try {
            //读
            Template template = conf.getTemplate("item.ftl");
            //输出 写
            out = new OutputStreamWriter(new FileOutputStream(new File(allPath)),"UTF-8");
            //处理
            template.process(root,out);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(null != out){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //获取全路径
    public String getPath(String path){
        return servletContext.getRealPath(path);
    }

    private ServletContext servletContext;
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
