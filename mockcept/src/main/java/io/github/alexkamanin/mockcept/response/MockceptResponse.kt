package io.github.alexkamanin.mockcept.response

import androidx.annotation.RawRes
import io.github.alexkamanin.mockcept.dsl.MockceptDsl

/**
 * Create response with status code and body.
 * @param status Http status code.
 * @param body Reference to response body in json format.
 */
class MockceptResponse private constructor(
    val status: StatusCode,
    @RawRes val body: Int?
) {
    @MockceptDsl
    class Builder {
        var status: StatusCode = StatusCode.OK

        @RawRes
        var body: Int? = null

        internal fun build(): MockceptResponse =
            MockceptResponse(status, body)
    }
}