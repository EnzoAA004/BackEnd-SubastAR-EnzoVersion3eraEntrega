package com.subastar.subastar.security;

import com.subastar.subastar.model.Credencial;
import com.subastar.subastar.repository.CredencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CredencialRepository credencialRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Credencial credencial = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        return new User(
                credencial.getEmail(),
                credencial.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
    }
}
