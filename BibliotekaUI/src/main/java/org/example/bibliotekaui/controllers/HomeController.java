package org.example.bibliotekaui.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("")
    public String getHomePage(Model model){
        model.addAttribute("pageStyle", "login");
        return "app";
    }
    @GetMapping("/seat_reservation")
    public String getSeatsPage(Model model) {
        model.addAttribute("pageStyle", "seat_reservation");
        return "app";
    }

}
