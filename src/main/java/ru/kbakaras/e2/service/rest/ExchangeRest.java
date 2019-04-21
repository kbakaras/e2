package ru.kbakaras.e2.service.rest;

import org.dom4j.Element;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.kbakaras.e2.service.Poller4Conversion;
import ru.kbakaras.e2.service.Processor4Request;

import javax.annotation.Resource;

/**
 * Создано: kbakaras, в день: 04.03.2018.
 */
@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_XML_VALUE,
        method = RequestMethod.POST, path = "rest")
public class ExchangeRest {
    @Resource private Poller4Conversion  poller4Conversion;
    @Resource private Processor4Request  processor4Request;


    @RequestMapping(path = "post")
    public @ResponseBody Element post(@RequestBody Element request) {
        String rootName = request.getName();

        switch (rootName) {
            case "updateRequest":
                poller4Conversion.updateRequest(request);
                return null;

            case "elementRequest":
                return processor4Request.elementRequest(request);

            case "listRequest":
                return processor4Request.listRequest(request);
        }

        throw new ExchangeException("Post request for root=" + rootName + " is not supported!");
    }

    @RequestMapping(path = "update")
    public void update(@RequestBody Element request) {
        String rootName = request.getName();

        if (!rootName.equals("updateRequest")) {
            throw new ExchangeException("Request's root=" + rootName + " is not 'updateRequest'!");
        }

        poller4Conversion.updateRequest(request);
    }

    @RequestMapping(path = "request")
    public @ResponseBody Element request(@RequestBody Element request) {
        String rootName = request.getName();

        switch (rootName) {
            case "elementRequest":
                return processor4Request.elementRequest(request);

            case "listRequest":
                return processor4Request.listRequest(request);
        }

        throw new ExchangeException("Request's root=" + rootName + " is neither 'elementRequest' nor 'listRequest'!");
    }

    @Deprecated
    @RequestMapping(path = "agr")
    public @ResponseBody Element agr(@RequestBody Element request) {
        return post(request);
    }


    /*@Transactional
    @RequestMapping(path = "repeat.stuck")
    public void repeatStuck() {
        queue4DeliveryRepository.getFirstByProcessedIsFalseAndStuckIsTrueOrderByTimestampAsc().ifPresent(
                stuck -> {
                    Queue4Conversion source = queue4ConversionRepository.getOne(stuck.getSourceMessageId());
                    LOG.info("Repeating the message {}", source);

                    Element root;
                    try {
                        root = DocumentHelper.parseText(source.getMessage()).getRootElement();
                        root.setQName(new QName("repeatRequest", root.getQName().getNamespace()));
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }

                    SystemInstance system = systemInstanceRepository.getOne(
                            UUID.fromString(root.attributeValue(E2.SYSTEM_UID)));

                    Queue4Repeat repeat = BaseEntity.newElement(Queue4Repeat.class);
                    repeat.setSourceMessageId(source.getId());
                    repeat.setDestination(system);
                    repeat.setTimestamp(timestampService.get());
                    repeat.setMessage(root.getDocument().asXML());
                    repeat.setSize(repeat.getMessage().length());
                    queue4RepeatRepository.save(repeat);

                    List<Queue4Delivery> list = queue4DeliveryRepository.findBySourceMessageId(source.getId());
                    list.forEach(deliver -> {
                        deliver.setProcessed(true);
                        queue4DeliveryRepository.save(deliver);
                    });
                }
        );
    }*/
}