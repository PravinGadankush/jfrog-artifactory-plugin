package com.checkmarx.sca.communication.fallbacks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PyPiFallbackTests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PyPiFallbackTests {

    @DisplayName("Apply Fallback System with success")
    @ParameterizedTest
    @CsvSource({
            "fs-extra,fs_extra",
            "fs_extra,fs-extra",
            "request,"
    })
    public void applyFallbackWithSuccess(String name, String expected) {
        var pyPiFallback = new PyPiFallback();

        var actual = pyPiFallback.applyFallback(name);

        Assertions.assertEquals(expected, actual);
    }
}
