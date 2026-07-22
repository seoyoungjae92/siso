package com.siso.backend.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Side side;

    @Column(nullable = false)
    private boolean enabled;

    protected Source() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Side getSide() {
        return side;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
