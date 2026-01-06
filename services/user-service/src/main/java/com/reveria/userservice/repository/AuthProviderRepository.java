package com.reveria.userservice.repository;

import com.reveria.userservice.model.entity.AuthProvider;
import com.reveria.userservice.model.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {

    Optional<AuthProvider> findByProviderAndProviderId(ProviderType provider, String providerId);

    boolean existsByUserIdAndProvider(Long userId, ProviderType provider);
}