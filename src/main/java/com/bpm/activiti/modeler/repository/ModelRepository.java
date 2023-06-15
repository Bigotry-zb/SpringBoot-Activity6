package com.bpm.activiti.modeler.repository;

import com.bpm.example.modeler.domain.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public abstract interface ModelRepository extends JpaRepository<Model, String> {
/*
    @Query("from Model as model where model.createdBy = :user and model.modelType = :modelType")
    public abstract List<Model> findModelsCreatedBy(@Param("user") String paramString, @Param("modelType") Integer paramInteger, Sort paramSort);

    @Query("from Model as model where model.createdBy = :user and (lower(model.name) like :filter or lower(model.description) like :filter) and model.modelType = :modelType")
    public abstract List<Model> findModelsCreatedBy(@Param("user") String paramString1, @Param("modelType") Integer paramInteger, @Param("filter") String paramString2, Sort paramSort);

    @Query("from Model as model where model.key = :key and model.modelType = :modelType")
    public abstract List<Model> findModelsByKeyAndType(@Param("key") String paramString, @Param("modelType") Integer paramInteger);
*/
    @Query("from Model as model where (lower(model.name) like :filter or lower(model.description) like :filter) and model.modelType = :modelType")
    public abstract List<Model> findModelsByModelType(@Param("modelType") Integer paramInteger, @Param("filter") String paramString);

    @Query("from Model as model where model.modelType = :modelType")
    public abstract List<Model> findModelsByModelType(@Param("modelType") Integer paramInteger);

    @Query("from Model as model where model.id = :parentModelId")
    public abstract List<Model> findModelsByParentModelId(@Param("parentModelId") String paramString);
/*
    @Query("select count(m.id) from Model m where m.createdBy = :user and m.modelType = :modelType")
    public abstract Long countByModelTypeAndUser(@Param("modelType") int paramInt, @Param("user") String paramString);



    @Query("select m from ModelRelation mr inner join m r.model m where mr.parentModelId = :parentModelId and m.modelType = :modelType")
    public abstract List<Model> findModelsByParentModelIdAndType(@Param("parentModelId") String paramString, @Param("modelType") Integer paramInteger);

    @Query("select m.id, m.name, m.modelType from ModelRelation mr inner join mr.parentModel m where mr.modelId = :modelId")
    public abstract List<Model> findModelsByChildModelId(@Param("modelId") String paramString);

    @Query("select model.key from Model as model where model.id = :modelId and model.createdBy = :user")
    public abstract String appDefinitionIdByModelAndUser(@Param("modelId") String paramString1, @Param("user") String paramString2);
*/
}

