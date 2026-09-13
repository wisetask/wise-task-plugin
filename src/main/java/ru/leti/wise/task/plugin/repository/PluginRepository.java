package ru.leti.wise.task.plugin.repository;

import io.micrometer.observation.annotation.Observed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.domain.GraphType;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.PluginType;

import java.util.UUID;

@Repository
public interface PluginRepository
        extends JpaRepository<PluginEntity, UUID>,
        JpaSpecificationExecutor<PluginEntity> {
}
