package com.reveria.userservice.security;

import com.reveria.userservice.model.entity.Moderator;
import com.reveria.userservice.model.enums.ModeratorRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class ModeratorPrincipal implements UserDetails {

    private Moderator moderator;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (moderator.getRole() == ModeratorRole.SUPER_ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_MODERATOR"),
                    new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"));
    }

    @Override
    public String getPassword() {
        return moderator.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return moderator.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return moderator.getActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return moderator.getActive();
    }
}