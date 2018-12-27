package cn.itcast.core.controller;

import entity.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * 购物车管理
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    //加入购物车
    @RequestMapping("/addGoodsToCartList")
   /* @CrossOrigin(origins = {"http://localhost:9003"},allowCredentials = "true")*/
    @CrossOrigin(origins = {"http://localhost:9003"})
    public Result addGoodsToCartList(Long itemId, Integer num, HttpServletResponse response){

        try {
            //添加商品到购物车中
            //不解决跨域
            //Springmvc
            //Servlet

  /*          response.setHeader("Access-Control-Allow-Origin", "http://localhost:9003");
            response.setHeader("Access-Control-Allow-Credentials", "true");*/
            return new Result(true,"加入购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"加入购物车失败");
        }

    }
}
