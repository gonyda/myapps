package com.myapps.web.myshare.domain.repository;

import com.myapps.web.myshare.domain.model.MiniAppStore;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@link MiniAppStore} 엔티티의 영속화 및 조회를 담당하는 Spring Data JPA 리포지토리. */
public interface MiniAppStoreRepository extends JpaRepository<MiniAppStore, String> {}
