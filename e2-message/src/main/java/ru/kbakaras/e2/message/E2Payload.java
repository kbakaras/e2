package ru.kbakaras.e2.message;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.SimpleVariableContext;
import ru.kbakaras.sugar.lazy.Lazy;
import ru.kbakaras.sugar.lazy.MapCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public class E2Payload {

    protected Element xml;

    private MapCache<String, E2Entity> entities = MapCache.of(entityName ->
        entity(entityName).orElseGet(() ->
                new E2Entity(xml.addElement(E2.ENTITY)
                        .addAttribute(E2.ENTITY_NAME, entityName))
        )
    );
    private MapCache<String, E2State> states = MapCache.of(stateName ->
            state(stateName).orElseGet(() ->
                    new E2State(xml.addElement(E2.STATE)
                            .addAttribute(E2.STATE_NAME, stateName))
            )
    );


    public E2Payload(Element xml) {
        this.xml = xml;
    }


    public UUID systemUid() {
        return UUID.fromString(xml.attributeValue(E2.SYSTEM_UID));
    }

    public String systemName() {
        return xml.attributeValue(E2.SYSTEM_NAME);
    }


    public List<E2Entity> entities() {
        return xml.elements(E2.ENTITY).stream()
                .map(E2Entity::new)
                .collect(Collectors.toList());
    }

    public Optional<E2Entity> entity(String entityName) {
        XPath expr = entityXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue(E2.ENTITY_NAME, entityName);

        return Optional.ofNullable((Element) expr.selectSingleNode(xml)).map(E2Entity::new);
    }

    public List<E2State> states() {
        return xml.elements(E2.STATE).stream()
                .map(E2State::new)
                .collect(Collectors.toList());
    }

    public Optional<E2State> state(String stateName) {
        XPath expr = stateXPath.get();
        SimpleVariableContext vc = (SimpleVariableContext) expr.getVariableContext();
        vc.setVariableValue("stateName", stateName);

        return Optional.ofNullable((Element) expr.selectSingleNode(xml)).map(E2State::new);
    }

    public Optional<E2Element> referencedElement(E2Reference reference) {
        return entity(reference.entityName)
                .flatMap(entity -> entity.element(reference.elementUid));
    }


    @SuppressWarnings("unchecked")
    public <P extends E2Payload> P setSystemUid(String systemUidString) {
        xml.addAttribute(E2.SYSTEM_UID, systemUidString);
        return (P) this;
    }

    public <P extends E2Payload> P setSystemUid(UUID systemUid) {
        return setSystemUid(systemUid.toString());
    }

    @SuppressWarnings("unchecked")
    public <P extends E2Payload> P setSystemName(String systemName) {
        xml.addAttribute(E2.SYSTEM_NAME, systemName);
        return (P) this;
    }


    /**
     * Создаёт в выходном сообщении узел для указанной сущности, если
     * он ещё не был создан.
     * @param entityName Имя сущности
     * @return Ссылку на обёртку для узла сущности.
     */
    public E2Entity createEntity(String entityName) {
        return entities.get(entityName);
    }

    /**
     * Создаёт в выходном сообщении узел для указанного состояния, если
     * он ещё не был создан.
     * @param stateName Имя состояния
     * @return Ссылку на обёртку для узла состояния.
     */
    public E2State createState(String stateName) {
        return states.get(stateName);
    }


    /**
     * Выполняет пересборку xml так, чтобы сначала шли все узлы <i>entity</i>,
     * а затем <i>state</i>. Так xml будет соответствовать xsd-схеме получателей.
     */
    void reorderEntitiesAndStates() {
        List<Element> errors   = new ArrayList<>();
        List<Element> entities = new ArrayList<>();
        List<Element> states   = new ArrayList<>();

        for (Element element: xml.elements()) {
            element.detach();
            if (E2.ENTITY.equals(element.getName())) {
                entities.add(element);
            } else if (E2.STATE.equals(element.getName())) {
                states.add(element);
            } else if (E2.ERROR.equals(element.getName())) {
                errors.add(element);
            }
        }

        errors.forEach(xml::add);
        entities.forEach(xml::add);
        states.forEach(xml::add);
    }


    private static Lazy<XPath> entityXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:entity[@entityName=$entityName]", new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });

    private static Lazy<XPath> stateXPath = Lazy.of(() -> {
        XPath expr = DocumentFactory.getInstance().createXPath(
                "e2:state[@stateName=$stateName]", new SimpleVariableContext());
        expr.setNamespaceURIs(E2.E2MAP);

        return expr;
    });

}