package com.tracemindai.knowledge.repository;

import com.tracemindai.knowledge.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Optional<KnowledgeDocument> findFirstByDocumentName(String documentName);

    void deleteByDocumentName(String documentName);

    List<KnowledgeDocument> findAll();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE knowledge_document
            SET embedding = CAST(:embedding AS vector)
            WHERE id = :id
            """, nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    @Query(value = """
            SELECT kd.id, kd.document_name, kd.chunk_id, kd.content, kd.created_at,
                   1 - (kd.embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM knowledge_document kd
            ORDER BY kd.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findSimilarDocuments(@Param("embedding") String embedding,
                                        @Param("topK") int topK);
}
