
// package com.example.dashboard_backend.Config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// @Configuration
// public class WebCorsConfig {
//     @Bean
//     public WebMvcConfigurer corsConfigurer() {
//         return new WebMvcConfigurer() {
//             @Override
//             public void addCorsMappings(CorsRegistry registry) {
//                 registry.addMapping("/api/**")
//                         .allowedOrigins(
//                                 "http://localhost:3000", // React (CRA/Next)
//                                 "http://localhost:5173"  // React (Vite)
//                         )
//                         .allowedMethods("GET","POST","PATCH","DELETE","OPTIONS")
//                         .allowedHeaders("*");
//             }
//         };
//     }
// }
