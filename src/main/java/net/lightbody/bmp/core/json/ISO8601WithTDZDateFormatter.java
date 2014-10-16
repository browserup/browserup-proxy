package net.lightbody.bmp.core.json;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.ScalarSerializerBase;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 
 * @author Damien Jubeau <damien.jubeau@dareboost.com>
 * Allows Date Format to be compliant with Har 1.2 Spec : ISO 8601 with Time Zone Designator
 * @see https://github.com/lightbody/browsermob-proxy/issues/44
 *
 */
public class ISO8601WithTDZDateFormatter extends ScalarSerializerBase<Date> {
	
    public final static ISO8601WithTDZDateFormatter instance = new ISO8601WithTDZDateFormatter();
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    

    public ISO8601WithTDZDateFormatter() {
        super(java.util.Date.class);
    }

    @Override
    public void serialize(java.util.Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        jgen.writeString(df.format(value));
    }


    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
        return createSchemaNode("string", true);
    }

}
