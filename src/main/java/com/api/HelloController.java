package com.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.net.HttpURLConnection;


@RestController
public class HelloController {

    // This tells Spring to run this method
    // when someone visits http://localhost:8080/hello
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello from my first REST API!";
    }

    // You can add another one!
    @GetMapping("/goodbye")
    public String sayGoodbye() {
        return "Goodbye for now!";
    }
}