package com.pollingsystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Every error uses this shape: {"error": CODE, "message": text, "details": [..]?}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message, List<String> details) { }
