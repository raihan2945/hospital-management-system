package com.example.hms.controller;

import com.example.hms.exception.ErrorPages;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ApplicationErrorController implements ErrorController {
    @RequestMapping("/error")
    public ModelAndView error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return ErrorPages.standard(status instanceof Integer code ? code : 404);
    }
}
