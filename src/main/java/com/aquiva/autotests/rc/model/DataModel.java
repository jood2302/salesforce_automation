package com.aquiva.autotests.rc.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common class for all data models in the framework.
 * Provides an implementation of the overridden {@link #toString()} method
 * for easier reporting on them.
 */
public abstract class DataModel {
    private static final Logger LOG = LoggerFactory.getLogger(DataModel.class);

    @Override
    public String toString() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new MrBeanModule());
        objectMapper.registerModule(new BlackbirdModule());
        objectMapper.registerModule(new JavaTimeModule());
        var ow = objectMapper.writer().withDefaultPrettyPrinter();
        try {
            return ow.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.warn("Error while parsing a data model class {}! Object.toString() will be used instead.",
                    this.getClass().getName());
            return super.toString();
        }
    }
}
