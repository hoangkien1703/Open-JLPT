package com.openjlpt.core

import com.openjlpt.core.glossary.Glossary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryTest {

    @Test
    fun keyMatchesTheGeneratorsHash() {
        // python3 -c "import hashlib; print(hashlib.sha1('まどの 近く'.encode()).hexdigest()[:16])"
        assertEquals("16a922eb5ce696c3", Glossary.key("まどの 近く"))
    }

    @Test
    fun spansResolveToWords() {
        val text = "まどの 近く"
        val json = """
            {"version":1,
             "words":{"1":{"w":"窓","r":"まど","m":["window"],"a":"abc"},"2":{"w":"近く","r":"ちかく","m":["near"],"a":"def"}},
             "texts":{"${Glossary.key(text)}":[[0,2,"1"],[4,6,"2"],[5,99,"2"],[0,1,"missing"]]}}
        """.trimIndent()
        val spans = Glossary.parse(json).spansFor(text)
        assertEquals(2, spans.size)
        assertEquals("窓", spans[0].word.headword)
        assertEquals("まど", text.substring(spans[0].start, spans[0].end))
        assertEquals("近く", text.substring(spans[1].start, spans[1].end))
        assertEquals("ちかく", spans[1].word.spokenText)
        assertTrue(Glossary.parse(json).spansFor("other text").isEmpty())
    }
}
