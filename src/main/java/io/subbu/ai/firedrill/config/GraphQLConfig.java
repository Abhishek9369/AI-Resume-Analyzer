package io.subbu.ai.firedrill.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GraphQL configuration for custom scalar types.
 * Provides implementations for UUID, LocalDateTime, and Upload scalar types used in the schema.
 */
@Configuration
public class GraphQLConfig {

    /**
     * Configure GraphQL scalars for UUID, LocalDateTime, and Upload types.
     * 
     * @return RuntimeWiringConfigurer with scalar type definitions
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(localDateTimeScalar())
                .scalar(uploadScalar());
    }

    /**
     * Create LocalDateTime scalar type for handling LocalDateTime fields.
     * 
     * @return GraphQLScalarType for LocalDateTime
     */
    private GraphQLScalarType localDateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("LocalDateTime")
                .description("LocalDateTime scalar")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } else if (dataFetcherResult instanceof OffsetDateTime) {
                            // Convert OffsetDateTime to LocalDateTime for serialization
                            return ((OffsetDateTime) dataFetcherResult).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } else if (dataFetcherResult instanceof LocalDate) {
                            // Convert LocalDate to LocalDateTime (midnight)
                            return ((LocalDate) dataFetcherResult).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime, LocalDate, or OffsetDateTime object, but got: " + dataFetcherResult.getClass().getName());
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid LocalDateTime value: " + input, e);
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) throws CoercingParseValueException {
                        if (input instanceof String) {
                            return parseValue(input);
                        }
                        throw new CoercingParseValueException("Expected a String literal");
                    }
                })
                .build();
    }

    /**
     * Create Upload scalar type for file uploads.
     * This is a custom scalar that handles multipart file uploads in GraphQL mutations.
     * 
     * @return GraphQLScalarType for Upload
     */
    private GraphQLScalarType uploadScalar() {
        return GraphQLScalarType.newScalar()
                .name("Upload")
                .description("Upload scalar type for file uploads")
                .coercing(new graphql.schema.Coercing<Object, Object>() {
                    @Override
                    public Object serialize(Object dataFetcherResult) {
                        // Not used for uploads
                        throw new UnsupportedOperationException("Upload scalar is only for input");
                    }

                    @Override
                    public Object parseValue(Object input) {
                        // Handle multipart file upload
                        return input;
                    }

                    @Override
                    public Object parseLiteral(Object input) {
                        // Uploads are not supported as literals
                        throw new UnsupportedOperationException("Upload scalar does not support literals");
                    }
                })
                .build();
    }
}
