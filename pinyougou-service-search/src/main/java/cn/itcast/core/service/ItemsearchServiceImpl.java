package cn.itcast.core.service;

import cn.itcast.core.pojo.item.Item;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.*;

@Service
public class ItemsearchServiceImpl implements ItemsearchService {

    @Autowired
    private SolrTemplate solrTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> search(Map<String, String> searchMap) {
        // 处理关键词
        searchMap.put("keywords",searchMap.get("keywords").replaceAll(" ",""));

        // 4：结果集
        // 5：总条数
        Map<String, Object> map = searchHighlightPage(searchMap);
        // 1：商品分类
        List<String> categoryList = searchCategoryByKeyword(searchMap);
        map.put("categoryList", categoryList);

        if (null != categoryList && categoryList.size() > 0) {
            // 查询第一个商品分类
            Object itemCatId = redisTemplate.boundHashOps("itemcat").get(categoryList.get(0));
            // 2：品牌列表
            List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(itemCatId);
            // 3：规格列表
            List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(itemCatId);
            map.put("brandList",brandList);
            map.put("specList",specList);
        }

        return map;
    }

    // 商品分类
    public List<String> searchCategoryByKeyword(Map<String, String> searchMap) {
        List<String> categorysList = new ArrayList<>();

        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        HighlightQuery query = new SimpleHighlightQuery(criteria);

        GroupOptions groupOptions = new GroupOptions();
        groupOptions.addGroupByField("item_category");
        // 分组设置
        query.setGroupOptions(groupOptions);

        // 执行查询
        GroupPage<Item> page = solrTemplate.queryForGroupPage(query, Item.class);
        GroupResult<Item> categoryList = page.getGroupResult("item_category");
        System.out.println(categoryList);
        Page<GroupEntry<Item>> groupEntries = categoryList.getGroupEntries();
        List<GroupEntry<Item>> content = groupEntries.getContent();
        for (GroupEntry<Item> groupEntry : content) {
            categorysList.add(groupEntry.getGroupValue());
        }
        return categorysList;
    }

    // 查询结果集 总条数
    public Map<String, Object> searchHighlightPage(Map<String, String> searchMap) {

        Map<String, Object> map = new HashMap<>();
        //关键词
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        HighlightQuery query = new SimpleHighlightQuery(criteria);

        // TODO：过滤条件
        //$scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sort':'','sortField':''};

        // 商品分类
        if (null != searchMap.get("category") && !"".equals(searchMap.get("category").trim())) {
            FilterQuery filterQuery = new SimpleFilterQuery(new Criteria("item_category").is(searchMap.get("category").trim()));
            query.addFilterQuery(filterQuery);
        }
        // 品牌
        if (null != searchMap.get("brand") && !"".equals(searchMap.get("brand").trim())) {
            FilterQuery filterQuery = new SimpleFilterQuery(new Criteria("item_brand").is(searchMap.get("brand").trim()));
            query.addFilterQuery(filterQuery);
        }
        // 规格
        if (null != searchMap.get("spec") && !"".equals(searchMap.get("spec").trim())) {
            /*"item_spec_网络": "联通3G",
            "item_spec_机身内存": "16G",*/
            Map<String, String> specMap = JSON.parseObject(searchMap.get("spec"), Map.class);
            Set<Map.Entry<String, String>> entries = specMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                FilterQuery filterQuery = new SimpleFilterQuery(new Criteria("item_spec_"+entry.getKey()).is(entry.getValue()));
                query.addFilterQuery(filterQuery);
            }
        }
        // 价格区间
        if (null != searchMap.get("price") && !"".equals(searchMap.get("price").trim())) {
            String[] prices = searchMap.get("price").trim().split("-");
            FilterQuery filterQuery = null;
            if (searchMap.get("price").contains("*")) {
                filterQuery = new SimpleFilterQuery(new Criteria("item_price").greaterThanEqual(prices[0]));

            } else {
                filterQuery = new SimpleFilterQuery(new Criteria("item_price").between(prices[0], prices[1], true, true));
            }

            query.addFilterQuery(filterQuery);
        }
        // TODO：排序
        // 'sort':'','sortField':''
        if (null != searchMap.get("sortField") && !"".equals(searchMap.get("sortField").trim())) {
            if ("DESC".equals(searchMap.get("sort"))) {
                query.addSort(new Sort(Sort.Direction.DESC, "item_" + searchMap.get("sortField").trim()));
            } else {
                query.addSort(new Sort(Sort.Direction.ASC, "item_" + searchMap.get("sortField").trim()));
            }
        }

        // 分页
        String pageNo = searchMap.get("pageNo");
        String pageSize = searchMap.get("pageSize");
        // 从第几个开始
        query.setOffset((Integer.parseInt(pageNo) - 1) * Integer.parseInt(pageSize));
        // 每页显示的条数
        query.setRows(Integer.parseInt(pageSize));
        // 设置高亮
        HighlightOptions highlightOptions = new HighlightOptions();
        // 需要高亮的域
        highlightOptions.addField("item_title");
        // 前缀
        highlightOptions.setSimplePrefix("<span style='color:red'>");
        // 后缀
        highlightOptions.setSimplePostfix("</span>");

        query.setHighlightOptions(highlightOptions);
        // 执行查询
        HighlightPage<Item> highlightPage = solrTemplate.queryForHighlightPage(query, Item.class);
        // 显示的总条数
        map.put("total", highlightPage.getTotalElements());
        // 显示的总页数
        map.put("totalPages", highlightPage.getTotalPages());
        List<HighlightEntry<Item>> highlightEntries = highlightPage.getHighlighted();
        for (HighlightEntry<Item> highlightEntry : highlightEntries) {
            Item item = highlightEntry.getEntity();
            List<HighlightEntry.Highlight> highlights = highlightEntry.getHighlights();
            if (highlights != null && highlights.size() > 0) {
                item.setTitle(highlights.get(0).getSnipplets().get(0));
            }
        }
        //结果集
        map.put("rows", highlightPage.getContent());
        return map;
    }
}
