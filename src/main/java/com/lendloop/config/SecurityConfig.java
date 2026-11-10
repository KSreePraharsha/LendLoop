
package com.lendloop.config;

import com.lendloop.service.StudentService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {

    private final StudentService studentService;

    public SecurityConfig(@Lazy StudentService studentService) {
        this.studentService = studentService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            com.lendloop.model.Student student =
                    studentService.findStudentByEmail(authentication.getName());
            request.getSession().setAttribute("studentId", student.getId());
            request.getSession().setAttribute("studentName", student.getFullName());
            response.sendRedirect("/dashboard");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .userDetailsService(studentService)

            // ---- URL authorization ----
            .authorizeHttpRequests(auth -> auth
                // Public routes: landing page, static resources, register
                .requestMatchers("/", "/register", "/login", "/css/**",
                                 "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ---- Form login ----
            .formLogin(form -> form
                .loginPage("/login")                      // our custom Thymeleaf login page
                .loginProcessingUrl("/login")             // Spring Security handles POST /login
                .usernameParameter("username")            // matches name="username" in login.html
                .passwordParameter("password")            // matches name="password" in login.html
                .successHandler(loginSuccessHandler())    // populates session then redirects
                .failureUrl("/login?error")               // on bad credentials
                .permitAll()
            )

            // ---- Logout ----
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ---- Session fixation protection ----
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
            );

        return http.build();
    }
}
