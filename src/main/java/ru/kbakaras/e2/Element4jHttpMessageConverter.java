package ru.kbakaras.e2;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;

//@Component
public class Element4jHttpMessageConverter extends AbstractHttpMessageConverter<Element> {
	private SAXReader reader;

	public Element4jHttpMessageConverter() {
		super(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
		this.reader = new SAXReader();
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return Element.class.isAssignableFrom(clazz);
	}

	@Override
	protected Element readInternal(Class<? extends Element> clazz, HttpInputMessage inputMessage) throws IOException {
		try {
			return reader.read(inputMessage.getBody()).getRootElement();
		} catch (DocumentException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Long getContentLength(Element element, MediaType contentType) {
		return null;
		/*Charset charset = getContentTypeCharset(contentType);
		try {
			return (long) element.getBytes(charset.name()).length;
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}*/
	}

	@Override
	protected void writeInternal(Element element, HttpOutputMessage outputMessage) throws IOException {
		StreamUtils.copy(element.asXML(), Charset.forName("UTF-8"), outputMessage.getBody());
	}

}
