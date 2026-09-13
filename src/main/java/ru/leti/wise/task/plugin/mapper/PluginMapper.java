package ru.leti.wise.task.plugin.mapper;

import org.mapstruct.*;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.plugin.PluginOuterClass.Plugin;
import ru.leti.wise.task.plugin.domain.GraphType;
import ru.leti.wise.task.plugin.domain.PluginEntity;
import ru.leti.wise.task.plugin.domain.PluginType;

import java.util.List;
import java.util.Base64;


@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PluginMapper {


    @Mapping(target = "jarFile", source = "jarFile", ignore = true)
    Plugin pluginEntityToPlugin(PluginEntity plugin);

    @Mapping(target = "jarFile", source = "jarFile", qualifiedByName = "jarMapping")
    PluginEntity pluginToPluginEntity(Plugin plugin);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "jarFile", ignore = true)
    @Mapping(target = "jarName", ignore = true)
    void updatePlugin(PluginEntity newPlugin, @MappingTarget PluginEntity oldPlugin);

    List<Plugin> pluginEntitiesToPlugins(List<PluginEntity> plugins);

    default PluginOuterClass.PluginType toPluginType(PluginType pluginType) {
        return PluginOuterClass.PluginType.valueOf(pluginType.name());
    }

    default PluginOuterClass.GraphType toGraphType(GraphType graphType) {
        return PluginOuterClass.GraphType.valueOf(graphType.name());
    }

    @Condition
    default boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    @Named("jarMapping")
    default byte[] toByteArray(String base64) {
        if (base64.isBlank()) {
            return null;
        }
        return Base64.getDecoder().decode(base64);
    }
}
