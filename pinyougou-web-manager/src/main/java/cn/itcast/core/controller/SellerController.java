package cn.itcast.core.controller;

import cn.itcast.core.pojo.seller.Seller;
import cn.itcast.core.service.SellerService;
import com.alibaba.dubbo.config.annotation.Reference;
import entity.PageResult;
import entity.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家管理
 */
@RestController
@RequestMapping("/seller")
public class SellerController {

    @Reference
    private SellerService sellerService;

    // 分页查询  条件
    @RequestMapping("/search")
    public PageResult search(Integer page, Integer rows, @RequestBody Seller seller) {
        return sellerService.search(page, rows, seller);
    }

    // 查询一条数据  数据回显
    @RequestMapping("/findOne")
    public Seller findOne(String sellerId) {
        return sellerService.findOne(sellerId);
    }

    // 修改审核状态
    @RequestMapping("/updateStatus")
    public Result updateStatus(String sellerId, String status) {
        try {
            sellerService.updateStatus(sellerId, status);
            if ("1".equals(status)) {
                return new Result(true, "修改审核通过成功");
            } else if ("2".equals(status)) {
                return new Result(true, "修改审核未通过成功");
            } else{
                return new Result(true, "修改关闭商家成功");
            }
        } catch (Exception e) {
            if ("1".equals(status)) {
                return new Result(false, "修改审核通过失败");
            } else if ("2".equals(status)) {
                return new Result(false, "修改审核未通过失败");
            } else {
                return new Result(false, "修改关闭商家失败");
            }
        }
    }

}
