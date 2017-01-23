package com.xsearch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Description: springmvc test 
 * <p>
 * https://spring.io/guides/gs/serving-web-content/
 *
 * @author: wuming.zy
 * @version: v1.0
 * @since: Jan 23, 2017 3:23:24 PM
 */
@Controller
public class GreetingController {
	
	@RequestMapping("/greeting")
    public String greeting(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }
}
