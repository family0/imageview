package org.itstory.imageview.controller

import org.itstory.imageview.service.MainService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MainController(
    private val svc: MainService
) {
    @GetMapping("/")
    fun main(): String {
        return "main"
    }
}
