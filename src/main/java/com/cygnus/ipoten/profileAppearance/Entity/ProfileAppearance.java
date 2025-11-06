package com.cygnus.ipoten.profileAppearance.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "profile_appearance")
public class ProfileAppearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long accountId;   // Account 객체 대신 단순 ID

    @Column(name = "photo_key")
    private String photoKey;

    public void setPhotoKey(String newKey) {
        this.photoKey = newKey;
    }

    public static ProfileAppearance init(Long accountId) {
        return new ProfileAppearance(null, accountId, null);
    }
}
