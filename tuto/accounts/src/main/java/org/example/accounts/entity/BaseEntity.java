package org.example.accounts.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// Marks this class as a superclass for other entities. Fields in this class will be inherited by child entities.
@MappedSuperclass
// Enables auditing for this entity. Automatically populates fields like createdAt, createdBy, etc.
@EntityListeners(AuditingEntityListener.class)
// Lombok annotations to automatically generate getters, setters, and toString method.
@Getter
@Setter
@ToString
public class BaseEntity {

    // Automatically sets this field to the current date and time when the entity is created.
    @CreatedDate
    // This column cannot be updated after the entity is created.
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Automatically sets this field to the current user who created the entity.
    @CreatedBy
    // This column cannot be updated after the entity is created.
    @Column(updatable = false)
    private String createdBy;

    // Automatically updates this field to the current date and time whenever the entity is modified.
    @LastModifiedDate
    // This column cannot be inserted manually (managed by AuditingEntityListener).
    @Column(insertable = false)
    private LocalDateTime updatedAt;

    // Automatically updates this field to the current user who modified the entity.
    @LastModifiedBy
    // This column cannot be inserted manually (managed by AuditingEntityListener).
    @Column(insertable = false)
    private String updatedBy;
}