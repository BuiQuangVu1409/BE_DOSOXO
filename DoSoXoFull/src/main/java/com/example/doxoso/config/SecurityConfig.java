package com.example.doxoso.config;

import com.example.doxoso.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * BCrypt chuẩn
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager chuẩn cách mới (Boot 3.x)
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       PasswordEncoder passwordEncoder,
                                                       CustomUserDetailsService userDetailsService) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return authBuilder.build();
    }

    /**
     * CORS Global cho FE in dev (5173, 3000, 3001)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:3001"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cfg.setAllowCredentials(true); // nếu gửi cookie/JWT
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Chuỗi filter & phân quyền
     */
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                // Bật CORS, tắt CSRF (REST + JWT)
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(csrf -> csrf.disable())
//
//                // Phân quyền theo route
//                .authorizeHttpRequests(auth -> auth
//                        // Cho preflight
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//
//                        // Public: auth endpoints (login/register/refresh)
//                        .requestMatchers("/auth/**").permitAll()
//
//                        // Public GET kết quả xổ số cho FE
//                        .requestMatchers(HttpMethod.GET, "/xoso/**").permitAll()
//
//                        // Public GET player
//                        .requestMatchers(HttpMethod.GET, "/api/player/**").permitAll()
//
//                        // Public GET bets
//                        .requestMatchers(HttpMethod.GET, "/api/bets/**", "/api/songuoichoi/**").permitAll()
//
//                        // ⭐ NEW: cho phép gọi POST vào /api/bets khi bạn test tạo cược bằng Postman
//                        // (nếu không cần thì có thể bỏ)
//                        .requestMatchers(HttpMethod.POST, "/api/bets/**", "/api/songuoichoi/**").permitAll()
//
//                        .requestMatchers(HttpMethod.GET, "/lich/**").permitAll()
//                        .requestMatchers(HttpMethod.GET, "/api/ketqua/**").permitAll()
//
//                        // Hiện tại bạn chỉ permitAll GET /ket-qua-tich/**
//                        .requestMatchers(HttpMethod.GET, "/ket-qua-tich/**").permitAll()
//
//                        // ⭐ NEW: CHO PHÉP POST để gọi run-save / run-save-all
//                        .requestMatchers(HttpMethod.POST, "/ket-qua-tich/**").permitAll()
//
//                        // (nếu sau này muốn PUT/PATCH/DELETE trên KQT cũng gọi được không cần login,
//                        // có thể thêm:
//                        // .requestMatchers(HttpMethod.PUT, "/ket-qua-tich/**").permitAll()
//                        // .requestMatchers(HttpMethod.PATCH, "/ket-qua-tich/**").permitAll()
//                        // .requestMatchers(HttpMethod.DELETE, "/ket-qua-tich/**").permitAll()
//                        // )
//
//                        .requestMatchers(HttpMethod.GET, "/tong-tien/**").permitAll()
//
//                        // Khu vực cần role
//                        .requestMatchers("/admin/tong/**").hasRole("ADMIN_TONG")
//                        .requestMatchers("/admin/quanly/**").hasAnyRole("ADMIN_QUAN_LY", "ADMIN_TONG")
//                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN_QUAN_LY", "ADMIN_TONG")
//
//                        // Mặc định: yêu cầu authenticated
//                        .anyRequest().authenticated()
//                )
//
//                // Stateless session (JWT)
//                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//
//                // Gắn JWT filter trước UsernamePasswordAuthenticationFilter
//                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Bật CORS, tắt CSRF (REST + JWT)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // 💥 TẠM THỜI: cho phép TẤT CẢ request, bỏ hết phân quyền route
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 💥 TẠM THỜI KHÔNG GẮN JWT FILTER
        // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
