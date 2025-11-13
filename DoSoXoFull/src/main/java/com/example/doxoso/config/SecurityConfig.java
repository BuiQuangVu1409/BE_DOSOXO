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

    /** BCrypt chuẩn */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** AuthenticationManager chuẩn cách mới (Boot 3.x) */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       PasswordEncoder passwordEncoder,
                                                       CustomUserDetailsService userDetailsService) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return authBuilder.build();
    }

    /** CORS Global cho FE in dev (5173, 3000, 3001) */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:3001"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
        cfg.setAllowCredentials(true); // nếu gửi cookie/JWT
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /** Chuỗi filter & phân quyền */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Bật CORS, tắt CSRF (REST + JWT)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // Phân quyền theo route
                .authorizeHttpRequests(auth -> auth
                        // Cho preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public: auth endpoints (login/register/refresh)
                        .requestMatchers("/auth/**").permitAll()

                        // Public GET kết quả xổ số cho FE
                        .requestMatchers(HttpMethod.GET, "/xoso/**").permitAll()
//requestMatchers Là bộ chọn request: bạn chỉ định điều kiện (đường dẫn, HTTP method, header…) để áp dụng một luật tiếp theo.
                        .requestMatchers(HttpMethod.GET, "/api/player/**").permitAll()
//permitAll Là luật cấp quyền: “cho phép tất cả” các request đã được chọn ở trên không cần đăng nhập/không cần role.
                        .requestMatchers(HttpMethod.GET, "/api/bets/**", "/api/songuoichoi/**").permitAll()

                                .requestMatchers(HttpMethod.GET, "/lich/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/ketqua/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/ket-qua-tich/**").permitAll()
                                // (Tuỳ bạn muốn public đọc bet hay không)
                        // .requestMatchers(HttpMethod.GET, "/api/bets/**", "/api/songuoichoi/**").permitAll()

                        // Khu vực cần role
                        .requestMatchers("/admin/tong/**").hasRole("ADMIN_TONG")
                        .requestMatchers("/admin/quanly/**").hasAnyRole("ADMIN_QUAN_LY","ADMIN_TONG")
                        .requestMatchers("/user/**").hasAnyRole("USER","ADMIN_QUAN_LY","ADMIN_TONG")

                        // Mặc định: yêu cầu authenticated
                        .anyRequest().authenticated()
                )

                // Stateless session (JWT)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Gắn JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
