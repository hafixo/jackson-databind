package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoercePojosTest extends BaseMapTest
{
    static class Bean {
        public String a;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String JSON_EMPTY = quote("");
    private final String JSON_BLANK = quote("    ");

    /*
    /********************************************************
    /* Test methods, from empty String
    /********************************************************
     */

    public void testPOJOFromEmptyStringLegacy() throws Exception
    {
        // first, verify default settings which do not accept empty String:
        assertFalse(MAPPER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));

        // should be ok to enable dynamically
        _verifyFromEmptyPass(MAPPER.reader()
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT),
                JSON_EMPTY);

    }

    public void testPOJOFromEmptyGlobalConfig() throws Exception
    {
        _testPOJOFromEmptyGlobalConfig(CoercionInputShape.EmptyString, JSON_EMPTY, null);
    }
    
    public void testPOJOFromEmptyLogicalTypeConfig() throws Exception
    {
        _testPOJOFromEmptyLogicalTypeConfig(CoercionInputShape.EmptyString, JSON_EMPTY, null);
    }

    public void testPOJOFromEmptyPhysicalTypeConfig() throws Exception
    {
        _testPOJOFromEmptyPhysicalTypeConfig(CoercionInputShape.EmptyString, JSON_EMPTY, null);
    }

    /*
    /********************************************************
    /* Test methods, from blank String
    /********************************************************
     */

    public void testPOJOFromBlankGlobalConfig() throws Exception
    {
        _testPOJOFromEmptyGlobalConfig(CoercionInputShape.EmptyString, JSON_BLANK, Boolean.TRUE);
    }

    public void testPOJOFromBlankLogicalTypeConfig() throws Exception
    {
        _testPOJOFromEmptyLogicalTypeConfig(CoercionInputShape.EmptyString, JSON_BLANK, Boolean.TRUE);
    }

    public void testPOJOFromBlankPhysicalTypeConfig() throws Exception
    {
        _testPOJOFromEmptyPhysicalTypeConfig(CoercionInputShape.EmptyString, JSON_BLANK, Boolean.TRUE);
    }

    /*
    /********************************************************
    /* Second-level helper methods
    /********************************************************
     */

    private void _testPOJOFromEmptyGlobalConfig(final CoercionInputShape shape, final String json,
            Boolean allowEmpty)
        throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> {
                    h.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull)
                        .setAcceptBlankAsEmpty(allowEmpty);
                })
                .build();
        assertNull(_verifyFromEmptyPass(mapper, JSON_EMPTY));

        // Then coerce as empty
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> {
                    h.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty)
                        .setAcceptBlankAsEmpty(allowEmpty);
                })
            .build();
        Bean b = _verifyFromEmptyPass(mapper, JSON_EMPTY);
        assertNotNull(b);

        // and finally, "try convert", which aliases to 'null'
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> {
                    h.setCoercion(CoercionInputShape.EmptyString, CoercionAction.TryConvert)
                        .setAcceptBlankAsEmpty(allowEmpty);
                })
            .build();
        assertNull(_verifyFromEmptyPass(mapper, JSON_EMPTY));
    }

    private void _testPOJOFromEmptyLogicalTypeConfig(final CoercionInputShape shape, final String json,
            Boolean allowEmpty)
        throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(shape, CoercionAction.AsNull)
                                .setAcceptBlankAsEmpty(allowEmpty))
                .build();
        assertNull(_verifyFromEmptyPass(mapper, json));

        // Then coerce as empty
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(shape, CoercionAction.AsEmpty)
                            .setAcceptBlankAsEmpty(allowEmpty))
                .build();

        Bean b = _verifyFromEmptyPass(mapper, json);
        assertNotNull(b);

        // But also make fail again with 2-level settings
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> h.setCoercion(shape, CoercionAction.AsNull)
                        .setAcceptBlankAsEmpty(allowEmpty))
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(shape, CoercionAction.Fail))
                .build();
        _verifyFromEmptyFail(mapper, json);
    }

    private void _testPOJOFromEmptyPhysicalTypeConfig(final CoercionInputShape shape, final String json,
            Boolean allowEmpty)
        throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = jsonMapperBuilder()
                .withCoercionConfig(Bean.class,
                        cfg -> cfg.setCoercion(shape, CoercionAction.AsNull)
                            .setAcceptBlankAsEmpty(allowEmpty))
                .build();
        assertNull(_verifyFromEmptyPass(mapper, json));

        // Then coerce as empty
        mapper = jsonMapperBuilder()
                .withCoercionConfig(Bean.class,
                        cfg -> cfg.setCoercion(shape, CoercionAction.AsEmpty)
                            .setAcceptBlankAsEmpty(allowEmpty))
                .build();

        Bean b = _verifyFromEmptyPass(mapper, json);
        assertNotNull(b);

        // But also make fail again with 2-level settings, with physical having precedence
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(shape, CoercionAction.AsEmpty)
                            .setAcceptBlankAsEmpty(allowEmpty))
                .withCoercionConfig(Bean.class,
                        cfg -> cfg.setCoercion(shape, CoercionAction.Fail))
                .build();
        _verifyFromEmptyFail(mapper, json);
    }

    private Bean _verifyFromEmptyPass(ObjectMapper m, String json) throws Exception {
        return _verifyFromEmptyPass(m.reader(), json);
    }

    private Bean _verifyFromEmptyPass(ObjectReader r, String json) throws Exception
    {
        return r.forType(Bean.class)
                .readValue(json);
    }

    private void _verifyFromEmptyFail(ObjectMapper m, String json) throws Exception
    {
        try {
            m.readValue(json, Bean.class);
            fail("Should not accept Empty/Blank String for POJO with passed settings");
        } catch (MismatchedInputException e) {
            _verifyFailMessage(e);
        }
    }

    private void _verifyFailMessage(JsonProcessingException e)
    {
        verifyException(e, "Cannot deserialize value of type ");
        verifyException(e, " from empty String ", " from blank String ");
        assertValidLocation(e.getLocation());
    }
}
