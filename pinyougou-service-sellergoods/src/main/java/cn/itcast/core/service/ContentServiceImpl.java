package cn.itcast.core.service;

import cn.itcast.core.mapper.ad.ContentMapper;
import cn.itcast.core.pojo.ad.Content;
import cn.itcast.core.pojo.ad.ContentQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ContentServiceImpl implements ContentService {
	
	@Autowired
	private ContentMapper contentMapper;

	@Override
	public List<Content> findAll() {
		List<Content> list = contentMapper.selectByExample(null);
		return list;
	}

	@Override
	public PageResult findPage(Content content, Integer pageNum, Integer pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<Content> page = (Page<Content>)contentMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void add(Content content) {
		// 1: 先通过广澳ID,查询广告的分类ID
		Content c = contentMapper.selectByPrimaryKey(content.getId());
		// 5: 修改Mysql
		contentMapper.insertSelective(content);
		// 2: 判断查询出的广告分类ID是否与现在的ID相同
		if (!c.getCategoryId().equals(content.getCategoryId())) {
			//  3: 不相同，删除原来的缓存
			redisTemplate.boundHashOps("content").delete(c.getCategoryId());
		}
		// 4: 无论是否相同，都要删除现在的缓存
		redisTemplate.boundHashOps("content").delete(content.getCategoryId());
	}

	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public void edit(Content content) {
		// 1: 先通过广澳ID,查询广告的分类ID
		Content c = contentMapper.selectByPrimaryKey(content.getId());
		// 5: 修改Mysql
		contentMapper.updateByPrimaryKeySelective(content);
		// 2: 判断查询出的广告分类ID是否与现在的ID相同
		if (!c.getCategoryId().equals(content.getCategoryId())) {
			//  3: 不相同，删除原来的缓存
			redisTemplate.boundHashOps("content").delete(c.getCategoryId());
		}
		// 4: 无论是否相同，都要删除现在的缓存
		redisTemplate.boundHashOps("content").delete(content.getCategoryId());

	}

	@Override
	public Content findOne(Long id) {
		Content content = contentMapper.selectByPrimaryKey(id);
		return content;
	}

	@Override
	public void delAll(Long[] ids) {
		if(ids != null){
			for(Long id : ids){
				contentMapper.deleteByPrimaryKey(id);
			}
		}
	}

	//根据外键 查询轮播图
    @Override
    public List<Content> findByCategoryId(Long categoryId) {
		//1:查询缓存
		List<Content> contentList = (List<Content>) redisTemplate.boundHashOps("content").get(categoryId);
		if (null == contentList || contentList.size() == 0) {
			//3:没有 查询Mysql数据
			ContentQuery contentQuery = new ContentQuery();
			contentQuery.createCriteria().andCategoryIdEqualTo(categoryId);
			contentQuery.setOrderByClause("sort_order desc");
			contentList = contentMapper.selectByExample(contentQuery);
			//4:保存缓存一份
			redisTemplate.boundHashOps("content").put(categoryId, contentList);
			redisTemplate.boundHashOps("content").expire(1, TimeUnit.DAYS);
		}
		//5:直接返回
		return contentList;
	}

}
