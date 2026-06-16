package com.tracemindai.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "chunk_id")
    private Integer chunkId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
