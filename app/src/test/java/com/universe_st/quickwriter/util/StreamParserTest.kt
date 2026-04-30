package com.universe_st.quickwriter.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamParserTest {

    private val parser = StreamParser()

    @Test
    fun parseLine_toolCallArgsWithoutId_preservesArgumentsDelta() {
        val chunk = parser.parseLine(
            """data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\"正文/第一章.md\"}"}}]}}]}"""
        )

        assertTrue(chunk is StreamChunk.ToolCallArgs)
        val args = chunk as StreamChunk.ToolCallArgs
        assertEquals(0, args.index)
        assertEquals("\"正文/第一章.md\"}", args.argsDelta)
    }

    @Test
    fun parseLine_toolCallBegin_preservesIndexIdNameAndInitialArguments() {
        val chunk = parser.parseLine(
            """data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"view_file","arguments":"{\"relativePath\":"}}]}}]}"""
        )

        assertTrue(chunk is StreamChunk.ToolCallBegin)
        val begin = chunk as StreamChunk.ToolCallBegin
        assertEquals(0, begin.index)
        assertEquals("call_1", begin.id)
        assertEquals("view_file", begin.name)
        assertEquals("{\"relativePath\":", begin.initialArgsDelta)
    }
}
