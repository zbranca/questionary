package com.questionary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    public Chapter() {}

    public Chapter(String name, int sortOrder, AppUser user) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.user = user;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}
