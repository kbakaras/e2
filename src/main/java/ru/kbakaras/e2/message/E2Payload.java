package ru.kbakaras.e2.message;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.SimpleVariableContext;
import ru.kbakaras.sugar.lazy.Lazy;
import ru.kbakaras.sugar.lazy.MapCache;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class E2Payload {
    protected Element xml;

    private MapCache<String, E2Entity> entities = MapCache.of(entityName -> {
        XPath expr = entityXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue("entityName", entityName);

        Element entity = (Element) expr.selectSingleNode(xml);
        if (entity == null) {
            entity = xml.addElement("entity");
            entity.addAttribute("entityName", entityName);
        }

        return new E2Entity(entity);
    });

    public E2Payload(Element xml) {
        this.xml = xml;
    }

    public List<E2Entity> entities() {
        return xml.elements(E2.ENTITY).stream()
                .map(E2Entity::new)
                .collect(Collectors.toList());
    }

    public Optional<E2Entity> entity(String entityName) {
        XPath expr = entityXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue("entityName", entityName);

        return Optional.ofNullable((Element) expr.selectSingleNode(xml)).map(E2Entity::new);
    }

    public Optional<E2Element> referencedElement(E2Reference reference) {
        return entity(reference.entityName)
                .flatMap(entity -> entity.element(reference.elementUid));
    }


    /**
     * Создаёт в выходном сообщении узел для указанной сущности, если
     * он ещё не был создан.
     * @param entityName Имя сущности
     * @param initializer Инициализатор для сущности. Он будет применён только в
     *                    том случае, если до вызова узел сущности не существовал.
     * @return Ссылку на обёртку для узла сущности.
     */
    public E2Entity createEntity(String entityName, Consumer<E2Entity> initializer) {
        return entities.get(entityName, initializer);
    }

    private static Lazy<XPath> entityXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:entity[@entityName=$entityName]", new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });
}