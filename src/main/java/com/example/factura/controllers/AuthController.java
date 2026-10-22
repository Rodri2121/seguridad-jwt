package com.example.factura.controllers;

import com.example.factura.dto.LoginRequest;
import com.example.factura.dto.LoginResponse;
import com.example.factura.dto.PerfilResponse;
import com.example.factura.services.TokenService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final TokenService tokens;

    public AuthController(AuthenticationManager manager,
                          TokenService tokens) {
        this.manager = manager;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest datos) {
        Authentication auth = manager.authenticate(
            new UsernamePasswordAuthenticationToken(
                datos.username(), datos.password()));
        return ResponseEntity.ok(
            new LoginResponse(tokens.crearToken(auth), "Bearer"));
    }

    @GetMapping("/perfil")
    public PerfilResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new PerfilResponse(jwt.getSubject(),
            roles == null ? List.of() : roles);
    }
}
