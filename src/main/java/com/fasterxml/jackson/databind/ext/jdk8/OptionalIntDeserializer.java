package com.fasterxml.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.OptionalInt;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.type.LogicalType;

public class OptionalIntDeserializer extends BaseScalarOptionalDeserializer<OptionalInt>
{
    static final OptionalIntDeserializer INSTANCE = new OptionalIntDeserializer();

    public OptionalIntDeserializer() {
        super(OptionalInt.class, OptionalInt.empty());
    }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override
    public OptionalInt deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // minor optimization, first, for common case
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return OptionalInt.of(p.getIntValue());
        }
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            {
                String text = p.getText();
                CoercionAction act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (OptionalInt) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (OptionalInt) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    _coerceTextualNull(ctxt, false);
                    return _empty;
                }
                return OptionalInt.of(_parseIntPrimitive(ctxt, text));
            }
        case JsonTokenId.ID_NUMBER_FLOAT:
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "int");
            }
            return OptionalInt.of(p.getValueAsInt());
        case JsonTokenId.ID_NULL:
            return _empty;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final OptionalInt parsed = deserialize(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;            
            }
            break;
        default:
        }
        return (OptionalInt) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }
}
