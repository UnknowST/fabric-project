package com.controller;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class IndexController implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {


        registry.addViewController("/").setViewName("redirect:/pages/login.html");
//        registry.addViewController("/admin").setViewName("forward:/index.html");
//        registry.addViewController("/admin/login").setViewName("forward:/page/login-1.html");


    }


}
