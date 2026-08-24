package com.myapps.web.myrpg.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 사용자 계정 정보를 보관하는 JPA 엔티티.
 *
 * <p>{@code user_account} 테이블에 매핑되며, 사용자 계정 자격증명과 연결된 캐릭터 식별자({@code characterId})를 영속 관리한다.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    /** JPA 전용 기본 생성자. */
    protected UserAccount() {}

    /**
     * 전체 필드를 지정하는 생성자.
     *
     * @param username 사용자 로그인 아이디 (고유값)
     * @param password 사용자 비밀번호
     * @param nickname 사용자 표시 닉네임
     * @param characterId 연결된 캐릭터 식별자
     */
    public UserAccount(
            final String username,
            final String password,
            final String nickname,
            final Long characterId) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.characterId = characterId;
    }

    /**
     * 엔티티 식별자를 반환한다.
     *
     * @return PK (미영속 시 null)
     */
    public Long getId() {
        return id;
    }

    /**
     * 사용자 아이디를 반환한다.
     *
     * @return 로그인 아이디
     */
    public String getUsername() {
        return username;
    }

    /**
     * 비밀번호를 반환한다.
     *
     * @return 비밀번호
     */
    public String getPassword() {
        return password;
    }

    /**
     * 표시 닉네임을 반환한다.
     *
     * @return 닉네임
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 연결된 캐릭터 식별자를 반환한다.
     *
     * @return 캐릭터 식별자 ID
     */
    public Long getCharacterId() {
        return characterId;
    }
}
